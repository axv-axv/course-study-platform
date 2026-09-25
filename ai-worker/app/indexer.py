from __future__ import annotations

import logging

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter
from psycopg.rows import dict_row
from psycopg_pool import ConnectionPool

from .config import Settings
from .embeddings import HashEmbeddings
from .extractors import extract_pages
from .storage import ObjectStorage

logger = logging.getLogger(__name__)


class RagIndexer:
    def __init__(self, pool: ConnectionPool, settings: Settings) -> None:
        self.pool = pool
        self.settings = settings
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.chunk_size,
            chunk_overlap=settings.chunk_overlap,
            length_function=len,
        )
        self.embeddings = HashEmbeddings(settings.embedding_dimensions)
        self.storage = ObjectStorage(settings)

    def recover_stale_jobs(self) -> None:
        with self.pool.connection() as connection, connection.transaction():
            connection.execute(
                """
                UPDATE rag_index_jobs
                SET status = 'PENDING', started_at = NULL,
                    error = 'worker restarted while processing'
                WHERE status = 'RUNNING'
                  AND started_at < CURRENT_TIMESTAMP - (%s * INTERVAL '1 minute')
                """,
                (self.settings.stale_job_minutes,),
            )

    def process_once(self) -> bool:
        job = self._claim_job()
        if job is None:
            return False
        try:
            chunks = self._build_chunks(job)
            self._complete(job, chunks)
            logger.info("indexed resource=%s chunks=%s", job["resource_id"], len(chunks))
        except Exception as exception:  # worker boundary: persist a concise failure for the Java API
            logger.exception("indexing failed for resource=%s", job["resource_id"])
            self._fail(job, str(exception)[:1000])
        return True

    def _claim_job(self) -> dict | None:
        with self.pool.connection() as connection, connection.transaction():
            return connection.cursor(row_factory=dict_row).execute(
                """
                WITH next_job AS (
                    SELECT id FROM rag_index_jobs
                    WHERE status = 'PENDING'
                    ORDER BY created_at, id
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                )
                UPDATE rag_index_jobs job
                SET status = 'RUNNING', attempts = attempts + 1,
                    started_at = CURRENT_TIMESTAMP, error = NULL
                FROM next_job
                WHERE job.id = next_job.id
                RETURNING job.id, job.resource_id
                """,
            ).fetchone()

    def _build_chunks(self, job: dict) -> list[dict]:
        with self.pool.connection() as connection:
            resource = connection.cursor(row_factory=dict_row).execute(
                """
                SELECT r.id, r.course_id, r.chapter_id, r.resource_type, r.file_id,
                       f.object_key, f.original_name
                FROM resources r
                JOIN files f ON f.id = r.file_id
                WHERE r.id = %s AND r.ai_index_status = 'PROCESSING'
                """,
                (job["resource_id"],),
            ).fetchone()
        if resource is None:
            raise ValueError("resource is missing, has no file, or indexing was cancelled")
        with self.storage.materialize(resource["object_key"]) as path:
            pages = extract_pages(path, resource["resource_type"])
        documents = [
            Document(page_content=page.text, metadata={"page": page.page})
            for page in pages
            if page.text.strip()
        ]
        split_documents = self.splitter.split_documents(documents)
        if not split_documents:
            raise ValueError("no extractable text found in resource")
        vectors = self.embeddings.embed_documents([document.page_content for document in split_documents])
        return [
            {
                "course_id": resource["course_id"],
                "chapter_id": resource["chapter_id"],
                "resource_id": resource["id"],
                "file_id": resource["file_id"],
                "page": document.metadata.get("page"),
                "chunk_index": index,
                "content": document.page_content,
                "embedding": vector,
            }
            for index, (document, vector) in enumerate(zip(split_documents, vectors, strict=True))
        ]

    def _complete(self, job: dict, chunks: list[dict]) -> None:
        with self.pool.connection() as connection, connection.transaction():
            active = connection.execute(
                "SELECT id FROM rag_index_jobs WHERE id = %s AND status = 'RUNNING' FOR UPDATE",
                (job["id"],),
            ).fetchone()
            if active is None:
                logger.info("discarding cancelled job=%s", job["id"])
                return
            connection.execute("DELETE FROM rag_chunks WHERE resource_id = %s", (job["resource_id"],))
            connection.cursor().executemany(
                """
                INSERT INTO rag_chunks(course_id, chapter_id, resource_id, file_id, page,
                                       chunk_index, content, embedding)
                VALUES (%(course_id)s, %(chapter_id)s, %(resource_id)s, %(file_id)s, %(page)s,
                        %(chunk_index)s, %(content)s, %(embedding)s)
                """,
                chunks,
            )
            connection.execute(
                """
                UPDATE resources
                SET ai_index_status = 'INDEXED', ai_index_error = NULL,
                    indexed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = %s AND ai_index_status = 'PROCESSING'
                """,
                (job["resource_id"],),
            )
            connection.execute(
                """
                UPDATE rag_index_jobs
                SET status = 'SUCCEEDED', finished_at = CURRENT_TIMESTAMP, error = NULL
                WHERE id = %s
                """,
                (job["id"],),
            )

    def _fail(self, job: dict, message: str) -> None:
        with self.pool.connection() as connection, connection.transaction():
            updated = connection.execute(
                """
                UPDATE rag_index_jobs
                SET status = 'FAILED', finished_at = CURRENT_TIMESTAMP, error = %s
                WHERE id = %s AND status = 'RUNNING'
                """,
                (message, job["id"]),
            ).rowcount
            if updated:
                connection.execute(
                    """
                    UPDATE resources
                    SET ai_index_status = 'FAILED', ai_index_error = %s,
                        indexed_at = NULL, updated_at = CURRENT_TIMESTAMP
                    WHERE id = %s AND ai_index_status = 'PROCESSING'
                    """,
                    (message, job["resource_id"]),
                )

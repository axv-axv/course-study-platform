from __future__ import annotations

from dataclasses import dataclass
import math

from psycopg.rows import dict_row
from psycopg_pool import ConnectionPool

from .config import Settings
from .embeddings import HashEmbeddings


@dataclass(frozen=True)
class Source:
    resource_id: int
    resource_title: str
    chapter_id: int | None
    chapter_title: str | None
    page: int | None
    snippet: str
    content: str
    score: float


class RagChatService:
    def __init__(self, pool: ConnectionPool, settings: Settings) -> None:
        self.pool = pool
        self.settings = settings
        self.embeddings = HashEmbeddings(settings.embedding_dimensions)

    def answer(
        self, course_id: int, question: str, chapter_id: int | None = None, resource_id: int | None = None
    ) -> tuple[str, list[Source]]:
        sources = self.search(course_id, question, chapter_id, resource_id)
        if not sources:
            return "当前范围内没有已建立索引的资料，暂时无法根据课程内容回答。", []
        if self.settings.ai_provider == "deepseek":
            return self._deepseek_answer(question, sources), sources
        return self._local_answer(sources), sources

    def search(
        self, course_id: int, question: str, chapter_id: int | None = None, resource_id: int | None = None
    ) -> list[Source]:
        clauses = ["rc.course_id = %s"]
        params: list[object] = [course_id]
        if chapter_id is not None:
            clauses.append("rc.chapter_id = %s")
            params.append(chapter_id)
        if resource_id is not None:
            clauses.append("rc.resource_id = %s")
            params.append(resource_id)
        params.append(self.settings.ai_max_candidates)
        sql = f"""
            SELECT rc.resource_id, r.title AS resource_title, rc.chapter_id,
                   ch.title AS chapter_title, rc.page, rc.content, rc.embedding
            FROM rag_chunks rc
            JOIN resources r ON r.id = rc.resource_id
            LEFT JOIN chapters ch ON ch.id = rc.chapter_id
            WHERE {' AND '.join(clauses)}
            ORDER BY rc.resource_id, rc.chunk_index
            LIMIT %s
        """
        with self.pool.connection() as connection:
            rows = connection.cursor(row_factory=dict_row).execute(sql, params).fetchall()
        query_vector = self.embeddings.embed_query(question)
        ranked = sorted(
            ((self._cosine(query_vector, row["embedding"]), row) for row in rows),
            key=lambda item: item[0],
            reverse=True,
        )[: self.settings.ai_top_k]
        return [
            Source(
                resource_id=row["resource_id"],
                resource_title=row["resource_title"],
                chapter_id=row["chapter_id"],
                chapter_title=row["chapter_title"],
                page=row["page"],
                snippet=self._snippet(row["content"]),
                content=row["content"],
                score=score,
            )
            for score, row in ranked
        ]

    def _deepseek_answer(self, question: str, sources: list[Source]) -> str:
        if not self.settings.deepseek_api_key:
            raise ValueError("AI_PROVIDER=deepseek requires DEEPSEEK_API_KEY")
        from langchain_openai import ChatOpenAI

        context = "\n\n".join(
            f"[{index}] 资料：{source.resource_title}，页码：{source.page or '无'}\n{source.content}"
            for index, source in enumerate(sources, start=1)
        )
        model = ChatOpenAI(
            api_key=self.settings.deepseek_api_key,
            base_url=self.settings.deepseek_base_url,
            model=self.settings.deepseek_model,
            temperature=0,
            timeout=self.settings.deepseek_timeout_seconds,
        )
        response = model.invoke(
            "你是课程学习助手。只能依据给定资料回答；资料不足时明确说明。"
            "回答使用中文，并在相关结论后标注 [1]、[2] 形式的来源编号。\n\n"
            f"课程资料：\n{context}\n\n学生问题：{question}"
        )
        return str(response.content).strip()

    def _local_answer(self, sources: list[Source]) -> str:
        sections = ["本地测试模式已从课程资料中找到以下相关内容："]
        for index, source in enumerate(sources[:3], start=1):
            sections.append(f"[{index}] {source.resource_title}：{source.snippet}")
        sections.append("配置 DeepSeek API Key 后，可由云端模型基于这些来源生成归纳回答。")
        return "\n\n".join(sections)

    @staticmethod
    def _cosine(left: list[float], right: list[float]) -> float:
        if not left or not right or len(left) != len(right):
            return 0.0
        denominator = math.sqrt(sum(value * value for value in left)) * math.sqrt(
            sum(value * value for value in right)
        )
        return 0.0 if denominator == 0 else sum(a * b for a, b in zip(left, right, strict=True)) / denominator

    @staticmethod
    def _snippet(content: str, limit: int = 240) -> str:
        compact = " ".join(content.split())
        return compact if len(compact) <= limit else compact[: limit - 1] + "…"

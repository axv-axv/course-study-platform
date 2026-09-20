from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
import logging
import hmac

from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
from psycopg_pool import ConnectionPool

from .config import Settings
from .chat import RagChatService
from .indexer import RagIndexer

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
settings = Settings.from_env()
pool = ConnectionPool(conninfo=settings.database_url, min_size=1, max_size=4, open=False)
indexer = RagIndexer(pool, settings)
chat_service = RagChatService(pool, settings)


class ChatRequest(BaseModel):
    courseId: int = Field(gt=0)
    chapterId: int | None = Field(default=None, gt=0)
    resourceId: int | None = Field(default=None, gt=0)
    question: str = Field(min_length=1, max_length=2000)


class SourceResponse(BaseModel):
    resourceId: int
    resourceTitle: str
    chapterId: int | None
    chapterTitle: str | None
    page: int | None
    snippet: str


class ChatResponse(BaseModel):
    answer: str
    sources: list[SourceResponse]


async def poll_jobs(stop: asyncio.Event) -> None:
    while not stop.is_set():
        processed = await asyncio.to_thread(indexer.process_once)
        if not processed:
            try:
                await asyncio.wait_for(stop.wait(), timeout=settings.poll_interval_seconds)
            except TimeoutError:
                pass


@asynccontextmanager
async def lifespan(_: FastAPI):
    pool.open(wait=True)
    indexer.recover_stale_jobs()
    stop = asyncio.Event()
    task = asyncio.create_task(poll_jobs(stop))
    try:
        yield
    finally:
        stop.set()
        await task
        pool.close()


app = FastAPI(title="Course Platform RAG Worker", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health() -> dict[str, str]:
    with pool.connection() as connection:
        connection.execute("SELECT 1").fetchone()
    return {"status": "UP"}


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest, x_worker_token: str = Header(default="")) -> ChatResponse:
    if not hmac.compare_digest(x_worker_token, settings.worker_internal_token):
        raise HTTPException(status_code=401, detail="invalid worker token")
    answer, sources = chat_service.answer(
        request.courseId, request.question.strip(), request.chapterId, request.resourceId
    )
    return ChatResponse(
        answer=answer,
        sources=[
            SourceResponse(
                resourceId=source.resource_id,
                resourceTitle=source.resource_title,
                chapterId=source.chapter_id,
                chapterTitle=source.chapter_title,
                page=source.page,
                snippet=source.snippet,
            )
            for source in sources
        ],
    )

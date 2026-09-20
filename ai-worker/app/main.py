from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI
from psycopg_pool import ConnectionPool

from .config import Settings
from .indexer import RagIndexer

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
settings = Settings.from_env()
pool = ConnectionPool(conninfo=settings.database_url, min_size=1, max_size=4, open=False)
indexer = RagIndexer(pool, settings)


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

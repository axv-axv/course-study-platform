from dataclasses import dataclass
import os
from pathlib import Path
from urllib.parse import quote_plus


@dataclass(frozen=True)
class Settings:
    database_url: str
    storage_root: Path
    poll_interval_seconds: float
    stale_job_minutes: int
    chunk_size: int
    chunk_overlap: int
    embedding_dimensions: int
    ai_provider: str
    ai_top_k: int
    ai_max_candidates: int
    worker_internal_token: str
    deepseek_api_key: str
    deepseek_base_url: str
    deepseek_model: str
    deepseek_timeout_seconds: float

    @classmethod
    def from_env(cls) -> "Settings":
        database_url = os.getenv("DATABASE_URL")
        if not database_url:
            username = quote_plus(os.getenv("DB_USERNAME", "course_platform"))
            password = quote_plus(os.getenv("DB_PASSWORD", "course_platform_dev"))
            host = os.getenv("DB_HOST", "localhost")
            port = os.getenv("DB_PORT", "5432")
            database = quote_plus(os.getenv("DB_NAME", "course_platform"))
            database_url = f"postgresql://{username}:{password}@{host}:{port}/{database}"
        return cls(
            database_url=database_url,
            storage_root=Path(os.getenv("STORAGE_ROOT", "./data/files")).resolve(),
            poll_interval_seconds=float(os.getenv("RAG_POLL_INTERVAL_SECONDS", "1")),
            stale_job_minutes=int(os.getenv("RAG_STALE_JOB_MINUTES", "15")),
            chunk_size=int(os.getenv("RAG_CHUNK_SIZE", "800")),
            chunk_overlap=int(os.getenv("RAG_CHUNK_OVERLAP", "120")),
            embedding_dimensions=int(os.getenv("RAG_EMBEDDING_DIMENSIONS", "384")),
            ai_provider=os.getenv("AI_PROVIDER", "local").lower(),
            ai_top_k=int(os.getenv("AI_TOP_K", "5")),
            ai_max_candidates=int(os.getenv("AI_MAX_CANDIDATES", "500")),
            worker_internal_token=os.getenv("AI_WORKER_INTERNAL_TOKEN", "local-worker-token-change-me"),
            deepseek_api_key=os.getenv("DEEPSEEK_API_KEY", ""),
            deepseek_base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
            deepseek_model=os.getenv("DEEPSEEK_MODEL", "deepseek-flash"),
            deepseek_timeout_seconds=float(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "60")),
        )

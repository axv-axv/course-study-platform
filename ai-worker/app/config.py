from dataclasses import dataclass
import os
from pathlib import Path
from urllib.parse import quote_plus


@dataclass(frozen=True)
class Settings:
    database_url: str
    storage_provider: str
    storage_root: Path
    oss_region: str
    oss_bucket: str
    oss_endpoint: str
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
        settings = cls(
            database_url=database_url,
            storage_provider=os.getenv("STORAGE_PROVIDER", "local").lower(),
            storage_root=Path(os.getenv("STORAGE_ROOT", "./data/files")).resolve(),
            oss_region=os.getenv("OSS_REGION", ""),
            oss_bucket=os.getenv("OSS_BUCKET", ""),
            oss_endpoint=os.getenv("OSS_ENDPOINT", ""),
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
        settings.validate()
        return settings

    def validate(self) -> None:
        if self.storage_provider not in {"local", "oss"}:
            raise ValueError("STORAGE_PROVIDER must be local or oss")
        if self.storage_provider == "oss":
            missing = [
                name
                for name, value in (
                    ("OSS_REGION", self.oss_region),
                    ("OSS_BUCKET", self.oss_bucket),
                    ("OSS_ACCESS_KEY_ID", os.getenv("OSS_ACCESS_KEY_ID", "")),
                    ("OSS_ACCESS_KEY_SECRET", os.getenv("OSS_ACCESS_KEY_SECRET", "")),
                )
                if not value
            ]
            if missing:
                raise ValueError(f"OSS storage requires: {', '.join(missing)}")
        if self.ai_provider not in {"local", "deepseek"}:
            raise ValueError("AI_PROVIDER must be local or deepseek")
        if self.ai_provider == "deepseek" and not self.deepseek_api_key:
            raise ValueError("DEEPSEEK_API_KEY is required when AI_PROVIDER=deepseek")
        if not 1 <= self.ai_top_k <= 20:
            raise ValueError("AI_TOP_K must be between 1 and 20")

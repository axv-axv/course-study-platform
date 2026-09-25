from __future__ import annotations

from contextlib import contextmanager
from pathlib import Path
import tempfile
from typing import Iterator

from .config import Settings


class ObjectStorage:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._oss = None
        self._client = None
        if settings.storage_provider == "oss":
            import alibabacloud_oss_v2 as oss

            config = oss.config.load_default()
            config.credentials_provider = oss.credentials.EnvironmentVariableCredentialsProvider()
            config.region = settings.oss_region
            if settings.oss_endpoint:
                config.endpoint = settings.oss_endpoint
            self._oss = oss
            self._client = oss.Client(config)

    @contextmanager
    def materialize(self, object_key: str) -> Iterator[Path]:
        if self.settings.storage_provider == "local":
            path = (self.settings.storage_root / object_key).resolve()
            if not path.is_relative_to(self.settings.storage_root):
                raise ValueError("invalid storage object key")
            if not path.is_file():
                raise FileNotFoundError(f"stored file not found: {object_key}")
            yield path
            return

        assert self._oss is not None and self._client is not None
        temporary = tempfile.NamedTemporaryFile(prefix="rag-source-", suffix=Path(object_key).suffix, delete=False)
        path = Path(temporary.name)
        temporary.close()
        try:
            result = self._client.get_object(
                self._oss.GetObjectRequest(bucket=self.settings.oss_bucket, key=object_key)
            )
            with result.body as body, path.open("wb") as output:
                while chunk := body.read(1024 * 1024):
                    output.write(chunk)
            yield path
        finally:
            path.unlink(missing_ok=True)

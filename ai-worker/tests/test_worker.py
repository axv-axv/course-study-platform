import math
from io import BytesIO
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
from unittest.mock import MagicMock

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.embeddings import HashEmbeddings
from app.extractors import extract_pages
from app.config import Settings
from app.chat import RagChatService, Source
from app.storage import ObjectStorage


class WorkerUnitTest(unittest.TestCase):
    def test_hash_embeddings_are_deterministic_and_normalized(self) -> None:
        embeddings = HashEmbeddings(64)
        first = embeddings.embed_query("梯度下降 gradient descent")
        second = embeddings.embed_query("梯度下降 gradient descent")

        self.assertEqual(first, second)
        self.assertAlmostEqual(math.sqrt(sum(value * value for value in first)), 1.0)

    def test_extracts_utf8_text(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "lesson.txt"
            path.write_text("第一章：线性回归", encoding="utf-8")

            pages = extract_pages(path, "TXT")

        self.assertEqual(pages[0].text, "第一章：线性回归")
        self.assertIsNone(pages[0].page)

    def test_langchain_splitter_preserves_page_metadata(self) -> None:
        splitter = RecursiveCharacterTextSplitter(chunk_size=20, chunk_overlap=4)
        chunks = splitter.split_documents([Document(page_content="课程内容 " * 20, metadata={"page": 3})])

        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(chunk.metadata["page"] == 3 for chunk in chunks))

    def test_database_credentials_are_url_encoded(self) -> None:
        with patch.dict(
            "os.environ",
            {"DB_USERNAME": "course user", "DB_PASSWORD": "p@ss/word", "DB_NAME": "course db"},
            clear=True,
        ):
            settings = Settings.from_env()

        self.assertEqual(
            settings.database_url,
            "postgresql://course+user:p%40ss%2Fword@localhost:5432/course+db",
        )

    def test_deepseek_requires_api_key(self) -> None:
        with patch.dict("os.environ", {"AI_PROVIDER": "deepseek"}, clear=True):
            with self.assertRaisesRegex(ValueError, "DEEPSEEK_API_KEY"):
                Settings.from_env()

    def test_oss_requires_complete_credentials(self) -> None:
        with patch.dict(
            "os.environ",
            {"STORAGE_PROVIDER": "oss", "OSS_REGION": "cn-hangzhou"},
            clear=True,
        ):
            with self.assertRaisesRegex(ValueError, "OSS_BUCKET"):
                Settings.from_env()

    def test_local_storage_materializes_only_files_below_root(self) -> None:
        with tempfile.TemporaryDirectory() as directory, patch.dict(
            "os.environ", {"STORAGE_ROOT": directory}, clear=True
        ):
            path = Path(directory) / "ab" / "lesson.txt"
            path.parent.mkdir()
            path.write_text("课程资料", encoding="utf-8")
            storage = ObjectStorage(Settings.from_env())

            with storage.materialize("ab/lesson.txt") as materialized:
                self.assertEqual(materialized.read_text(encoding="utf-8"), "课程资料")
            with self.assertRaisesRegex(ValueError, "invalid storage object key"):
                with storage.materialize("../outside.txt"):
                    pass

    def test_oss_storage_downloads_to_a_disposable_file(self) -> None:
        environment = {
            "STORAGE_PROVIDER": "oss",
            "OSS_REGION": "cn-hangzhou",
            "OSS_BUCKET": "course-bucket",
            "OSS_ACCESS_KEY_ID": "test-id",
            "OSS_ACCESS_KEY_SECRET": "test-secret",
        }
        with patch.dict("os.environ", environment, clear=True):
            storage = ObjectStorage(Settings.from_env())
        storage._client = MagicMock()
        storage._client.get_object.return_value = SimpleNamespace(body=BytesIO("云端资料".encode()))

        with storage.materialize("course-platform/ab/file.txt") as path:
            temporary = path
            self.assertEqual(path.read_text(encoding="utf-8"), "云端资料")

        self.assertFalse(temporary.exists())

    def test_local_chat_uses_ranked_sources_without_cloud_key(self) -> None:
        settings = Settings.from_env()
        service = RagChatService(MagicMock(), settings)
        source = Source(1, "线性代数", 2, "向量", 3, "向量是有方向的量", "向量是有方向的量", 0.9)
        with patch.object(service, "search", return_value=[source]):
            answer, sources = service.answer(11, "什么是向量？")

        self.assertIn("线性代数", answer)
        self.assertEqual(sources, [source])

    def test_chat_returns_grounded_empty_result(self) -> None:
        service = RagChatService(MagicMock(), Settings.from_env())
        with patch.object(service, "search", return_value=[]):
            answer, sources = service.answer(11, "没有资料的问题")

        self.assertIn("没有已建立索引的资料", answer)
        self.assertEqual(sources, [])


if __name__ == "__main__":
    unittest.main()

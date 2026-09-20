import math
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.embeddings import HashEmbeddings
from app.extractors import extract_pages
from app.config import Settings


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


if __name__ == "__main__":
    unittest.main()

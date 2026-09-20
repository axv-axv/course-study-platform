from __future__ import annotations

import hashlib
import math
import re

from langchain_core.embeddings import Embeddings


class HashEmbeddings(Embeddings):
    """Deterministic local embeddings for development and repeatable Docker tests.

    M7 can replace this provider without changing the chunk persistence contract.
    """

    def __init__(self, dimensions: int = 384) -> None:
        if dimensions < 32:
            raise ValueError("embedding dimensions must be at least 32")
        self.dimensions = dimensions

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed(text)

    def _embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        for token in self._tokens(text):
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "big") % self.dimensions
            sign = 1.0 if digest[4] & 1 else -1.0
            vector[index] += sign
        norm = math.sqrt(sum(value * value for value in vector))
        return vector if norm == 0 else [value / norm for value in vector]

    def _tokens(self, text: str) -> list[str]:
        normalized = text.lower().strip()
        words = re.findall(r"[a-z0-9_]+", normalized)
        chinese = [char for char in normalized if "\u4e00" <= char <= "\u9fff"]
        chinese_bigrams = ["".join(chinese[i : i + 2]) for i in range(max(0, len(chinese) - 1))]
        return words + chinese + chinese_bigrams

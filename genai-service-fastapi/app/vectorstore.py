import os
import re
import math
import hashlib
import logging
from pathlib import Path
from typing import List, Dict, Any, Tuple

logger = logging.getLogger(__name__)

class Document:
    def __init__(self, page_content: str, metadata: Dict[str, Any] = None):
        self.page_content = page_content
        self.metadata = metadata or {}

class FallbackDeterministicEmbeddings:
    """
    Lightweight, deterministic hashing vectorizer (384 dimensions) that runs
    instantly in all environments without external neural network weights or GPU/network.
    """
    def __init__(self, dim: int = 384):
        self.dim = dim

    def embed_query(self, text: str) -> List[float]:
        return self._embed(text)

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return [self._embed(t) for t in texts]

    def _embed(self, text: str) -> List[float]:
        vec = [0.0] * self.dim
        tokens = re.findall(r"\w+", text.lower())
        if not tokens:
            return vec

        for token in tokens:
            h = int(hashlib.md5(token.encode("utf-8")).hexdigest(), 16)
            idx = h % self.dim
            val = 1.0 if ((h >> 8) & 1) else -1.0
            vec[idx] += val

        # L2 normalize
        norm = math.sqrt(sum(v * v for v in vec))
        if norm > 0:
            vec = [v / norm for v in vec]
        return vec

class LocalMemoryVectorStore:
    """
    High-performance in-memory and Chroma-compatible vector store for MyGaadi knowledge base.
    """
    def __init__(self, embeddings = None):
        self.embeddings = embeddings or FallbackDeterministicEmbeddings()
        self.documents: List[Document] = []
        self.vectors: List[List[float]] = []

    def add_documents(self, docs: List[Document]):
        if not docs:
            return
        texts = [doc.page_content for doc in docs]
        embeds = self.embeddings.embed_documents(texts)
        self.documents.extend(docs)
        self.vectors.extend(embeds)
        logger.info(f"VectorStore indexed {len(docs)} document chunks (Total: {len(self.documents)})")

    def similarity_search_with_score(self, query: str, k: int = 5) -> List[Tuple[Document, float]]:
        if not self.documents:
            return []

        q_vec = self.embeddings.embed_query(query)
        scored = []

        for doc, vec in zip(self.documents, self.vectors):
            dot_product = sum(q * v for q, v in zip(q_vec, vec))
            scored.append((doc, dot_product))

        # Sort descending by cosine similarity score
        scored.sort(key=lambda x: x[1], reverse=True)
        return scored[:k]

    def similarity_search(self, query: str, k: int = 5) -> List[Document]:
        return [doc for doc, _ in self.similarity_search_with_score(query, k)]

def split_text_into_chunks(text: str, chunk_size: int = 600, overlap: int = 80) -> List[str]:
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunk = text[start:end]
        chunks.append(chunk.strip())
        start += (chunk_size - overlap)
    return [c for c in chunks if c]

def build_knowledge_base_store(kb_dir: str, car_inventory: List[Dict[str, Any]] = None) -> LocalMemoryVectorStore:
    store = LocalMemoryVectorStore()
    docs: List[Document] = []

    # 1. Ingest Markdown Knowledge Base files
    kb_path = Path(kb_dir)
    if kb_path.exists():
        for md_file in kb_path.glob("*.md"):
            try:
                content = md_file.read_text(encoding="utf-8")
                chunks = split_text_into_chunks(content, chunk_size=600, overlap=80)
                for chunk in chunks:
                    docs.append(Document(
                        page_content=chunk,
                        metadata={"source": md_file.name}
                    ))
            except Exception as e:
                logger.error(f"Error reading {md_file}: {e}")

    # 2. Ingest Live Car Inventory (without seller PII)
    if car_inventory:
        for car in car_inventory:
            if isinstance(car, dict):
                price_val = car.get('price', 0)
                km_val = car.get('kmDriven', 0)
                formatted_price = f"₹{price_val:,}" if isinstance(price_val, (int, float)) else f"₹{price_val}"
                formatted_km = f"{km_val:,} km" if isinstance(km_val, (int, float)) else f"{km_val} km"
                car_text = (
                    f"Car ID: {car.get('id')} | {car.get('title', '')} | Brand: {car.get('brand')} | "
                    f"Model: {car.get('model')} | Year: {car.get('year')} | Price: {formatted_price} | "
                    f"Kilometers: {formatted_km} | Fuel: {car.get('fuelType')} | "
                    f"Transmission: {car.get('transmission')} | Body: {car.get('bodyType')} | "
                    f"Location: {car.get('city')}, {car.get('state')} | Color: {car.get('color')} | "
                    f"Description: {car.get('description', '')}"
                )
                docs.append(Document(
                    page_content=car_text,
                    metadata={"source": "live_car_inventory", "car_id": car.get("id")}
                ))
            elif isinstance(car, str):
                docs.append(Document(
                    page_content=car,
                    metadata={"source": "live_car_inventory"}
                ))

    store.add_documents(docs)
    return store

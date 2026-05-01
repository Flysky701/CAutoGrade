import os
import chromadb
from chromadb.config import Settings

_client = None
_collection_name = "c_programming_knowledge"


def _get_chroma_client():
    global _client
    if _client is None:
        host = os.getenv("CHROMA_HOST")
        port = os.getenv("CHROMA_PORT")
        if host and port:
            _client = chromadb.HttpClient(
                host=host,
                port=int(port),
                settings=Settings(anonymized_telemetry=False),
            )
        else:
            persist_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "chroma_data")
            os.makedirs(persist_dir, exist_ok=True)
            _client = chromadb.PersistentClient(
                path=persist_dir,
                settings=Settings(anonymized_telemetry=False),
            )
    return _client


def get_collection():
    client = _get_chroma_client()
    existing = [c.name for c in client.list_collections()]
    if _collection_name not in existing:
        return client.create_collection(
            name=_collection_name,
            metadata={"description": "C programming course knowledge base"},
        )
    return client.get_collection(_collection_name)


def add_documents(docs: list[dict]) -> None:
    if not docs:
        return
    collection = get_collection()
    ids = [d["id"] for d in docs]
    contents = [d["content"] for d in docs]
    metadatas = [d.get("metadata", {}) for d in docs]
    embeddings = [d.get("embedding") for d in docs]

    try:
        existing = collection.get(ids=ids)
        if existing and existing["ids"]:
            collection.delete(ids=[i for i in ids if i in existing["ids"]])
    except Exception:
        pass

    collection.add(ids=ids, documents=contents, metadatas=metadatas, embeddings=embeddings or None)


def search_by_embedding(query_embedding: list[float], n_results: int = 10) -> list[dict]:
    collection = get_collection()
    try:
        results = collection.query(query_embeddings=[query_embedding], n_results=n_results)
        if not results or not results["ids"] or not results["ids"][0]:
            return []
        docs = []
        for i, doc_id in enumerate(results["ids"][0]):
            docs.append({
                "id": doc_id,
                "content": results["documents"][0][i] if results.get("documents") and results["documents"][0] else "",
                "metadata": results["metadatas"][0][i] if results.get("metadatas") and results["metadatas"][0] else {},
                "distance": results["distances"][0][i] if results.get("distances") and results["distances"][0] else None,
            })
        return docs
    except Exception:
        return []


def search_by_keyword(keywords: list[str], n_results: int = 10) -> list[dict]:
    if not keywords:
        return []
    collection = get_collection()
    try:
        count = collection.count()
        if count == 0:
            return []
        all_docs = collection.get(limit=max(1, min(count, 500)))
        if not all_docs or not all_docs["ids"]:
            return []
    except Exception:
        return []

    scored = []
    for i, doc_id in enumerate(all_docs["ids"]):
        content = all_docs["documents"][i] if all_docs.get("documents") else ""
        meta = all_docs["metadatas"][i] if all_docs.get("metadatas") else {}
        tags = meta.get("tags", []) if isinstance(meta, dict) else []
        score = sum(1 for kw in keywords if kw.lower() in content.lower())
        score += sum(2 for kw in keywords for tag in tags if kw.lower() in str(tag).lower())
        if score > 0:
            scored.append({"id": doc_id, "content": content, "metadata": meta, "keyword_score": score})

    scored.sort(key=lambda x: x["keyword_score"], reverse=True)
    return scored[:n_results]

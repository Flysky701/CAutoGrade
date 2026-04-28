"""Knowledge base loader for C programming course.

Loads predefined knowledge documents (grading rubrics, common errors,
knowledge points) into the ChromaDB vector store.
"""
import json
import os
from .embedding import embed_texts
from .vector_store import add_documents, get_collection

_loaded = False

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")


def _read_json(file_name: str) -> list[dict]:
    path = os.path.join(DATA_DIR, file_name)
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_knowledge_base(force_reload: bool = False) -> None:
    """Load all knowledge base documents into the vector store."""
    global _loaded
    if _loaded and not force_reload:
        return

    collection = get_collection()
    if collection.count() > 0 and not force_reload:
        _loaded = True
        return

    docs = []

    # Load grading rubrics
    rubrics = _read_json("grading_rubrics.json")
    for item in rubrics:
        docs.append({
            "id": f"rubric_{item.get('id', hash(item.get('content', '')))}",
            "content": item.get("content", ""),
            "metadata": {
                "type": "rubric",
                "tags": item.get("tags", []),
                "dimension": item.get("dimension", ""),
            },
        })

    # Load knowledge points
    knowledge = _read_json("knowledge_points.json")
    for item in knowledge:
        docs.append({
            "id": f"kp_{item.get('id', hash(item.get('content', '')))}",
            "content": item.get("content", ""),
            "metadata": {
                "type": "knowledge_point",
                "tags": item.get("tags", []),
                "difficulty": item.get("difficulty", 3),
            },
        })

    # Load common error patterns
    errors = _read_json("common_errors.json")
    for item in errors:
        docs.append({
            "id": f"error_{item.get('id', hash(item.get('content', '')))}",
            "content": item.get("content", ""),
            "metadata": {
                "type": "common_error",
                "tags": item.get("tags", []),
                "severity": item.get("severity", "warning"),
            },
        })

    if docs:
        embeddings = embed_texts([d["content"] for d in docs])
        for i, doc in enumerate(docs):
            doc["embedding"] = embeddings[i]
        add_documents(docs)

    _loaded = True


def is_loaded() -> bool:
    return _loaded

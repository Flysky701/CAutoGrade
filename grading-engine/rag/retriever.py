"""Hybrid retriever: combines vector similarity and keyword matching."""
from .embedding import embed_query
from .vector_store import search_by_embedding, search_by_keyword


def retrieve(problem_description: str, student_code: str, knowledge_tags: list[str] | None = None,
             top_k: int = 8) -> dict:
    """Retrieve relevant knowledge for grading context.

    Uses hybrid retrieval: vector similarity on the problem + code,
    combined with keyword matching on knowledge tags.

    Returns a dict with rubric, knowledge_points, and common_errors.
    """
    query_text = f"题目：{problem_description}\n代码：{student_code}"
    query_embedding = embed_query(query_text)

    vector_results = search_by_embedding(query_embedding, n_results=top_k * 2)
    keyword_results = search_by_keyword(knowledge_tags or [], n_results=top_k)

    # Merge results (vector first, deduplicate by id)
    seen_ids: set[str] = set()
    merged: list[dict] = []
    for r in vector_results:
        if r["id"] not in seen_ids:
            seen_ids.add(r["id"])
            merged.append(r)
    for r in keyword_results:
        if r["id"] not in seen_ids and len(merged) < top_k:
            seen_ids.add(r["id"])
            merged.append(r)

    # Categorize by type
    rubric_parts: list[str] = []
    knowledge_points: list[str] = []
    common_errors: list[dict] = []

    for r in merged[:top_k]:
        meta = r.get("metadata", {})
        doc_type = meta.get("type", "")

        if doc_type == "rubric":
            rubric_parts.append(r["content"])
        elif doc_type == "knowledge_point":
            knowledge_points.append(r["content"])
        elif doc_type == "common_error":
            common_errors.append({
                "content": r["content"],
                "severity": meta.get("severity", "warning"),
                "tags": meta.get("tags", []),
            })

    return {
        "rubric": "\n".join(rubric_parts) if rubric_parts else "正确性60分，规范性20分，效率20分",
        "knowledge_points": knowledge_points if knowledge_points else (knowledge_tags or []),
        "common_errors": common_errors,
    }

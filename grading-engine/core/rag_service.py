"""RAG service — wraps the RAG module for use by the grading dispatcher."""
from rag import load_knowledge_base, retrieve


class RAGService:
    def __init__(self):
        self._initialized = False

    def _ensure_loaded(self):
        if not self._initialized:
            load_knowledge_base()
            self._initialized = True

    def retrieve(self, problem_description: str, student_code: str,
                 knowledge_tags: list[str] | None = None) -> dict:
        self._ensure_loaded()
        result = retrieve(problem_description, student_code, knowledge_tags)
        return {
            "problem_id": None,
            "description": problem_description,
            "rubric": result["rubric"],
            "knowledge_points": result["knowledge_points"],
            "common_errors": result["common_errors"],
        }

    def reload(self):
        """Force reload the knowledge base."""
        load_knowledge_base(force_reload=True)
        self._initialized = True

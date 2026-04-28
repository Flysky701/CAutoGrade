"""RAG module initialization and public API."""
from .knowledge_loader import load_knowledge_base, is_loaded
from .retriever import retrieve

__all__ = ["load_knowledge_base", "is_loaded", "retrieve"]

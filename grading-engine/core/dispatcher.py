from core.llm_service import LLMService
from core.rag_service import RAGService
from core.sandbox import SandboxRunner
from core.scorer import RuleScorer
from core.static_analyzer import StaticAnalyzer


class Dispatcher:
    def __init__(self, llm_service: LLMService):
        self.static_analyzer = StaticAnalyzer()
        self.rag_service = RAGService()
        self.sandbox_runner = SandboxRunner()
        self.rule_scorer = RuleScorer()
        self.llm_service = llm_service

    def grade(self, submission_id: int, code_content: str, problem_id: int,
              test_cases: list[dict] = None,
              problem_description: str = "", knowledge_tags: list[str] = None) -> dict:
        """Run the full grading pipeline and return structured results."""
        test_cases = test_cases or []
        knowledge_tags = knowledge_tags or []

        # Layer 1: Static analysis + test case execution
        static_result = self.static_analyzer.analyze(code_content, test_cases)

        # Convert test case results to the format expected by downstream consumers
        test_case_summary = []
        for r in static_result.test_case_results:
            test_case_summary.append({
                "id": r.case_id,
                "passed": r.passed,
                "input": r.input_data[:200],
                "expected": r.expected[:200],
                "actual": r.actual[:200],
                "weight": r.weight,
            })

        # Layer 2: RAG context retrieval
        rag_context = self.rag_service.retrieve(
            problem_description=problem_description,
            student_code=code_content,
            knowledge_tags=knowledge_tags,
        )

        # Layer 3: LLM evaluation
        llm_feedback = self.llm_service.evaluate(
            code_content=code_content,
            static_result={
                "compile_success": static_result.compile_success,
                "compile_error": static_result.compile_error,
                "passed_cases": static_result.passed_cases,
                "total_cases": static_result.total_cases,
                "warnings": static_result.warnings,
                "test_case_details": test_case_summary,
            },
            rag_context=rag_context,
        )

        # Fallback: rule-based scoring when LLM output deviates
        fallback_score = self.rule_scorer.score({
            "passed_cases": static_result.passed_cases,
            "total_cases": static_result.total_cases,
        })

        return {
            "submission_id": submission_id,
            "status": "DONE",
            "static_result": {
                "compile_success": static_result.compile_success,
                "compile_error": static_result.compile_error,
                "passed_cases": static_result.passed_cases,
                "total_cases": static_result.total_cases,
                "warnings": static_result.warnings,
                "test_cases": test_case_summary,
            },
            "llm_feedback": llm_feedback,
            "fallback_score": fallback_score,
        }

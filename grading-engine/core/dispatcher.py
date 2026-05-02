import logging

from core.llm_service import LLMService
from core.rag_service import RAGService
from core.sandbox import SandboxRunner
from core.scorer import RuleScorer
from core.static_analyzer import StaticAnalyzer

logger = logging.getLogger(__name__)


class Dispatcher:
    def __init__(self, llm_service: LLMService):
        self.static_analyzer = StaticAnalyzer()
        self.rag_service = RAGService()
        self.sandbox_runner = SandboxRunner()
        self.rule_scorer = RuleScorer()
        self.llm_service = llm_service

    def grade(self, submission_id: int, code_content: str, problem_id: int,
              test_cases: list[dict] = None,
              problem_description: str = "", knowledge_tags: list[str] = None,
              language: str = "c") -> dict:
        test_cases = test_cases or []
        knowledge_tags = knowledge_tags or []

        static_result = self._run_static_analysis(code_content, test_cases, language=language)

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

        rag_context = self._run_rag_retrieval(
            problem_description, code_content, knowledge_tags
        )

        llm_feedback = self._run_llm_evaluation(
            code_content, static_result, test_case_summary, rag_context
        )

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

    def _run_static_analysis(self, code_content: str, test_cases: list[dict], language: str = "c"):
        try:
            return self.static_analyzer.analyze(code_content, test_cases, language=language)
        except Exception as e:
            logger.error(f"Static analysis failed, using fallback: {e}")
            from core.static_analyzer import StaticAnalysisResult
            return StaticAnalysisResult(
                compile_success=False,
                compile_error=f"Static analysis error: {str(e)}",
                passed_cases=0,
                total_cases=len(test_cases),
                warnings=[],
            )

    def _run_rag_retrieval(self, problem_description, code_content, knowledge_tags):
        try:
            return self.rag_service.retrieve(
                problem_description=problem_description,
                student_code=code_content,
                knowledge_tags=knowledge_tags,
            )
        except Exception as e:
            logger.warning(f"RAG retrieval failed, using empty context: {e}")
            return {
                "rubric": "正确性60分，规范性20分，效率20分",
                "knowledge_points": knowledge_tags or [],
                "common_errors": [],
            }

    def _run_llm_evaluation(self, code_content, static_result, test_case_summary, rag_context):
        try:
            return self.llm_service.evaluate(
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
        except Exception as e:
            logger.error(f"LLM evaluation failed: {e}")
            return {"total_score": None, "error": str(e)}

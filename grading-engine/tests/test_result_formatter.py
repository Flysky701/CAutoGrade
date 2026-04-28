"""Tests for grading result formatter."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from utils.result_formatter import format_grading_result


class TestFormatGradingResult:
    def test_format_with_llm_feedback(self):
        dispatcher_output = {
            "submission_id": 1,
            "status": "DONE",
            "static_result": {
                "compile_success": True,
                "compile_error": "",
                "warnings": [],
                "test_cases": [
                    {"id": 1, "passed": True, "input": "1 2", "expected": "3", "actual": "3", "weight": 5},
                ],
            },
            "llm_feedback": {
                "total_score": 95.0,
                "correctness_score": 58.0,
                "style_score": 18.0,
                "efficiency_score": 19.0,
                "summary": "Good job!",
                "line_annotations": [{"line": 1, "message": "OK"}],
                "improvements": ["Add comments"],
            },
            "fallback_score": {},
        }

        result = format_grading_result(dispatcher_output)

        assert result["submission_id"] == 1
        assert result["status"] == "DONE"
        assert result["total_score"] == 95.0
        assert result["used_fallback"] is False
        assert result["summary"] == "Good job!"
        assert len(result["line_annotations"]) == 1
        assert len(result["improvements"]) == 1
        assert len(result["test_case_results"]) == 1
        assert result["test_case_results"][0]["caseId"] == 1
        assert result["test_case_results"][0]["passed"] is True

    def test_format_with_fallback_only(self):
        dispatcher_output = {
            "submission_id": 2,
            "status": "DONE",
            "static_result": {
                "compile_success": False,
                "compile_error": "syntax error",
                "warnings": ["unused variable"],
                "test_cases": [],
            },
            "llm_feedback": {},
            "fallback_score": {
                "total_score": 32.0,
                "correctness_score": 0.0,
                "style_score": 80.0,
                "efficiency_score": 80.0,
            },
        }

        result = format_grading_result(dispatcher_output)

        assert result["submission_id"] == 2
        assert result["total_score"] == 32.0
        assert result["used_fallback"] is True
        assert "LLM evaluation unavailable" in result["summary"]
        assert result["compile_success"] is False
        assert result["compile_error"] == "syntax error"

    def test_format_with_llm_missing_score(self):
        dispatcher_output = {
            "submission_id": 3,
            "status": "DONE",
            "static_result": {"compile_success": True, "compile_error": "", "warnings": [], "test_cases": []},
            "llm_feedback": {"total_score": None, "summary": "test"},
            "fallback_score": {"total_score": 50.0, "correctness_score": 30.0, "style_score": 10.0, "efficiency_score": 10.0},
        }

        result = format_grading_result(dispatcher_output)

        assert result["used_fallback"] is True
        assert result["total_score"] == 50.0

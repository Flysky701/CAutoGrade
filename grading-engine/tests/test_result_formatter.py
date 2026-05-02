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
                "total_cases": 1,
                "passed_cases": 1,
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
        assert result["correctness_score"] == 100.0
        assert result["total_score"] == round(100.0 * 0.6 + 18.0 * 0.2 + 19.0 * 0.2, 2)
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
                "total_cases": 0,
                "passed_cases": 0,
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
        assert result["total_score"] == round(0.0 * 0.6 + 80.0 * 0.2 + 80.0 * 0.2, 2)
        assert result["used_fallback"] is True
        assert "LLM evaluation unavailable" in result["summary"]
        assert result["compile_success"] is False
        assert result["compile_error"] == "syntax error"

    def test_format_with_llm_missing_score(self):
        dispatcher_output = {
            "submission_id": 3,
            "status": "DONE",
            "static_result": {
                "compile_success": True,
                "compile_error": "",
                "warnings": [],
                "test_cases": [],
                "total_cases": 0,
                "passed_cases": 0,
            },
            "llm_feedback": {"total_score": None, "summary": "test"},
            "fallback_score": {"total_score": 50.0, "correctness_score": 30.0, "style_score": 10.0, "efficiency_score": 10.0},
        }

        result = format_grading_result(dispatcher_output)

        assert result["used_fallback"] is True
        assert result["correctness_score"] == 30.0
        assert result["total_score"] == round(30.0 * 0.6 + 10.0 * 0.2 + 10.0 * 0.2, 2)

    def test_correctness_uses_pass_rate_when_test_cases_exist(self):
        dispatcher_output = {
            "submission_id": 4,
            "status": "DONE",
            "static_result": {
                "compile_success": True,
                "compile_error": "",
                "warnings": [],
                "test_cases": [
                    {"id": 1, "passed": True, "input": "1 2", "expected": "3", "actual": "3"},
                    {"id": 2, "passed": True, "input": "3 4", "expected": "7", "actual": "7"},
                    {"id": 3, "passed": False, "input": "5 6", "expected": "11", "actual": "10"},
                ],
                "total_cases": 3,
                "passed_cases": 2,
            },
            "llm_feedback": {
                "total_score": 90.0,
                "correctness_score": 90.0,
                "style_score": 90.0,
                "efficiency_score": 90.0,
                "summary": "test",
            },
            "fallback_score": {},
        }

        result = format_grading_result(dispatcher_output)

        assert result["correctness_score"] == round(100 * 2 / 3, 2)
        assert result["used_fallback"] is False

    def test_correctness_uses_llm_when_no_test_cases(self):
        dispatcher_output = {
            "submission_id": 5,
            "status": "DONE",
            "static_result": {
                "compile_success": True,
                "compile_error": "",
                "warnings": [],
                "test_cases": [],
                "total_cases": 0,
                "passed_cases": 0,
            },
            "llm_feedback": {
                "total_score": 85.0,
                "correctness_score": 70.0,
                "style_score": 90.0,
                "efficiency_score": 80.0,
                "summary": "test",
            },
            "fallback_score": {},
        }

        result = format_grading_result(dispatcher_output)

        assert result["correctness_score"] == 70.0
        assert result["used_fallback"] is False

    def test_total_score_recalculated_by_formula(self):
        dispatcher_output = {
            "submission_id": 6,
            "status": "DONE",
            "static_result": {
                "compile_success": True,
                "compile_error": "",
                "warnings": [],
                "test_cases": [
                    {"id": 1, "passed": True},
                ],
                "total_cases": 1,
                "passed_cases": 1,
            },
            "llm_feedback": {
                "total_score": 50.0,
                "correctness_score": 50.0,
                "style_score": 50.0,
                "efficiency_score": 50.0,
                "summary": "test",
            },
            "fallback_score": {},
        }

        result = format_grading_result(dispatcher_output)

        assert result["correctness_score"] == 100.0
        expected_total = round(100.0 * 0.6 + 50.0 * 0.2 + 50.0 * 0.2, 2)
        assert result["total_score"] == expected_total

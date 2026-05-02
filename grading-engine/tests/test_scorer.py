"""Tests for RuleScorer fallback scoring."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from core.scorer import RuleScorer


class TestRuleScorer:
    scorer = RuleScorer()

    def test_all_cases_pass(self):
        result = self.scorer.score({
            "passed_cases": 5,
            "total_cases": 5,
        })
        assert result["correctness_score"] == 100.0
        assert result["total_score"] == 92.0
        assert result["fallback"] is True
        assert result["style_score"] == 80.0
        assert result["efficiency_score"] == 80.0

    def test_half_cases_pass(self):
        result = self.scorer.score({
            "passed_cases": 2,
            "total_cases": 4,
        })
        assert result["correctness_score"] == 50.0
        expected_total = round(50.0 * 0.6 + 80.0 * 0.2 + 80.0 * 0.2, 2)
        assert result["total_score"] == expected_total

    def test_no_cases_pass(self):
        result = self.scorer.score({
            "passed_cases": 0,
            "total_cases": 3,
        })
        assert result["correctness_score"] == 0.0
        expected_total = round(0.0 * 0.6 + 80.0 * 0.2 + 80.0 * 0.2, 2)
        assert result["total_score"] == expected_total

    def test_zero_total_cases_handled_gracefully(self):
        result = self.scorer.score({
            "passed_cases": 0,
            "total_cases": 0,
        })
        assert result["fallback"] is True
        assert isinstance(result["total_score"], float)

    def test_missing_keys_defaulted(self):
        result = self.scorer.score({})
        assert result["correctness_score"] == 0.0
        assert result["fallback"] is True

    def test_compile_failure_lower_scores(self):
        result = self.scorer.score({
            "passed_cases": 0,
            "total_cases": 3,
            "compile_success": False,
        })
        assert result["correctness_score"] == 0.0
        assert result["style_score"] == 20.0
        assert result["efficiency_score"] == 20.0
        expected_total = round(0.0 * 0.6 + 20.0 * 0.2 + 20.0 * 0.2, 2)
        assert result["total_score"] == expected_total

    def test_compile_success_normal_scores(self):
        result = self.scorer.score({
            "passed_cases": 3,
            "total_cases": 3,
            "compile_success": True,
        })
        assert result["correctness_score"] == 100.0
        assert result["style_score"] == 80.0
        assert result["efficiency_score"] == 80.0

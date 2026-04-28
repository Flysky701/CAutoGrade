class RuleScorer:
    def score(self, static_result: dict) -> dict:
        passed = static_result.get("passed_cases", 0)
        total = max(static_result.get("total_cases", 1), 1)
        correctness = round(100 * passed / total, 2)
        return {
            "correctness_score": correctness,
            "style_score": 80.0,
            "efficiency_score": 80.0,
            "total_score": round(correctness * 0.6 + 80.0 * 0.2 + 80.0 * 0.2, 2),
            "fallback": True,
        }

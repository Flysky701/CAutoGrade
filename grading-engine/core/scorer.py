class RuleScorer:
    def score(self, static_result: dict) -> dict:
        passed = static_result.get("passed_cases", 0)
        total = max(static_result.get("total_cases", 1), 1)
        compile_success = static_result.get("compile_success", True)
        correctness = round(100 * passed / total, 2)
        if not compile_success:
            style = 20.0
            efficiency = 20.0
        else:
            style = 80.0
            efficiency = 80.0
        return {
            "correctness_score": correctness,
            "style_score": style,
            "efficiency_score": efficiency,
            "total_score": round(correctness * 0.6 + style * 0.2 + efficiency * 0.2, 2),
            "fallback": True,
        }

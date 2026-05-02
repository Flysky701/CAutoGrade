"""Standardize grading pipeline output for consumption by the backend API."""


def format_grading_result(dispatcher_output: dict) -> dict:
    """
    Transform the dispatcher's raw output into the format expected
    by the backend's /api/submissions/review endpoint.

    Correctness score is always derived from test-case pass rate when
    test cases exist, because it is an objective metric.  The LLM's
    correctness_score is only used as a fallback when there are zero
    test cases.
    """
    static = dispatcher_output.get("static_result", {})
    llm = dispatcher_output.get("llm_feedback", {})
    fallback = dispatcher_output.get("fallback_score", {})

    total_cases = static.get("total_cases", 0)
    passed_cases = static.get("passed_cases", 0)

    if total_cases > 0:
        correctness = round(100 * passed_cases / total_cases, 2)
    elif llm and llm.get("correctness_score") is not None:
        correctness = llm["correctness_score"]
    else:
        correctness = fallback.get("correctness_score", 0)

    if llm and llm.get("total_score") is not None:
        style = llm.get("style_score", 0)
        efficiency = llm.get("efficiency_score", 0)
        summary = llm.get("summary", "")
        annotations = llm.get("line_annotations", [])
        improvements = llm.get("improvements", [])
        used_fallback = False
    else:
        style = fallback.get("style_score", 0)
        efficiency = fallback.get("efficiency_score", 0)
        summary = "LLM evaluation unavailable; using rule-based scoring."
        annotations = []
        improvements = []
        used_fallback = True

    total = round(correctness * 0.6 + style * 0.2 + efficiency * 0.2, 2)

    return {
        "submission_id": dispatcher_output["submission_id"],
        "status": dispatcher_output["status"],
        "total_score": total,
        "correctness_score": correctness,
        "style_score": style,
        "efficiency_score": efficiency,
        "summary": summary,
        "line_annotations": annotations,
        "improvements": improvements,
        "test_case_results": [
            {
                "caseId": tc.get("id"),
                "passed": tc.get("passed"),
                "input": tc.get("input"),
                "expected": tc.get("expected"),
                "actual": tc.get("actual"),
                "weight": tc.get("weight"),
            }
            for tc in static.get("test_cases", [])
        ],
        "compile_success": static.get("compile_success", False),
        "compile_error": static.get("compile_error", ""),
        "warnings": static.get("warnings", []),
        "used_fallback": used_fallback,
    }

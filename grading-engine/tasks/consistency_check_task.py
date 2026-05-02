"""Consistency check task: validates LLM grading against golden-standard references.

Periodically compares LLM scores with human-annotated golden scores to detect
model drift. If the deviation exceeds the threshold, fallback to rule-based scoring.
"""
import json
import logging
import os

from celery_app import app

logger = logging.getLogger(__name__)

DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "tests", "golden_standard")


def _load_golden_set() -> list[dict]:
    """Load the golden-standard test dataset (human-annotated submissions)."""
    if not os.path.isdir(DATA_DIR):
        logger.warning("Golden standard data directory not found: %s", DATA_DIR)
        return []
    samples = []
    for fname in sorted(os.listdir(DATA_DIR)):
        if fname.endswith(".json"):
            path = os.path.join(DATA_DIR, fname)
            try:
                with open(path, "r", encoding="utf-8") as f:
                    sample = json.load(f)
                    if sample.get("code") and sample.get("golden_score") is not None:
                        samples.append(sample)
            except (json.JSONDecodeError, KeyError) as e:
                logger.warning("Skipping invalid golden sample %s: %s", fname, e)
    return samples


def _compute_deviation(llm_result: dict, golden: dict) -> dict:
    """Compute the deviation between LLM score and golden standard."""
    llm_total = llm_result.get("total_score")
    golden_total = golden.get("golden_score")

    if llm_total is None or golden_total is None:
        return {"diff": None, "need_fallback": False, "reason": "missing_score"}

    diff = abs(float(llm_total) - float(golden_total))
    threshold = float(golden.get("threshold", 15.0))
    exceeded = diff > threshold

    dimension_diffs = {}
    for dim in ["correctness_score", "style_score", "efficiency_score"]:
        llm_val = llm_result.get(dim)
        golden_val = golden.get(f"golden_{dim}")
        if llm_val is not None and golden_val is not None:
            dimension_diffs[dim] = abs(float(llm_val) - float(golden_val))

    return {
        "submission_id": golden.get("submission_id"),
        "llm_score": llm_total,
        "golden_score": golden_total,
        "diff": round(diff, 2),
        "threshold": threshold,
        "need_fallback": exceeded,
        "dimension_diffs": dimension_diffs,
    }


@app.task(name="tasks.consistency_check_task.check_consistency")
def check_consistency(submission_id: int, llm_score: float, golden_score: float,
                      threshold: float = 10.0):
    """Compare a single LLM score against a golden standard.

    Returns whether fallback (rule-based scoring) is needed.
    """
    diff = abs(llm_score - golden_score)
    fallback = diff > threshold
    return {
        "submission_id": submission_id,
        "llm_score": llm_score,
        "golden_score": golden_score,
        "diff": round(diff, 2),
        "threshold": threshold,
        "need_fallback": fallback,
        "message": f"Deviation {diff:.1f} exceeds threshold {threshold}" if fallback
                    else f"Deviation {diff:.1f} within threshold {threshold}",
    }


@app.task(name="tasks.consistency_check_task.run_batch_check")
def run_batch_check(llm_grading_results: list[dict]) -> dict:
    """Run consistency checks against all golden-standard samples.

    Accepts a list of LLM grading results and compares each against
    its corresponding golden standard from the test dataset.

    Returns aggregate statistics about the deviation.
    """
    golden_samples = _load_golden_set()
    if not golden_samples:
        logger.warning("No golden standard samples available for consistency check")
        return {"status": "skipped", "reason": "no_golden_data"}

    golden_by_id = {s.get("submission_id"): s for s in golden_samples}
    results = []
    exceeded_count = 0

    for llm_result in llm_grading_results:
        sid = llm_result.get("submission_id")
        golden = golden_by_id.get(sid)
        if golden:
            dev = _compute_deviation(llm_result, golden)
            results.append(dev)
            if dev["need_fallback"]:
                exceeded_count += 1

    if not results:
        return {"status": "done", "checked": 0, "message": "No matching golden samples found"}

    avg_diff = sum(r.get("diff", 0) or 0 for r in results) / len(results)
    return {
        "status": "done",
        "total_checked": len(results),
        "exceeded_threshold": exceeded_count,
        "average_deviation": round(avg_diff, 2),
        "needs_review": exceeded_count > len(results) * 0.3,  # >30% exceeded → review
        "details": results,
    }

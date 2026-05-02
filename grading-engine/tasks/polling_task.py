import os
import json
import logging
import yaml

from db import MySQLClient
from core.dispatcher import Dispatcher
from core.llm_service import LLMService
from utils.result_formatter import format_grading_result

logger = logging.getLogger(__name__)


def _load_config():
    config_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "config.yaml"
    )
    if not os.path.exists(config_path):
        return {}
    with open(config_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def _build_dispatcher():
    config = _load_config()
    llm_cfg = config.get("llm", {})
    api_key = os.getenv("DEEPSEEK_API_KEY", "") or llm_cfg.get("api_key", "")
    llm_service = LLMService(
        api_key=api_key,
        base_url=llm_cfg.get("base_url", "https://api.deepseek.com/v1"),
        model=llm_cfg.get("model", "deepseek-chat"),
        timeout=llm_cfg.get("timeout", 30),
        max_tokens=llm_cfg.get("max_tokens", 128000),
    )
    return Dispatcher(llm_service=llm_service)


def _send_notification(submission_id, student_id, score):
    try:
        from tasks.notification_task import notify_grading_done
        notify_grading_done.delay(submission_id, student_id, score)
    except Exception as e:
        logger.warning(f"Failed to queue notification for submission {submission_id}: {e}")


def poll_and_grade():
    db = MySQLClient()
    db.reset_stuck_processing()
    pending = db.fetch_pending_grading_results(limit=10)

    if not pending:
        return 0

    logger.info(f"Found {len(pending)} pending grading task(s)")

    try:
        dispatcher = _build_dispatcher()
    except Exception as e:
        logger.error(f"Failed to build dispatcher: {e}")
        return 0

    for row in pending:
        submission_id = row["submission_id"]
        student_id = row.get("student_id")

        if not db.mark_processing(submission_id):
            logger.info(f"Submission {submission_id} already being processed, skipping")
            continue

        try:
            problem = db.fetch_problem(row["problem_id"])
            test_cases = db.fetch_test_cases(row["problem_id"])
            knowledge_tags = _parse_tags(problem.get("knowledge_tags", ""))

            tc_list = [
                {
                    "id": tc["id"],
                    "input_data": tc["input_data"],
                    "expected_output": tc["expected_output"],
                    "weight": tc.get("weight", 10),
                    "is_hidden": bool(tc.get("is_hidden", 0)),
                }
                for tc in test_cases
            ]

            raw_result = dispatcher.grade(
                submission_id=submission_id,
                code_content=row["code_content"],
                problem_id=row["problem_id"],
                test_cases=tc_list,
                problem_description=problem.get("description", ""),
                knowledge_tags=knowledge_tags,
                language=row.get("language", "c"),
            )

            formatted = format_grading_result(raw_result)
            db.update_grading_result(submission_id, formatted)

            score = formatted.get("total_score")
            _send_notification(submission_id, student_id, score)

        except Exception as e:
            logger.error(f"Grading failed for submission {submission_id}: {e}", exc_info=True)
            db.mark_failed(submission_id, str(e))

    return len(pending)


def _parse_tags(tags_str):
    if not tags_str:
        return []
    try:
        return json.loads(tags_str)
    except (json.JSONDecodeError, TypeError):
        return [t.strip() for t in tags_str.split(",") if t.strip()]

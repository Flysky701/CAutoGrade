"""Notification callback task: notifies the backend API when grading completes."""
import requests
import logging
import os
import yaml

from celery_app import app

logger = logging.getLogger(__name__)

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api")


def _get_backend_url():
    try:
        config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "config.yaml")
        with open(config_path, "r", encoding="utf-8") as f:
            config = yaml.safe_load(f) or {}
        return config.get("backend", {}).get("url", BACKEND_URL)
    except Exception:
        return BACKEND_URL


@app.task(name="tasks.notification_task.notify_grading_done")
def notify_grading_done(submission_id: int, student_id: int, score: float = None,
                        problem_title: str = ""):
    """Call the backend API to push a notification when grading is complete."""
    backend_url = _get_backend_url()
    url = f"{backend_url}/notifications"
    payload = {
        "userId": student_id,
        "title": "批阅完成",
        "content": f"你的提交 #{submission_id} 已批阅完成" +
                   (f"，得分 {score} 分" if score is not None else "") +
                   (f"（{problem_title}）" if problem_title else ""),
        "type": "GRADING",
        "relatedId": submission_id,
    }
    try:
        resp = requests.post(url, json=payload, timeout=10)
        resp.raise_for_status()
        logger.info(f"Notification sent for submission {submission_id}")
        return {"status": "sent", "submission_id": submission_id}
    except requests.RequestException as e:
        logger.error(f"Failed to send notification for submission {submission_id}: {e}")
        return {"status": "failed", "submission_id": submission_id, "error": str(e)}


@app.task(name="tasks.notification_task.notify_batch_done")
def notify_batch_done(assignment_id: int, results: list[dict]):
    """Send batch notifications when grading of an entire assignment completes."""
    backend_url = _get_backend_url()
    sent = 0
    failed = 0
    for r in results:
        url = f"{backend_url}/notifications"
        payload = {
            "userId": r.get("student_id"),
            "title": "批阅完成",
            "content": f"作业「{r.get('assignment_title', '')}」的批阅结果已公布，"
                       f"你的得分：{r.get('score', '—')} 分",
            "type": "GRADING",
            "relatedId": assignment_id,
        }
        try:
            resp = requests.post(url, json=payload, timeout=10)
            resp.raise_for_status()
            sent += 1
        except requests.RequestException:
            failed += 1
    logger.info(f"Batch notifications for assignment {assignment_id}: {sent} sent, {failed} failed")
    return {"status": "done", "sent": sent, "failed": failed}

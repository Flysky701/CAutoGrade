from celery_app import app
import logging
import os
import yaml

from core.dispatcher import Dispatcher
from core.llm_service import LLMService

logger = logging.getLogger(__name__)


def _load_config():
    config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "config.yaml")
    if not os.path.exists(config_path):
        return {}
    with open(config_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


@app.task(bind=True, max_retries=3, default_retry_delay=5)
def grade_code(self, submission_id, code_content, problem_id, test_cases=None,
               problem_description="", knowledge_tags=None):
    logger.info(f"Grading task started — submission {submission_id}")

    try:
        config = _load_config()
        llm_cfg = config.get("llm", {})
        api_key = os.getenv("DEEPSEEK_API_KEY", "")
        llm_service = LLMService(
            api_key=api_key,
            base_url=llm_cfg.get("base_url", "https://api.deepseek.com/v1"),
            model=llm_cfg.get("model", "deepseek-chat"),
            timeout=llm_cfg.get("timeout", 30),
        )
        dispatcher = Dispatcher(llm_service=llm_service)
        result = dispatcher.grade(
            submission_id=submission_id,
            code_content=code_content,
            problem_id=problem_id,
            test_cases=test_cases or [],
            problem_description=problem_description,
            knowledge_tags=knowledge_tags or [],
        )
        logger.info(f"Grading complete — submission {submission_id}")
        return result
    except Exception as e:
        logger.error(f"Grading failed for submission {submission_id}: {str(e)}")
        self.retry(exc=e)

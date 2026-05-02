"""Entry point for the grading poller. Intended to run inside the Docker container."""

import time
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    stream=sys.stdout,
)
logger = logging.getLogger("grading_poller")

# Give Celery worker a moment to start
time.sleep(2)

logger.info("Grading poller started — polling every 5 seconds")

while True:
    try:
        from tasks.polling_task import poll_and_grade

        count = poll_and_grade()
        if count > 0:
            logger.info(f"Processed {count} submission(s)")
    except Exception as e:
        logger.error(f"Poll cycle failed: {e}", exc_info=True)

    time.sleep(5)

"""Retry helpers for external API calls."""

import time
import logging

logger = logging.getLogger(__name__)


def retry_with_backoff(func, max_retries=3, base_delay=1.0, backoff_factor=2.0):
    """Call func() with exponential backoff on failure."""
    for attempt in range(max_retries):
        try:
            return func()
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            delay = base_delay * (backoff_factor ** attempt)
            logger.warning(f"Attempt {attempt + 1} failed: {e}. Retrying in {delay:.1f}s...")
            time.sleep(delay)

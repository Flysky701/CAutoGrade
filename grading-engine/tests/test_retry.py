"""Tests for retry utility."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import pytest
from utils.retry import retry_with_backoff


class TestRetryWithBackoff:
    def test_success_on_first_attempt(self):
        call_count = [0]

        def work():
            call_count[0] += 1
            return "ok"

        result = retry_with_backoff(work, max_retries=3, base_delay=0.01)
        assert result == "ok"
        assert call_count[0] == 1

    def test_success_after_retry(self):
        call_count = [0]

        def work():
            call_count[0] += 1
            if call_count[0] < 2:
                raise ValueError("temporary error")
            return "recovered"

        result = retry_with_backoff(work, max_retries=3, base_delay=0.01)
        assert result == "recovered"
        assert call_count[0] == 2

    def test_exhausts_retries(self):
        call_count = [0]

        def work():
            call_count[0] += 1
            raise RuntimeError("always fails")

        with pytest.raises(RuntimeError):
            retry_with_backoff(work, max_retries=3, base_delay=0.01)
        assert call_count[0] == 3

    def test_single_attempt_mode(self):
        call_count = [0]

        def work():
            call_count[0] += 1
            raise Exception("fail")

        with pytest.raises(Exception):
            retry_with_backoff(work, max_retries=1, base_delay=0.01)
        assert call_count[0] == 1

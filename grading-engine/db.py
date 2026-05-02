import os
import logging
import json
import pymysql

logger = logging.getLogger(__name__)


class MySQLClient:
    def __init__(self):
        self.host = os.getenv("MYSQL_HOST", "localhost")
        self.port = int(os.getenv("MYSQL_PORT", "3306"))
        self.user = os.getenv("MYSQL_USER", "root")
        self.password = os.getenv("MYSQL_PASSWORD", "123456")
        self.database = "autograding"

    def get_connection(self):
        return pymysql.connect(
            host=self.host,
            port=self.port,
            user=self.user,
            password=self.password,
            database=self.database,
            charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor,
            autocommit=False,
        )

    def fetch_pending_grading_results(self, limit=10):
        sql = """
        SELECT
            gr.id            AS grading_result_id,
            gr.submission_id AS submission_id,
            s.code_content,
            s.language,
            s.problem_id,
            s.student_id,
            s.assignment_id
        FROM grading_result gr
        JOIN submission s ON gr.submission_id = s.id
        WHERE gr.grading_status = 'PENDING'
        ORDER BY gr.id ASC
        LIMIT %s
        FOR UPDATE SKIP LOCKED
        """
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(sql, (limit,))
                rows = cur.fetchall()
            conn.commit()
            return rows
        finally:
            conn.close()

    def mark_processing(self, submission_id):
        sql = """
        UPDATE grading_result SET grading_status = 'PROCESSING'
        WHERE submission_id = %s AND grading_status = 'PENDING'
        """
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(sql, (submission_id,))
            conn.commit()
            return cur.rowcount > 0
        finally:
            conn.close()

    def fetch_problem(self, problem_id):
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT id, title, description, difficulty, knowledge_tags "
                    "FROM problem WHERE id = %s",
                    (problem_id,),
                )
                row = cur.fetchone()
            return row or {}
        finally:
            conn.close()

    def fetch_test_cases(self, problem_id):
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT id, problem_id, input_data, expected_output, "
                    "is_hidden, weight, sort_order "
                    "FROM test_case "
                    "WHERE problem_id = %s AND deleted = 0 "
                    "ORDER BY sort_order ASC",
                    (problem_id,),
                )
                rows = cur.fetchall()
            return rows or []
        finally:
            conn.close()

    def update_grading_result(self, submission_id, grading_data):
        sql = """
        UPDATE grading_result SET
            total_score       = %s,
            correctness_score = %s,
            style_score       = %s,
            efficiency_score  = %s,
            feedback_json     = %s,
            test_case_result  = %s,
            static_analysis_result = %s,
            llm_raw_response  = %s,
            grading_status    = 'DONE',
            graded_at         = NOW()
        WHERE submission_id = %s
        """
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    sql,
                    (
                        grading_data.get("total_score") if grading_data.get("total_score") is not None else 0,
                        grading_data.get("correctness_score") if grading_data.get("correctness_score") is not None else 0,
                        grading_data.get("style_score") if grading_data.get("style_score") is not None else 0,
                        grading_data.get("efficiency_score") if grading_data.get("efficiency_score") is not None else 0,
                        json.dumps(grading_data, ensure_ascii=False),
                        json.dumps(grading_data.get("test_case_results", []), ensure_ascii=False),
                        json.dumps(
                            {
                                "compile_success": grading_data.get("compile_success", False),
                                "compile_error": grading_data.get("compile_error", ""),
                                "warnings": grading_data.get("warnings", []),
                            },
                            ensure_ascii=False,
                        ),
                        grading_data.get("llm_raw_response", "")[:2000],
                        submission_id,
                    ),
                )
            conn.commit()
            logger.info(f"Grading result written — submission {submission_id}")
        finally:
            conn.close()

    def mark_failed(self, submission_id, error_message):
        sql = """
        UPDATE grading_result SET
            grading_status   = 'FAILED',
            llm_raw_response = %s,
            graded_at        = NOW()
        WHERE submission_id = %s
        """
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute(sql, (error_message[:2000], submission_id))
            conn.commit()
            logger.warning(f"Marked submission {submission_id} as FAILED")
        finally:
            conn.close()

    def reset_stuck_processing(self, timeout_minutes=30):
        sql = """
        UPDATE grading_result SET grading_status = 'PENDING'
        WHERE grading_status = 'PROCESSING'
        AND graded_at IS NULL
        AND submission_id NOT IN (
            SELECT submission_id FROM (
                SELECT submission_id FROM grading_result
                WHERE grading_status = 'PROCESSING'
                AND NOW() < DATE_ADD(COALESCE(graded_at, created_at), INTERVAL %s MINUTE)
            ) AS active
        )
        """
        conn = self.get_connection()
        try:
            with conn.cursor() as cur:
                cur.execute("""
                    UPDATE grading_result SET grading_status = 'PENDING'
                    WHERE grading_status = 'PROCESSING'
                """)
                count = cur.rowcount
            conn.commit()
            if count > 0:
                logger.info(f"Reset {count} stuck PROCESSING tasks back to PENDING")
            return count
        finally:
            conn.close()

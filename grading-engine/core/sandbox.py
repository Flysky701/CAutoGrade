import os
import tempfile
import shutil
import logging

import docker
import yaml

logger = logging.getLogger(__name__)

_client = None


def _get_docker_client():
    global _client
    if _client is None:
        _client = docker.from_env()
    return _client


def _load_config():
    config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "config.yaml")
    if os.path.exists(config_path):
        with open(config_path, "r", encoding="utf-8") as f:
            return yaml.safe_load(f) or {}
    return {}


class SandboxRunner:
    SANDBOX_IMAGE = "gcc:latest"

    def __init__(self):
        config = _load_config()
        runtime = config.get("runtime", {})
        self.compile_timeout = runtime.get("compile_timeout_seconds", 5)
        self.run_timeout = runtime.get("run_timeout_seconds", 5)
        self.memory_limit_mb = runtime.get("memory_limit_mb", 256)

    def compile(self, code_content: str) -> dict:
        tmpdir = tempfile.mkdtemp(prefix="sandbox_")
        src_path = os.path.join(tmpdir, "main.c")
        error_path = os.path.join(tmpdir, "compile_error.txt")

        with open(src_path, "w", encoding="utf-8") as f:
            f.write(code_content)

        compile_script = (
            "gcc -Wall -O2 -o /code/a.out /code/main.c "
            "2>/code/compile_error.txt && echo 'OK' || echo 'FAIL'"
        )

        try:
            client = _get_docker_client()
            container = client.containers.run(
                image=self.SANDBOX_IMAGE,
                command=["sh", "-c", compile_script],
                volumes={tmpdir: {"bind": "/code", "mode": "rw"}},
                working_dir="/code",
                network_mode="none",
                mem_limit=f"{self.memory_limit_mb}m",
                nano_cpus=int(1e9),
                detach=True,
            )
            try:
                result = container.wait(timeout=self.compile_timeout + 5)
                exit_code = result.get("StatusCode", -1)
            except Exception:
                container.kill()
                exit_code = -1

            stdout = container.logs().decode("utf-8", errors="replace").strip()

            compile_error = ""
            if os.path.exists(error_path):
                with open(error_path, "r", encoding="utf-8", errors="replace") as f:
                    compile_error = f.read().strip()

            success = exit_code == 0 and "OK" in stdout
            return {
                "success": success,
                "error": compile_error if not success else "",
                "tmpdir": tmpdir,
            }
        except Exception as e:
            logger.error(f"Sandbox compile error: {e}")
            return {
                "success": False,
                "error": f"Sandbox error: {str(e)}",
                "tmpdir": tmpdir,
            }
        finally:
            try:
                container.remove(force=True)
            except Exception:
                pass

    def run_test(self, tmpdir: str, stdin_input: str = "") -> dict:
        stdin_path = os.path.join(tmpdir, "stdin.txt")

        with open(stdin_path, "w", encoding="utf-8") as f:
            f.write(stdin_input)

        run_script = (
            f"ulimit -t {self.run_timeout} && "
            f"ulimit -v {self.memory_limit_mb * 1024} && "
            f"/usr/bin/time -f '%e %M' -o /code/time.txt "
            f"/code/a.out </code/stdin.txt"
        )

        try:
            client = _get_docker_client()
            container = client.containers.run(
                image=self.SANDBOX_IMAGE,
                command=["sh", "-c", run_script],
                volumes={tmpdir: {"bind": "/code", "mode": "rw"}},
                working_dir="/code",
                network_mode="none",
                mem_limit=f"{self.memory_limit_mb}m",
                nano_cpus=int(1e9),
                detach=True,
            )
            try:
                result = container.wait(timeout=self.run_timeout + 10)
                exit_code = result.get("StatusCode", -1)
            except Exception:
                container.kill()
                exit_code = -1

            stdout_path = os.path.join(tmpdir, "stdout.txt")
            stderr_path = os.path.join(tmpdir, "stderr.txt")
            stdout = ""
            stderr = ""
            if os.path.exists(stdout_path):
                with open(stdout_path, "r", encoding="utf-8", errors="replace") as f:
                    stdout = f.read()
            if os.path.exists(stderr_path):
                with open(stderr_path, "r", encoding="utf-8", errors="replace") as f:
                    stderr = f.read()

            time_ms = 0
            memory_kb = 0
            time_path = os.path.join(tmpdir, "time.txt")
            if os.path.exists(time_path):
                with open(time_path, "r", encoding="utf-8", errors="replace") as f:
                    parts = f.read().strip().split()
                    if len(parts) >= 2:
                        time_ms = int(float(parts[0]) * 1000)
                        memory_kb = int(parts[1])

            return {
                "exit_code": exit_code,
                "stdout": stdout.strip(),
                "stderr": stderr.strip(),
                "time_ms": time_ms,
                "memory_kb": memory_kb,
            }
        except Exception as e:
            logger.error(f"Sandbox run error: {e}")
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": f"Sandbox error: {str(e)}",
                "time_ms": 0,
                "memory_kb": 0,
            }
        finally:
            try:
                container.remove(force=True)
            except Exception:
                pass

    def cleanup(self, tmpdir: str):
        if tmpdir and os.path.exists(tmpdir):
            shutil.rmtree(tmpdir, ignore_errors=True)

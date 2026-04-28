import os
import tempfile
import subprocess
import yaml
import logging

logger = logging.getLogger(__name__)


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
        """Compile C code in a Docker container, return binary path or errors."""
        tmpdir = tempfile.mkdtemp(prefix="sandbox_")
        src_path = os.path.join(tmpdir, "main.c")
        out_path = os.path.join(tmpdir, "a.out")
        error_path = os.path.join(tmpdir, "compile_error.txt")

        with open(src_path, "w", encoding="utf-8") as f:
            f.write(code_content)

        cmd = [
            "docker", "run", "--rm",
            "--network", "none",
            f"--memory={self.memory_limit_mb}m",
            f"--cpus=1",
            "-v", f"{tmpdir}:/code:rw",
            "-w", "/code",
            self.SANDBOX_IMAGE,
            "sh", "-c",
            f"gcc -Wall -O2 -o /code/a.out /code/main.c 2>/code/compile_error.txt && echo 'OK' || echo 'FAIL'"
        ]

        try:
            result = subprocess.run(cmd, capture_output=True, text=True,
                                    timeout=self.compile_timeout + 5)
            stdout = result.stdout.strip()

            compile_error = ""
            if os.path.exists(error_path):
                with open(error_path, "r", encoding="utf-8", errors="replace") as f:
                    compile_error = f.read().strip()

            success = "OK" in stdout and "FAIL" not in stdout
            return {
                "success": success,
                "error": compile_error if not success else "",
                "tmpdir": tmpdir,
                "binary": out_path if success else None,
            }
        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error": "Compilation timed out",
                "tmpdir": tmpdir,
                "binary": None,
            }

    def run(self, code_content: str, stdin_input: str = "") -> dict:
        """Compile and run C code, returning execution results."""
        compile_result = self.compile(code_content)

        if not compile_result["success"]:
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": compile_result["error"],
                "time_ms": 0,
                "memory_kb": 0,
                "compile_success": False,
                "compile_error": compile_result["error"],
            }

        tmpdir = compile_result["tmpdir"]
        stdin_path = os.path.join(tmpdir, "stdin.txt")
        stdout_path = os.path.join(tmpdir, "stdout.txt")
        stderr_path = os.path.join(tmpdir, "stderr.txt")

        with open(stdin_path, "w", encoding="utf-8") as f:
            f.write(stdin_input)

        run_script = (
            f"ulimit -t {self.run_timeout} && "
            f"ulimit -v {self.memory_limit_mb * 1024} && "
            f"/usr/bin/time -f '%e %M' -o /code/time.txt "
            f"/code/a.out </code/stdin.txt >/code/stdout.txt 2>/code/stderr.txt"
        )

        cmd = [
            "docker", "run", "--rm",
            "--network", "none",
            f"--memory={self.memory_limit_mb}m",
            f"--cpus=1",
            "-v", f"{tmpdir}:/code:rw",
            "-w", "/code",
            self.SANDBOX_IMAGE,
            "sh", "-c", run_script
        ]

        try:
            result = subprocess.run(cmd, capture_output=True, text=True,
                                    timeout=self.run_timeout + 10)

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
                "exit_code": result.returncode,
                "stdout": stdout.strip(),
                "stderr": stderr.strip(),
                "time_ms": time_ms,
                "memory_kb": memory_kb,
                "compile_success": True,
                "compile_error": "",
            }
        except subprocess.TimeoutExpired:
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": "Execution timed out",
                "time_ms": self.run_timeout * 1000,
                "memory_kb": 0,
                "compile_success": True,
                "compile_error": "",
            }
        finally:
            import shutil
            shutil.rmtree(tmpdir, ignore_errors=True)

import os
import io
import tarfile
import time
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


def _ensure_image(client, image_name):
    try:
        client.images.get(image_name)
        return True
    except docker.errors.ImageNotFound:
        pass

    config = _load_config()
    mirror = config.get("docker", {}).get("mirror_url", "")
    if mirror:
        mirror_image = f"{mirror}/{image_name}"
        logger.info(f"Image {image_name} not found locally, trying mirror: {mirror_image}")
        try:
            client.images.pull(mirror_image)
            img = client.images.get(mirror_image)
            img.tag(image_name)
            logger.info(f"Successfully pulled and tagged {image_name} from mirror")
            return True
        except Exception as e:
            logger.warning(f"Mirror pull failed for {mirror_image}: {e}")

    try:
        logger.info(f"Pulling {image_name} from default registry...")
        client.images.pull(image_name)
        return True
    except Exception as e:
        logger.error(f"Failed to pull image {image_name}: {e}")
        return False


def _make_tar_bytes(files: dict) -> bytes:
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w") as tar:
        for name, content in files.items():
            if content is None:
                content = ""
            data = content.encode("utf-8") if isinstance(content, str) else content
            info = tarfile.TarInfo(name=name)
            info.size = len(data)
            tar.addfile(info, io.BytesIO(data))
    buf.seek(0)
    return buf.read()


def _extract_file_from_archive(archive_bytes, filename):
    buf = io.BytesIO(archive_bytes)
    with tarfile.open(fileobj=buf, mode="r") as tar:
        for member in tar.getmembers():
            if member.name == filename or member.name.endswith("/" + filename):
                f = tar.extractfile(member)
                if f:
                    return f.read().decode("utf-8", errors="replace")
    return ""


class SandboxRunner:
    SANDBOX_IMAGE = "gcc:latest"

    def __init__(self):
        config = _load_config()
        runtime = config.get("runtime", {})
        self.compile_timeout = runtime.get("compile_timeout_seconds", 5)
        self.run_timeout = runtime.get("run_timeout_seconds", 5)
        self.memory_limit_mb = runtime.get("memory_limit_mb", 256)

    def _create_running_container(self, client):
        container = client.containers.create(
            image=self.SANDBOX_IMAGE,
            command=["sleep", "300"],
            network_mode="none",
            mem_limit=f"{self.memory_limit_mb}m",
        )
        container.start()

        for _ in range(10):
            container.reload()
            if container.status == "running":
                break
            time.sleep(0.5)
        else:
             logger.error(f"Container failed to start, status: {container.status}, logs: {container.logs().decode('utf-8', errors='replace')[:500]}")
             try:
                 container.remove(force=True)
             except Exception:
                 pass
             return None

        return container

    def compile(self, code_content: str, language: str = "c") -> dict:
        ext = "cpp" if language in ("cpp", "c++", "C++") else "c"
        compiler = "g++" if ext == "cpp" else "gcc"
        src_file = f"main.{ext}"
        compile_script = (
            f"{compiler} -Wall -O2 -o /code/a.out /code/{src_file} "
            f"2>/code/compile_error.txt && echo 'OK' || echo 'FAIL'"
        )

        container = None
        try:
            client = _get_docker_client()
            _ensure_image(client, self.SANDBOX_IMAGE)

            container = self._create_running_container(client)
            if container is None:
                return {
                    "success": False,
                    "error": "Failed to start sandbox container",
                    "container_id": None,
                }

            tar_data = _make_tar_bytes({src_file: code_content})
            container.put_archive("/tmp", tar_data)
            container.exec_run(cmd=["sh", "-c", f"mkdir -p /code && cp /tmp/{src_file} /code/{src_file}"])

            exec_result = container.exec_run(
                cmd=["sh", "-c", compile_script],
                workdir="/code",
            )

            exit_code = exec_result.exit_code
            output = exec_result.output or b""
            if isinstance(output, bytes):
                stdout = output.decode("utf-8", errors="replace").strip()
            elif isinstance(output, tuple):
                out_bytes = output[0] or b""
                stdout = out_bytes.decode("utf-8", errors="replace").strip()
            else:
                stdout = str(output).strip()

            compile_error = ""
            try:
                bits, stat = container.get_archive("/code/compile_error.txt")
                archive_data = b"".join(bits)
                compile_error = _extract_file_from_archive(archive_data, "compile_error.txt").strip()
            except Exception:
                pass

            success = exit_code == 0 and "OK" in stdout

            if not success:
                try:
                    container.remove(force=True)
                except Exception:
                    pass
                container = None

            return {
                "success": success,
                "error": compile_error if not success else "",
                "container_id": container.id if container else None,
            }
        except Exception as e:
            logger.error(f"Sandbox compile error: {e}")
            try:
                if container:
                    container.remove(force=True)
            except Exception:
                pass
            return {
                "success": False,
                "error": f"Sandbox error: {str(e)}",
                "container_id": None,
            }

    def run_test(self, container_id: str, stdin_input: str = "") -> dict:
        try:
            client = _get_docker_client()

            try:
                container = client.containers.get(container_id)
            except Exception:
                return {
                    "exit_code": -1,
                    "stdout": "",
                    "stderr": "Container not found",
                    "time_ms": 0,
                    "memory_kb": 0,
                }

            stdin_tar = _make_tar_bytes({"stdin.txt": stdin_input})
            container.put_archive("/tmp", stdin_tar)
            container.exec_run(cmd=["sh", "-c", "cp /tmp/stdin.txt /code/stdin.txt"])

            run_script = (
                f"ulimit -t {self.run_timeout} && "
                f"ulimit -v {self.memory_limit_mb * 1024} && "
                f"/code/a.out </code/stdin.txt > /code/stdout.txt 2>/code/stderr.txt; "
                f"echo $?"
            )

            start = time.time()
            exec_result = container.exec_run(
                cmd=["sh", "-c", run_script],
                workdir="/code",
            )
            elapsed_ms = int((time.time() - start) * 1000)

            exit_code = exec_result.exit_code

            stdout = ""
            try:
                bits, _ = container.get_archive("/code/stdout.txt")
                stdout = _extract_file_from_archive(b"".join(bits), "stdout.txt").strip()
            except Exception:
                output = exec_result.output or b""
                if isinstance(output, bytes):
                    stdout = output.decode("utf-8", errors="replace").strip()

            stderr = ""
            try:
                bits, _ = container.get_archive("/code/stderr.txt")
                stderr = _extract_file_from_archive(b"".join(bits), "stderr.txt").strip()
            except Exception:
                pass

            time_ms = elapsed_ms
            memory_kb = 0

            return {
                "exit_code": exit_code,
                "stdout": stdout,
                "stderr": stderr,
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

    def cleanup(self, container_id):
        if not container_id:
            return
        try:
            client = _get_docker_client()
            container = client.containers.get(container_id)
            container.remove(force=True)
        except Exception:
            pass

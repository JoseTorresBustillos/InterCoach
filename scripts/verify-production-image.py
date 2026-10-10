#!/usr/bin/env python3
"""Smoke-test an image against disposable infrastructure, never a deployed database."""

import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid


def docker(*arguments, timeout=30, check=True):
    return subprocess.run(
        ["docker", *arguments], check=check, capture_output=True, text=True, timeout=timeout
    )


def request(base, path, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode()
    with urllib.request.urlopen(
        urllib.request.Request(base + path, data=data, headers=headers), timeout=60
    ) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image")
    parser.add_argument(
        "--allow-daemon-access", action="store_true",
        help="Allow this trusted image root-equivalent access to the test host Docker daemon."
    )
    arguments = parser.parse_args()
    if not arguments.allow_daemon_access:
        parser.error("Explicit --allow-daemon-access is required; run only on a trusted test host.")

    endpoint = os.environ.get("DOCKER_HOST") or docker(
        "context", "inspect", "--format", "{{.Endpoints.docker.Host}}"
    ).stdout.strip()
    if not endpoint.startswith("unix://"):
        parser.error("This local smoke test requires a Unix socket; production may use authenticated TLS.")
    socket = endpoint.removeprefix("unix://")
    prefix = "intercoach-smoke-" + uuid.uuid4().hex[:12]
    database, app, network = prefix + "-db", prefix + "-app", prefix + "-net"
    password, jwt = secrets.token_urlsafe(32), secrets.token_urlsafe(48)
    resources = []
    network_created = False
    with tempfile.TemporaryDirectory(prefix="intercoach-smoke-") as directory:
        workspace = Path(directory) / "execution"
        workspace.mkdir()
        # Only the disposable workspace is writable by the image's non-root user.
        workspace.chmod(0o1777)
        try:
            docker("network", "create", network)
            network_created = True
            resources.append(database)
            docker("run", "-d", "--name", database, "--network", network,
                   "-e", "POSTGRES_DB=smoke", "-e", "POSTGRES_USER=smoke",
                   "-e", "POSTGRES_PASSWORD=" + password, "pgvector/pgvector:pg16", timeout=120)
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if docker("exec", database, "pg_isready", "-U", "smoke", "-d", "smoke",
                          check=False).returncode == 0:
                    break
                time.sleep(1)
            else:
                raise RuntimeError("Disposable PostgreSQL did not become ready")

            socket_mount = "type=bind,source=" + socket + ",target=/var/run/docker.sock"
            group = docker("run", "--rm", "--mount", socket_mount,
                           "--entrypoint", "sh", arguments.image,
                           "-c", "test $(id -u) -ne 0 && docker --version >&2 && stat -c %g /var/run/docker.sock").stdout.strip()
            environment = {
                "SPRING_PROFILES_ACTIVE": "production",
                "SPRING_DATASOURCE_URL": "jdbc:postgresql://" + database + ":5432/smoke",
                "SPRING_DATASOURCE_USERNAME": "smoke",
                "SPRING_DATASOURCE_PASSWORD": password,
                "JWT_SECRET": jwt,
                "OPENAI_API_KEY": "synthetic-smoke-key-not-a-real-credential",
                "INTERCOACH_EXECUTION_WORKSPACE_ROOT": str(workspace),
                "INTERCOACH_REVIEW_QUEUE_ENABLED": "true",
                "INTERCOACH_EXECUTION_DOCKER_PRE_PULL": "false",
                "INTERCOACH_EXECUTION_COMPILE_TIMEOUT_SECONDS": "20",
                "INTERCOACH_EXECUTION_TEST_TIMEOUT_SECONDS": "10",
            }
            command = ["run", "-d", "--name", app, "--network", network,
                       "--read-only", "--tmpfs", "/tmp:rw,nosuid,size=128m",
                       "--group-add", group, "--mount", socket_mount,
                       "--mount", "type=bind,source=" + str(workspace) + ",target=" + str(workspace),
                       "-p", "127.0.0.1::8080"]
            for key, value in environment.items():
                command.extend(["-e", key + "=" + value])
            resources.append(app)
            docker(*command, arguments.image)
            port = json.loads(docker("inspect", app).stdout)[0]["NetworkSettings"]["Ports"]["8080/tcp"][0]["HostPort"]
            base = "http://127.0.0.1:" + port
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                if docker("inspect", "--format", "{{.State.Running}}", app).stdout.strip() != "true":
                    raise RuntimeError("Production application exited during startup")
                try:
                    request(base, "/api/health")
                    break
                except (urllib.error.URLError, TimeoutError):
                    time.sleep(1)
            else:
                raise RuntimeError("Production application did not become healthy")

            docker("exec", database, "psql", "-v", "ON_ERROR_STOP=1", "-U", "smoke", "-d", "smoke", "-c",
                   "INSERT INTO problem (id,title,description,difficulty) VALUES (90001,'Echo','Echo input','EASY');"
                   "INSERT INTO test_case (problem_id,input,expected_output,hidden) VALUES (90001,'hello','hello',false);")
            auth = request(base, "/api/auth/register", {
                "username": "smokeuser", "email": "smoke@example.test", "password": secrets.token_urlsafe(24)
            })
            token = auth["token"]
            docker("exec", database, "psql", "-v", "ON_ERROR_STOP=1", "-U", "smoke", "-d", "smoke", "-c",
                   "UPDATE app_user SET role='ADMIN' WHERE username='smokeuser';")
            # HTTP health and Boot's 'Started' log can precede ApplicationRunner completion.
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                if request(base, "/api/admin/execution/status", token=token)["preflight"]["hostReady"]:
                    break
                if docker("inspect", "--format", "{{.State.Running}}", app).stdout.strip() != "true":
                    raise RuntimeError("Production preflight failed")
                time.sleep(1)
            else:
                raise RuntimeError("Production preflight did not finish")
            source = "public class Main { public static void main(String[] a) throws Exception { System.out.write(System.in.readAllBytes()); } }"
            result = request(base, "/api/problems/90001/run", {"language": "Java", "submittedCode": source}, token)
            if result["status"] != "SUCCESS" or not result["allPassed"]:
                raise RuntimeError("Production Docker compilation/execution failed: " + json.dumps(result))
            try:
                request(base, "/api/smoke-missing", token=token)
                raise RuntimeError("Missing route unexpectedly succeeded")
            except urllib.error.HTTPError as error:
                if error.code != 404:
                    raise
            if list(workspace.iterdir()):
                raise RuntimeError("Execution left files in its disposable workspace")
            print("PASS: non-root production image, queue wiring, migrations, Docker preflight, Java execution, and 404 handling")
        except Exception:
            if app in resources:
                logs = docker("logs", app, check=False)
                print(logs.stdout + logs.stderr)
            raise
        finally:
            for name in reversed(resources):
                result = docker("rm", "--force", name, check=False)
                if result.returncode:
                    print("Cleanup warning: " + result.stderr)
            if network_created:
                result = docker("network", "rm", network, check=False)
                if result.returncode:
                    print("Cleanup warning: " + result.stderr)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Local runner for the first service in deploy.yml -> local.

This intentionally avoids the server-only deploy.sh steps: git checkout/reset,
nginx host configuration, sudo, Discord notifications and post-deploy hooks.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any


STACK_SPRING_DIR = "spring-3.9-java-17"
STACK_SPRING_MARIA_DIR = "spring-3.9-java-17-maria-11"
STACK_SPRING_POSTGRES_DIR = "spring-3.9-java-17-postgres-18-4"

RESERVED_ENV_KEYS = {
    "stack",
    "port",
    "branch",
    "path",
    "domain",
    "ssl",
    "composer_file",
    "nginx_config",
    "application_properties",
    "script",
    "environment",
    "compose_service",
    "env",
    "mysql_user",
    "mysql_db",
    "mysql_pass",
    "mysql_external_port",
    "postgres_user",
    "postgres_db",
    "postgres_pass",
    "postgres_external_port",
    "CONTAINER_NAME",
    "EXTERNAL_PORT",
    "INTERNAL_PORT",
    "NAME",
    "PORT",
    "MYSQL_USER",
    "MYSQL_DB",
    "MYSQL_PASS",
    "MYSQL_ROOT_PASSWORD",
    "MYSQL_EXTERNAL_PORT",
    "POSTGRES_USER",
    "POSTGRES_DB",
    "POSTGRES_PASS",
    "POSTGRES_EXTERNAL_PORT",
    "SPRING_CONFIG_LOCATION",
    "LOGGING_FILE_PATH",
    "LOGGING_FILE_BASENAME",
    "NG_BUILD_CONFIGURATION",
    "NG_APP_BASE_HREF",
    "VITE_BUILD_MODE",
    "VITE_BASE_PATH",
    "APP_BASE_PATH",
}


def die(message: str) -> None:
    print(f"local.py: {message}", file=sys.stderr)
    raise SystemExit(1)


def strip_yaml_comment(text: str) -> str:
    quote: str | None = None
    escaped = False
    for index, char in enumerate(text):
        if escaped:
            escaped = False
            continue
        if char == "\\":
            escaped = True
            continue
        if char in {"'", '"'}:
            if quote == char:
                quote = None
            elif quote is None:
                quote = char
            continue
        if char == "#" and quote is None:
            return text[:index].rstrip()
    return text.rstrip()


def parse_scalar(value: str) -> Any:
    text = strip_yaml_comment(value).strip()
    if text == "":
        return ""
    if (text.startswith('"') and text.endswith('"')) or (text.startswith("'") and text.endswith("'")):
        return text[1:-1]
    lowered = text.lower()
    if lowered == "true":
        return True
    if lowered == "false":
        return False
    if lowered in {"null", "~"}:
        return None
    if re.match(r"^-?\d+$", text):
        return int(text)
    if re.match(r"^-?\d+\.\d+$", text):
        return float(text)
    return text


def load_simple_deploy_yaml(path: Path) -> dict[str, Any]:
    root: dict[str, Any] = {}
    stack: list[tuple[int, dict[str, Any]]] = [(-1, root)]

    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not raw_line.strip() or raw_line.lstrip().startswith("#"):
            continue
        indent = len(raw_line) - len(raw_line.lstrip(" "))
        content = strip_yaml_comment(raw_line.strip())
        if not content:
            continue
        if ":" not in content:
            die(f"{path}:{line_number}: linea YAML no soportada por el parser local")

        key, raw_value = content.split(":", 1)
        key = key.strip()
        if not key:
            die(f"{path}:{line_number}: clave YAML vacia")

        while indent <= stack[-1][0]:
            stack.pop()
        parent = stack[-1][1]

        if raw_value.strip() == "":
            child: dict[str, Any] = {}
            parent[key] = child
            stack.append((indent, child))
        else:
            parent[key] = parse_scalar(raw_value)

    return root


def load_yaml(path: Path) -> dict[str, Any]:
    try:
        import yaml
    except ImportError:
        return load_simple_deploy_yaml(path)

    with path.open(encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict):
        die(f"{path} no contiene un mapa YAML valido")
    return data


def is_spring_stack(stack: str) -> bool:
    return stack in {"spring", "spring_maria", "spring_postgres"}


def is_nginx_stack(stack: str) -> bool:
    return stack.startswith("nginx-")


def spring_stack_dir(stack: str) -> str:
    if stack == "spring":
        return STACK_SPRING_DIR
    if stack == "spring_maria":
        return STACK_SPRING_MARIA_DIR
    if stack == "spring_postgres":
        return STACK_SPRING_POSTGRES_DIR
    die(f"stack Spring invalido: {stack}")


def scalar_to_dotenv(value: Any) -> str:
    if isinstance(value, bool):
        text = "true" if value else "false"
    elif isinstance(value, (int, float)):
        text = str(value)
    elif value is None:
        text = ""
    elif isinstance(value, str):
        text = value
    else:
        die("env solo acepta valores escalares (string, numero o booleano)")

    if re.search(r"[\s#\"'\\]", text) or text == "":
        return json.dumps(text, ensure_ascii=False)
    return text


def write_runtime_env(deploy_dir: Path, service: dict[str, Any]) -> None:
    env_block = service.get("env") or {}
    if not isinstance(env_block, dict):
        die("env debe ser un mapa clave-valor en deploy.yml")

    stack = str(service.get("stack") or "").strip()
    reserved_upper = {key.upper() for key in RESERVED_ENV_KEYS}
    if stack in ("spring_maria", "spring_postgres", "python_postgres"):
        reserved_upper |= {"SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"}
    lines: list[str] = []
    for raw_key, value in env_block.items():
        key = str(raw_key).strip()
        if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", key):
            die(f"env: clave invalida [{key}] (use letras, numeros y _)")
        if key.upper() in reserved_upper:
            die(f"env: clave reservada [{key}] (infra/deploy); use otro nombre")
        lines.append(f"{key}={scalar_to_dotenv(value)}")

    (deploy_dir / "runtime.env").write_text(
        ("\n".join(lines) + "\n") if lines else "",
        encoding="utf-8",
    )


DEFAULT_DOCKERIGNORE_CONTENT = """.git/
.idea/
.vscode/
node_modules/
.npm/
.pnpm-store/
.yarn/cache/
.angular/cache/
.next/
.nuxt/
.turbo/
coverage/
target/
.m2/
.gradle/
deploy/application.properties
deploy/runtime.env
"""


def ensure_default_dockerignore(repo_root: Path) -> None:
    dockerignore = repo_root / ".dockerignore"
    if dockerignore.is_file():
        return
    dockerignore.write_text(DEFAULT_DOCKERIGNORE_CONTENT, encoding="utf-8")


def prepare_application_properties(repo_root: Path, deploy_dir: Path, service_name: str, service: dict[str, Any]) -> None:
    app_props = str(service.get("application_properties") or "").strip()
    if not app_props:
        die(f"{service_name}: falta application_properties en deploy.yml")

    src = Path(app_props)
    if not src.is_absolute():
        src = repo_root / src
    if not src.is_file():
        die(f"{service_name}: no existe application_properties: {src}")
    shutil.copyfile(src, deploy_dir / "application.properties")


def apply_port_env(env: dict[str, str], service_name: str, port: str, stack: str, path_value: str) -> None:
    env["NAME"] = service_name
    env["PORT"] = port
    env["CONTAINER_NAME"] = service_name
    env["EXTERNAL_PORT"] = port

    if stack in ("angular", "vue") or is_nginx_stack(stack):
        env["INTERNAL_PORT"] = "80"
        if stack in ("angular", "vue"):
            env["NG_APP_BASE_HREF"] = f"/{path_value}/" if path_value else "/"
            env["VITE_BASE_PATH"] = f"/{path_value}/" if path_value else "/"
        else:
            env.pop("VITE_BASE_PATH", None)
            env.pop("NG_APP_BASE_HREF", None)
    elif stack == "angular_ssr":
        env["INTERNAL_PORT"] = "4000"
        env["NG_APP_BASE_HREF"] = f"/{path_value}/" if path_value else "/"
        env.pop("VITE_BASE_PATH", None)
    elif stack == "tanstack":
        env["INTERNAL_PORT"] = "3000"
        env["VITE_BASE_PATH"] = f"/{path_value}/" if path_value else "/"
        env.pop("NG_APP_BASE_HREF", None)
    else:
        env["INTERNAL_PORT"] = port
        env.pop("NG_APP_BASE_HREF", None)
        env.pop("VITE_BASE_PATH", None)


def apply_frontend_env(env: dict[str, str], stack: str, environment_name: str) -> None:
    if stack in {"angular", "angular_ssr"}:
        env["NG_BUILD_CONFIGURATION"] = environment_name or "production"
        env.pop("VITE_BUILD_MODE", None)
    elif stack in ("tanstack", "vue"):
        env["VITE_BUILD_MODE"] = environment_name or "production"
        env.pop("NG_BUILD_CONFIGURATION", None)
    else:
        env.pop("NG_BUILD_CONFIGURATION", None)
        env.pop("VITE_BUILD_MODE", None)


def apply_database_env(env: dict[str, str], service_name: str, service: dict[str, Any], stack: str) -> None:
    if stack == "spring_maria":
        required = ["mysql_user", "mysql_db", "mysql_pass", "mysql_external_port"]
        missing = [key for key in required if not str(service.get(key) or "").strip()]
        if missing:
            die(f"{service_name}: faltan campos en deploy.yml: {', '.join(missing)}")
        mysql_user = str(service["mysql_user"]).strip()
        mysql_db = str(service["mysql_db"]).strip()
        mysql_pass = str(service["mysql_pass"]).strip()
        env["MYSQL_USER"] = mysql_user
        env["MYSQL_DB"] = mysql_db
        env["MYSQL_PASS"] = mysql_pass
        env["MYSQL_ROOT_PASSWORD"] = mysql_pass
        env["MYSQL_EXTERNAL_PORT"] = str(service["mysql_external_port"]).strip()
        env["SPRING_DATASOURCE_URL"] = f"jdbc:mysql://mariadb:3306/{mysql_db}?sessionVariables=sql_mode=''"
        env["SPRING_DATASOURCE_USERNAME"] = mysql_user
        env["SPRING_DATASOURCE_PASSWORD"] = mysql_pass
    elif stack in ("spring_postgres", "python_postgres"):
        required = ["postgres_user", "postgres_db", "postgres_pass", "postgres_external_port"]
        missing = [key for key in required if not str(service.get(key) or "").strip()]
        if missing:
            die(f"{service_name}: faltan campos en deploy.yml: {', '.join(missing)}")
        postgres_user = str(service["postgres_user"]).strip()
        postgres_db = str(service["postgres_db"]).strip()
        postgres_pass = str(service["postgres_pass"]).strip()
        env["POSTGRES_USER"] = postgres_user
        env["POSTGRES_DB"] = postgres_db
        env["POSTGRES_PASS"] = postgres_pass
        env["POSTGRES_EXTERNAL_PORT"] = str(service["postgres_external_port"]).strip()
        if stack == "spring_postgres":
            env["SPRING_DATASOURCE_URL"] = f"jdbc:postgresql://postgres:5432/{postgres_db}"
            env["SPRING_DATASOURCE_USERNAME"] = postgres_user
            env["SPRING_DATASOURCE_PASSWORD"] = postgres_pass


def apply_compose_env(env: dict[str, str], service: dict[str, Any]) -> None:
    env_block = service.get("env") or {}
    if isinstance(env_block, dict) and "RESTART_POLICY" in env_block:
        env["RESTART_POLICY"] = str(env_block["RESTART_POLICY"]).strip()


def apply_local_logging_defaults(env: dict[str, str], stack: str, service: dict[str, Any]) -> None:
    if is_spring_stack(stack):
        env_block = service.get("env") or {}
        if not isinstance(env_block, dict) or "LOGGING_FILE_PATH" not in env_block:
            env["LOGGING_FILE_PATH"] = ""
        if not isinstance(env_block, dict) or "LOGS_VOLUME" not in env_block:
            env["LOGS_VOLUME"] = "./logs"


def select_service(data: dict[str, Any], deploy_env: str, service_name: str | None) -> tuple[str, dict[str, Any]]:
    block = data.get(deploy_env)
    if not isinstance(block, dict):
        die(f"en deploy.yml no existe el entorno [{deploy_env}] o no es un mapa")

    if service_name:
        service = block.get(service_name)
        if not isinstance(service, dict):
            die(f"en deploy.yml no existe el servicio [{service_name}] bajo [{deploy_env}]")
        return service_name, service

    for name, service in block.items():
        if isinstance(service, dict):
            return str(name), service
    die(f"el entorno [{deploy_env}] no tiene servicios")


def main(argv: list[str]) -> int:
    delete_db = os.environ.get("LOCAL_DELETE_DB") == "1" or "--delete-db" in argv[1:]
    args = [arg for arg in argv[1:] if arg != "--delete-db"]
    deploy_env = args[0] if len(args) > 0 else os.environ.get("DEPLOY_ENV", "local")
    service_arg = args[1] if len(args) > 1 else None

    deploy_dir = Path(__file__).resolve().parent
    repo_root = deploy_dir.parent
    deploy_yml = repo_root / "deploy.yml"
    if not deploy_yml.is_file():
        die(f"no existe {deploy_yml}")

    data = load_yaml(deploy_yml)
    service_name, service = select_service(data, deploy_env, service_arg)

    stack = str(service.get("stack") or "spring").strip().lower()
    valid_stacks = {"spring", "spring_maria", "spring_postgres", "angular", "angular_ssr", "tanstack", "python_postgres", "vue"}
    if stack not in valid_stacks and not is_nginx_stack(stack):
        die(f"{service_name}: stack invalido [{stack}]")

    composer = str(service.get("composer_file") or "").strip()
    if is_spring_stack(stack) and not composer:
        composer = f"{spring_stack_dir(stack)}/docker-compose.yml"
    if not composer:
        die(f"{service_name}: falta composer_file en deploy.yml")
    composer_path = deploy_dir / composer
    if not composer_path.is_file():
        die(f"{service_name}: no existe composer_file: {composer_path}")

    port = str(service.get("port") or "").strip()
    if not port:
        die(f"{service_name}: falta port en deploy.yml")

    if is_spring_stack(stack):
        prepare_application_properties(repo_root, deploy_dir, service_name, service)

    ensure_default_dockerignore(repo_root)
    write_runtime_env(deploy_dir, service)

    run_env = os.environ.copy()
    apply_frontend_env(run_env, stack, str(service.get("environment") or "").strip())
    apply_port_env(run_env, service_name, port, stack, str(service.get("path") or "").strip())
    apply_database_env(run_env, service_name, service, stack)
    apply_compose_env(run_env, service)
    apply_local_logging_defaults(run_env, stack, service)

    compose_file_args = ["docker", "compose", "-f", composer]

    if delete_db:
        print(f"Borrando volumenes locales del proyecto Docker Compose [{service_name}]...")
        subprocess.run(
            [*compose_file_args, "down", "--volumes", "--remove-orphans"],
            cwd=deploy_dir,
            env=run_env,
            check=True,
        )

    compose_args = [*compose_file_args, "up", "-d", "--build"]

    print(f"Local deploy: env={deploy_env} servicio={service_name} stack={stack} puerto={port}")
    print("Ejecutando:", " ".join(compose_args))
    subprocess.run(compose_args, cwd=deploy_dir, env=run_env, check=True)

    compose_service = str(service.get("compose_service") or "app").strip()
    subprocess.run(
        ["docker", "compose", "-f", composer, "ps"],
        cwd=deploy_dir,
        env=run_env,
        check=False,
    )
    print("")
    print(f"Listo. URL directa: http://localhost:{port}")
    print(f"Logs: docker logs -f {service_name}")
    print(f"Compose service: {compose_service}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))

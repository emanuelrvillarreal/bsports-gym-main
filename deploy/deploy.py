#!/usr/bin/env python3
#
# Version: 1.9.0
# Ultima modificacion: 2026-06-12
#
# Despliegue segun deploy.yml y entorno definido en /home/.env (variable env=...).
# Por cada servicio: checkout de su branch, luego docker compose + nginx.
# Stacks: spring, spring_maria, spring_postgres (Spring Boot), angular (SPA nginx), angular_ssr (Angular SSR Node), tanstack (TanStack Start SSR Node), nginx-* (estatico).
# Fragmentos nginx: deploy/nginx/ssl-fragments/<env>/*.conf -> /etc/nginx/snippets/ssl-fragments/;
# si el servicio define domain, el fragmento renderizado inicia con "# domain: <dominio>".
# Requiere python3, python3-yaml, docker compose, sudo (nginx y logs host solo spring*).

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import traceback
from pathlib import Path

DISCORD_WEBHOOK = "https://discord.com/api/webhooks/1200063276776693941/6hc1XPVrOcTyCpw0Qo17mf1JOQvXo6Y8OGBH21nqzNeWwr4Evnn2HIKu_G53n_uJiyGs"
DEPLOY_DIR = Path(__file__).resolve().parent
HOME_ENV_FILE = Path("/home/.env")

STACK_SPRING_DIR = "spring-3.9-java-17"
STACK_SPRING_MARIA_DIR = "spring-3.9-java-17-maria-11"
STACK_SPRING_POSTGRES_DIR = "spring-3.9-java-17-postgres-18-4"

REPO_ROOT: str = ""
DEPLOY_ENV: str = ""
CURRENT_SERVICE: str = ""
CURRENT_BRANCH: str = ""


# --- Discord / error helpers ---

def discord_notify(msg: str) -> None:
    try:
        import urllib.request
        payload = json.dumps({"content": msg}).encode("utf-8")
        req = urllib.request.Request(
            DISCORD_WEBHOOK,
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        urllib.request.urlopen(req, timeout=10)
    except Exception:
        print("Aviso: no se pudo enviar notificacion a Discord.", file=sys.stderr)


def die_discord(msg: str) -> None:
    discord_notify(msg)
    print(msg, file=sys.stderr)
    sys.exit(1)


def service_die(name: str, branch: str, reason: str) -> None:
    die_discord(f"{name} Be {branch} {DEPLOY_ENV} - End | {reason}")


def require_cmd(cmd: str) -> None:
    if shutil.which(cmd) is None:
        die_discord(f"deploy.py: falta el comando requerido: {cmd}")


def require_sudo_for_nginx_work(services: list) -> None:
    frag_dir = DEPLOY_DIR / "nginx" / "ssl-fragments" / DEPLOY_ENV
    has_env_fragments = frag_dir.is_dir() and any(frag_dir.glob("*.conf"))
    has_service_nginx = any(is_spring_stack(row["stack"]) or row.get("nginx_config") for row in services)
    if has_service_nginx or has_env_fragments:
        require_cmd("sudo")


# --- Path helpers ---

def resolve_in_deploy_dir(rel: str) -> Path:
    p = Path(rel) if Path(rel).is_absolute() else DEPLOY_DIR / rel
    try:
        return p.resolve()
    except Exception:
        return p


def resolve_in_repo_root(rel: str) -> Path:
    p = Path(rel) if Path(rel).is_absolute() else Path(REPO_ROOT) / rel
    try:
        return p.resolve()
    except Exception:
        return p


# --- Stack registry ---

def is_spring_stack(stack: str) -> bool:
    return stack in ("spring", "spring_maria", "spring_postgres")


def is_nginx_stack(stack: str) -> bool:
    return stack.startswith("nginx-")


def is_valid_stack(stack: str) -> bool:
    return stack in ("spring", "spring_maria", "spring_postgres", "angular", "angular_ssr", "tanstack", "python_postgres", "vue") or is_nginx_stack(stack)


def spring_stack_dir(stack: str) -> str:
    return {"spring": STACK_SPRING_DIR, "spring_maria": STACK_SPRING_MARIA_DIR, "spring_postgres": STACK_SPRING_POSTGRES_DIR}.get(stack, "")


def apply_spring_stack_defaults(stack: str, composer: str, nginx_src: str):
    sd = spring_stack_dir(stack)
    if not sd:
        service_die("", "", f"stack invalido para spring_stack_dir: {stack}")
    if not composer:
        composer = f"{sd}/docker-compose.yml"
    if not nginx_src:
        nginx_src = f"{sd}/nginx.spring-boot-api.conf"
    return composer, nginx_src


# --- Validation ---

def require_non_empty(name: str, branch: str, field_label: str, value: str) -> None:
    if not value:
        service_die(name, branch, f"falta {field_label} en deploy.yml")


def validate_stack_config(name, branch, stack, app_props,
                           mysql_user, mysql_db, mysql_pass, mysql_external_port,
                           postgres_user, postgres_db, postgres_pass, postgres_external_port):
    if not is_valid_stack(stack):
        service_die(name, branch, f"stack invalido [{stack}] (use spring, spring_maria, spring_postgres, angular, angular_ssr, tanstack o nginx-<version>)")
    if is_spring_stack(stack):
        require_non_empty(name, branch, "application_properties", app_props)
    if stack == "spring_maria":
        require_non_empty(name, branch, "mysql_user", mysql_user)
        require_non_empty(name, branch, "mysql_db", mysql_db)
        require_non_empty(name, branch, "mysql_pass", mysql_pass)
        require_non_empty(name, branch, "mysql_external_port", mysql_external_port)
    elif stack in ("spring_postgres", "python_postgres"):
        require_non_empty(name, branch, "postgres_user", postgres_user)
        require_non_empty(name, branch, "postgres_db", postgres_db)
        require_non_empty(name, branch, "postgres_pass", postgres_pass)
        require_non_empty(name, branch, "postgres_external_port", postgres_external_port)


# --- DB env ---

def clear_embedded_db_env() -> None:
    for key in ("MYSQL_USER", "MYSQL_DB", "MYSQL_PASS", "MYSQL_ROOT_PASSWORD", "MYSQL_EXTERNAL_PORT",
                "POSTGRES_USER", "POSTGRES_DB", "POSTGRES_PASS", "POSTGRES_EXTERNAL_PORT",
                "SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"):
        os.environ.pop(key, None)


def configure_embedded_db_compose(stack, composer, mysql_user, mysql_db, mysql_pass, mysql_external_port,
                                   postgres_user, postgres_db, postgres_pass, postgres_external_port):
    clear_embedded_db_env()
    if stack == "spring_maria":
        os.environ["MYSQL_USER"] = mysql_user
        os.environ["MYSQL_DB"] = mysql_db
        os.environ["MYSQL_PASS"] = mysql_pass
        os.environ["MYSQL_ROOT_PASSWORD"] = mysql_pass
        os.environ["MYSQL_EXTERNAL_PORT"] = mysql_external_port
        os.environ["SPRING_DATASOURCE_URL"] = f"jdbc:mysql://mariadb:3306/{mysql_db}?sessionVariables=sql_mode=''"
        os.environ["SPRING_DATASOURCE_USERNAME"] = mysql_user
        os.environ["SPRING_DATASOURCE_PASSWORD"] = mysql_pass
    elif stack in ("spring_postgres", "python_postgres"):
        os.environ["POSTGRES_USER"] = postgres_user
        os.environ["POSTGRES_DB"] = postgres_db
        os.environ["POSTGRES_PASS"] = postgres_pass
        os.environ["POSTGRES_EXTERNAL_PORT"] = postgres_external_port
        if stack == "spring_postgres":
            os.environ["SPRING_DATASOURCE_URL"] = f"jdbc:postgresql://postgres:5432/{postgres_db}"
            os.environ["SPRING_DATASOURCE_USERNAME"] = postgres_user
            os.environ["SPRING_DATASOURCE_PASSWORD"] = postgres_pass
    return ["-f", composer]


# --- Spring files ---

def prepare_spring_application_properties(name: str, branch: str, app_props: str) -> None:
    src = resolve_in_repo_root(app_props)
    if not src.is_file():
        service_die(name, branch, f"no existe application_properties: {src}")
    dest = DEPLOY_DIR / "application.properties"
    try:
        shutil.copy2(str(src), str(dest))
    except Exception:
        service_die(name, branch, f"no se pudo copiar application.properties a {dest}")


def ensure_default_dockerignore(name: str, branch: str) -> None:
    dockerignore = Path(REPO_ROOT) / ".dockerignore"
    if dockerignore.is_file():
        return
    content = (
        ".git/\n.idea/\n.vscode/\nnode_modules/\n.npm/\n.pnpm-store/\n"
        ".yarn/cache/\n.angular/cache/\n.next/\n.nuxt/\n.turbo/\n"
        "coverage/\ntarget/\n.m2/\n.gradle/\n"
        "deploy/application.properties\ndeploy/runtime.env\ndeploy/local-logs/\n"
    )
    tmp = tempfile.NamedTemporaryFile(mode="w", delete=False, encoding="utf-8")
    try:
        tmp.write(content)
        tmp.close()
        shutil.move(tmp.name, str(dockerignore))
    except Exception:
        try:
            os.unlink(tmp.name)
        except Exception:
            pass
        service_die(name, branch, "no se pudo escribir .dockerignore")


# --- runtime.env ---

_RESERVED_ENV_KEYS = {"CONTAINER_NAME", "NAME"}


def _format_dotenv_value(value) -> str:
    if isinstance(value, bool):
        text = "true" if value else "false"
    elif isinstance(value, (int, float)):
        text = str(value)
    elif value is None:
        text = ""
    elif isinstance(value, str):
        text = value
    else:
        raise ValueError("valor debe ser escalar (string, numero o booleano)")
    if re.search(r'[\s#"\'\\]', text) or text == "":
        return json.dumps(text, ensure_ascii=False)
    return text


def write_runtime_env_file(name: str, branch: str, service_row: dict) -> None:
    dest = DEPLOY_DIR / "runtime.env"
    env = service_row.get("env")
    if env is None:
        env = {}
    if not isinstance(env, dict):
        service_die(name, branch, "env debe ser un mapa clave-valor en deploy.yml")
    lines = []
    for key, value in env.items():
        k = str(key).strip()
        if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", k):
            service_die(name, branch, f"env: clave invalida [{k}] (use letras, numeros y _)")
        if k in _RESERVED_ENV_KEYS or k.upper() in {x.upper() for x in _RESERVED_ENV_KEYS}:
            service_die(name, branch, f"env: clave reservada [{k}] (infra/deploy); use otro nombre")
        try:
            lines.append(f"{k}={_format_dotenv_value(value)}")
        except ValueError as exc:
            service_die(name, branch, f"env[{k}]: {exc}")
    with open(str(dest), "w", encoding="utf-8") as f:
        if lines:
            f.write("\n".join(lines) + "\n")


# --- Port/env exports ---

def export_service_port_vars(name: str, port: str, stack: str, deploy_path: str) -> None:
    os.environ["NAME"] = name
    os.environ["PORT"] = port
    os.environ["CONTAINER_NAME"] = name
    os.environ["EXTERNAL_PORT"] = port
    if stack in ("angular", "vue") or is_nginx_stack(stack):
        os.environ["INTERNAL_PORT"] = "80"
        if stack in ("angular", "vue"):
            os.environ["NG_APP_BASE_HREF"] = f"/{deploy_path}/" if deploy_path else "/"
            os.environ["VITE_BASE_PATH"] = f"/{deploy_path}/" if deploy_path else "/"
        else:
            os.environ.pop("NG_APP_BASE_HREF", None)
            os.environ.pop("VITE_BASE_PATH", None)
    elif stack == "angular_ssr":
        os.environ["INTERNAL_PORT"] = "4000"
        os.environ["NG_APP_BASE_HREF"] = f"/{deploy_path}/" if deploy_path else "/"
        os.environ.pop("VITE_BASE_PATH", None)
    elif stack == "tanstack":
        os.environ["INTERNAL_PORT"] = "3000"
        os.environ["VITE_BASE_PATH"] = f"/{deploy_path}/" if deploy_path else "/"
        os.environ.pop("NG_APP_BASE_HREF", None)
    else:
        os.environ["INTERNAL_PORT"] = port
        os.environ.pop("NG_APP_BASE_HREF", None)
        os.environ.pop("VITE_BASE_PATH", None)


def configure_frontend_build_env(stack: str, svc_environment: str) -> None:
    if stack in ("angular", "angular_ssr"):
        os.environ["NG_BUILD_CONFIGURATION"] = svc_environment or "production"
        os.environ.pop("VITE_BUILD_MODE", None)
    elif stack in ("tanstack", "vue"):
        os.environ["VITE_BUILD_MODE"] = svc_environment or "production"
        os.environ.pop("NG_BUILD_CONFIGURATION", None)
    elif is_nginx_stack(stack):
        for k in ("NG_BUILD_CONFIGURATION", "NG_APP_BASE_HREF", "VITE_BUILD_MODE", "VITE_BASE_PATH"):
            os.environ.pop(k, None)
    else:
        for k in ("NG_BUILD_CONFIGURATION", "VITE_BUILD_MODE", "VITE_BASE_PATH"):
            os.environ.pop(k, None)


# --- Docker container conflict removal ---

def _docker_inspect_label(container_ref: str, label: str) -> str:
    r = subprocess.run(["docker", "inspect", "-f", f'{{{{ index .Config.Labels "{label}" }}}}', container_ref],
                       capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else ""


def remove_conflicting_compose_container(name, branch, container_name, expected_project, expected_service):
    if subprocess.run(["docker", "container", "inspect", container_name], capture_output=True).returncode != 0:
        return
    actual_project = _docker_inspect_label(container_name, "com.docker.compose.project")
    actual_service = _docker_inspect_label(container_name, "com.docker.compose.service")
    if actual_project == expected_project and actual_service == expected_service:
        return
    print(f"deploy.py: [{name}] removiendo contenedor incompatible {container_name} "
          f"(project={actual_project or 'sin-label'}, service={actual_service or 'sin-label'})")
    r = subprocess.run(["docker", "rm", "-f", container_name], capture_output=True)
    if r.returncode != 0:
        service_die(name, branch, f"no se pudo remover contenedor incompatible {container_name}")


def remove_conflicting_compose_container_id(name, branch, container_id, expected_project, expected_service):
    name_r = subprocess.run(["docker", "inspect", "-f", "{{ .Name }}", container_id], capture_output=True, text=True)
    actual_name = name_r.stdout.strip().lstrip("/") if name_r.returncode == 0 else ""
    actual_project = _docker_inspect_label(container_id, "com.docker.compose.project")
    actual_service = _docker_inspect_label(container_id, "com.docker.compose.service")
    if actual_project == expected_project and actual_service == expected_service:
        return
    label = actual_name or container_id
    print(f"deploy.py: [{name}] removiendo contenedor incompatible {label} "
          f"(project={actual_project or 'sin-label'}, service={actual_service or 'sin-label'})")
    r = subprocess.run(["docker", "rm", "-f", container_id], capture_output=True)
    if r.returncode != 0:
        service_die(name, branch, f"no se pudo remover contenedor incompatible {label}")


def remove_prefixed_conflicting_compose_containers(name, branch, container_name, expected_project, expected_service):
    r = subprocess.run(["docker", "ps", "-aq", "--filter", f"name={container_name}"],
                       capture_output=True, text=True)
    if r.returncode != 0:
        return
    for container_id in r.stdout.strip().splitlines():
        if not container_id:
            continue
        nr = subprocess.run(["docker", "inspect", "-f", "{{ .Name }}", container_id], capture_output=True, text=True)
        actual_name = nr.stdout.strip().lstrip("/") if nr.returncode == 0 else ""
        if (actual_name == container_name
                or actual_name.endswith(f"_{container_name}")
                or actual_name.endswith(f"-{container_name}")):
            remove_conflicting_compose_container_id(name, branch, container_id, expected_project, expected_service)


def remove_conflicting_compose_containers(name, branch, stack, compose_service):
    remove_conflicting_compose_container(name, branch, name, name, compose_service)
    remove_prefixed_conflicting_compose_containers(name, branch, name, name, compose_service)
    if stack == "spring_maria":
        remove_conflicting_compose_container(name, branch, f"{name}-mariadb", name, "mariadb")
        remove_prefixed_conflicting_compose_containers(name, branch, f"{name}-mariadb", name, "mariadb")
    elif stack in ("spring_postgres", "python_postgres"):
        remove_conflicting_compose_container(name, branch, f"{name}-postgres", name, "postgres")
        remove_prefixed_conflicting_compose_containers(name, branch, f"{name}-postgres", name, "postgres")


# --- Docker diagnostics ---

def dump_docker_app_diagnostics(svc_name: str, cid: str, compose_svc: str, compose_file_args: list) -> None:
    e = sys.stderr
    print("", file=e)
    print(f"========== deploy.py: diagnostico Docker ({svc_name}, servicio {compose_svc}) ==========", file=e)
    print("--- docker compose ps -a ---", file=e)
    subprocess.run(["docker", "compose"] + compose_file_args + ["ps", "-a"],
                   cwd=str(DEPLOY_DIR), stdout=e, stderr=e)
    print("", file=e)
    print(f"--- docker compose logs {compose_svc} (ultimas 500 lineas) ---", file=e)
    subprocess.run(["docker", "compose"] + compose_file_args + ["logs", "--no-color", "--tail=500", compose_svc],
                   cwd=str(DEPLOY_DIR), stdout=e, stderr=e)
    if cid:
        print("", file=e)
        print(f"--- docker inspect (State) cid={cid} ---", file=e)
        subprocess.run(["docker", "inspect", "-f",
                        "Status={{.State.Status}} ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} "
                        "Error={{.State.Error}} FinishedAt={{.State.FinishedAt}}", cid],
                       stdout=e, stderr=subprocess.DEVNULL)
        print("", file=e)
        print(f"--- docker logs (ultimas 500 lineas) cid={cid} ---", file=e)
        subprocess.run(["docker", "logs", "--tail=500", cid], stdout=e, stderr=e)
    print("========== fin diagnostico Docker ==========", file=e)
    print("", file=e)


# --- Host log directory ---

def ensure_host_log_directory_for_compose(svc_name: str) -> None:
    docker_uid, docker_gid = 10001, 10001
    host_log_dir = f"/var/log/{svc_name}"
    if not Path("/var/log").is_dir():
        print(f"deploy.py: aviso: no existe /var/log; omitiendo preparacion de {host_log_dir}.", file=sys.stderr)
        return
    if os.getuid() == 0:
        r1 = subprocess.run(["mkdir", "-p", host_log_dir])
        if r1.returncode != 0:
            die_discord(f"{svc_name}: no se pudo crear {host_log_dir}")
        r2 = subprocess.run(["chown", "-R", f"{docker_uid}:{docker_gid}", host_log_dir])
        if r2.returncode != 0:
            die_discord(f"{svc_name}: no se pudo chown {host_log_dir} a {docker_uid} (debe coincidir con Dockerfile)")
        return
    if shutil.which("sudo") is None:
        die_discord(f"{svc_name}: se requiere sudo o root para preparar {host_log_dir} (permisos de logs Docker UID {docker_uid})")
    if subprocess.run(["sudo", "mkdir", "-p", host_log_dir]).returncode != 0:
        die_discord(f"{svc_name}: sudo mkdir fallo en {host_log_dir}")
    if subprocess.run(["sudo", "chown", "-R", f"{docker_uid}:{docker_gid}", host_log_dir]).returncode != 0:
        die_discord(f"{svc_name}: sudo chown fallo en {host_log_dir} (use UID:GID {docker_uid}:{docker_gid} del Dockerfile)")


# --- nginx ---

def run_nginx_test_or_die(die_msg: str) -> None:
    with tempfile.NamedTemporaryFile(delete=False, suffix=".log") as lf:
        log_file = lf.name
    with open(log_file, "wb") as lf:
        r = subprocess.run(["sudo", "nginx", "-t"], stdout=lf, stderr=lf)
    if r.returncode == 0:
        os.unlink(log_file)
        return
    print("deploy.py: nginx -t fallo.", file=sys.stderr)
    print("---------- salida de nginx -t ----------", file=sys.stderr)
    with open(log_file) as lf:
        print(lf.read(), file=sys.stderr)
    print("---------- fin salida nginx -t ----------", file=sys.stderr)
    with open(log_file) as lf:
        snippet = lf.read(1500).replace("\n", " ")
    os.unlink(log_file)
    die_discord(f"{die_msg}: {snippet}")


def configure_nginx_for_service(name, branch, port, deploy_path, nginx_src, domain):
    if not nginx_src:
        return
    nginx_path = resolve_in_deploy_dir(nginx_src)
    if not nginx_path.is_file():
        service_die(name, branch, f"nginx_config no encontrado: {nginx_path}")
    content = nginx_path.read_text(encoding="utf-8")
    if "{PATH}" in content and not deploy_path:
        root_nginx_src = (nginx_src[:-5] if nginx_src.endswith(".conf") else nginx_src) + "-root.conf"
        root_nginx_path = resolve_in_deploy_dir(root_nginx_src)
        if not root_nginx_path.is_file():
            service_die(name, branch, f"falta path en deploy.yml y no existe plantilla root: {root_nginx_path}")
        nginx_src = root_nginx_src
        nginx_path = root_nginx_path
        content = nginx_path.read_text(encoding="utf-8")
    if "{PATH}" in content:
        rendered = content.replace("{PATH}", deploy_path).replace("{PORT}", port)
    else:
        rendered = content.replace("{PORT}", port)
    if domain:
        rendered = f"# domain: {domain}\n" + rendered

    if re.search(r'^\s*server\s*\{', content, re.MULTILINE):
        return

    tmp_fd, rendered_path = tempfile.mkstemp(suffix=".conf")
    try:
        os.close(tmp_fd)
        with open(rendered_path, "w", encoding="utf-8") as f:
            f.write(rendered)
        require_cmd("sudo")
        subprocess.run(["sudo", "mkdir", "-p", "/etc/nginx/snippets/ssl-fragments"], check=True)
        subprocess.run(["sudo", "rm", "-f", f"/etc/nginx/snippets/ssl-fragments/{name}.conf"], check=True)
        r = subprocess.run(["sudo", "install", "-m", "644", rendered_path,
                             f"/etc/nginx/snippets/ssl-fragments/{name}.conf"])
        if r.returncode != 0:
            service_die(name, branch, f"no se pudo escribir fragmento HTTPS /etc/nginx/snippets/ssl-fragments/{name}.conf")
    finally:
        try:
            os.unlink(rendered_path)
        except Exception:
            pass


def install_ssl_environment_fragments() -> None:
    frag_dir = DEPLOY_DIR / "nginx" / "ssl-fragments" / DEPLOY_ENV
    target = "/etc/nginx/snippets/ssl-fragments"
    if not frag_dir.is_dir():
        return
    files = list(frag_dir.glob("*.conf"))
    if not files:
        return
    require_cmd("sudo")
    subprocess.run(["sudo", "mkdir", "-p", target], check=True)
    for f in files:
        subprocess.run(["sudo", "install", "-m", "644", str(f), f"{target}/{f.name}"], check=True)
    run_nginx_test_or_die(f"deploy.py: nginx -t fallo tras instalar fragmentos en {target}")
    discord_notify(f"deploy.py: fragmentos nginx ({DEPLOY_ENV}) instalados en {target} ({len(files)} .conf). El reload queda a cargo del orquestador.")


# --- git ---

def validate_repo_root_for_destructive_git(root: str) -> Path:
    try:
        root_path = Path(root).resolve()
        deploy_path = DEPLOY_DIR.resolve()
        common = Path(os.path.commonpath([str(root_path), str(deploy_path)]))
    except Exception:
        die_discord(f"deploy.py: REPO_ROOT invalido para limpieza git: {root}")

    if common != root_path:
        die_discord(f"deploy.py: proteccion git: DEPLOY_DIR {deploy_path} no esta dentro de REPO_ROOT {root_path}")
    if root_path == deploy_path:
        die_discord(f"deploy.py: proteccion git: REPO_ROOT apunta al directorio deploy, no al repo de app ({root_path})")
    if not (root_path / ".git").exists():
        die_discord(f"deploy.py: proteccion git: REPO_ROOT no parece un repo git ({root_path})")
    if not (root_path / "deploy.yml").is_file():
        die_discord(f"deploy.py: proteccion git: no existe deploy.yml en REPO_ROOT ({root_path})")
    if root_path.parent == root_path or str(root_path) in ("/", "/home"):
        die_discord(f"deploy.py: proteccion git: REPO_ROOT demasiado amplio ({root_path})")
    return root_path


def clean_git_worktree(root: str) -> None:
    root_path = validate_repo_root_for_destructive_git(root)
    print(f"deploy.py: descartando cambios locales en {root_path} (reset --hard, clean -fd)...")
    subprocess.run(["git", "reset", "--hard", "HEAD"], check=True, cwd=str(root_path))
    subprocess.run(["git", "clean", "-fd"], check=True, cwd=str(root_path))


def checkout_service_branch(svc_name: str, branch: str, root: str) -> None:
    clean_git_worktree(root)
    print(f"deploy.py: [{svc_name}] fetch y checkout de rama: {branch}")
    r = subprocess.run(["git", "fetch", "origin", branch], capture_output=True, cwd=root)
    if r.returncode != 0:
        r2 = subprocess.run(["git", "fetch", "origin"], cwd=root)
        if r2.returncode != 0:
            die_discord(f"{svc_name} {branch} {DEPLOY_ENV} - End | git fetch fallo")
    clean_git_worktree(root)
    if subprocess.run(["git", "show-ref", "--verify", "--quiet", f"refs/remotes/origin/{branch}"], cwd=root).returncode == 0:
        r1 = subprocess.run(["git", "checkout", "-B", branch, f"origin/{branch}"], cwd=root)
        r2 = subprocess.run(["git", "reset", "--hard", f"origin/{branch}"], cwd=root)
        if r1.returncode != 0 or r2.returncode != 0:
            die_discord(f"{svc_name} {branch} {DEPLOY_ENV} - End | checkout origin/{branch} fallo")
    elif subprocess.run(["git", "rev-parse", "--verify", branch], capture_output=True, cwd=root).returncode == 0:
        r1 = subprocess.run(["git", "checkout", branch], cwd=root)
        r2 = subprocess.run(["git", "reset", "--hard", "HEAD"], cwd=root)
        if r1.returncode != 0 or r2.returncode != 0:
            die_discord(f"{svc_name} {branch} {DEPLOY_ENV} - End | checkout {branch} fallo")
    else:
        die_discord(f"{svc_name} {branch} {DEPLOY_ENV} - End | rama '{branch}' no existe en origin ni local")
    clean_git_worktree(root)
    head = subprocess.run(["git", "rev-parse", "--abbrev-ref", "HEAD"], capture_output=True, text=True, cwd=root).stdout.strip()
    short = subprocess.run(["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True, cwd=root).stdout.strip()
    print(f"deploy.py: [{svc_name}] rama activa: {head} ({short})")


# --- deploy.yml parsing ---

def list_services_from_deploy_yml() -> list:
    try:
        import yaml
    except ImportError:
        die_discord("instale python3-yaml (apt install python3-yaml)")
    path = Path(REPO_ROOT) / "deploy.yml"
    if not path.is_file():
        die_discord(f"no existe {path}")
    with open(str(path), encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict) or DEPLOY_ENV not in data:
        die_discord(f"en deploy.yml no existe el entorno [{DEPLOY_ENV}]")
    block = data[DEPLOY_ENV]
    if not isinstance(block, dict):
        die_discord(f"bloque [{DEPLOY_ENV}] no es un mapa en deploy.yml")
    services = []
    for svc_name, cfg in block.items():
        if not isinstance(cfg, dict):
            continue
        services.append({
            "name": svc_name,
            "stack": str(cfg.get("stack", "spring") or "spring").strip().lower(),
            "compose_service": str(cfg.get("compose_service", "") or "app").strip(),
            "port": str(cfg.get("port", "")).strip(),
            "branch": str(cfg.get("branch", "")).strip(),
            "path": str(cfg.get("path", "") or "").strip(),
            "domain": str(cfg.get("domain", "") or "").strip(),
            "ssl": str(cfg.get("ssl", "") or "").strip(),
            "composer_file": str(cfg.get("composer_file", "") or "").strip(),
            "nginx_config": str(cfg.get("nginx_config", "") or "").strip(),
            "application_properties": str(cfg.get("application_properties", "") or "").strip(),
            "script": str(cfg.get("script", "") or "").strip(),
            "environment": str(cfg.get("environment", "") or "").strip(),
            "mysql_user": str(cfg.get("mysql_user", "") or "").strip(),
            "mysql_db": str(cfg.get("mysql_db", "") or "").strip(),
            "mysql_pass": str(cfg.get("mysql_pass", "") or "").strip(),
            "mysql_external_port": str(cfg.get("mysql_external_port", "") or "").strip(),
            "postgres_user": str(cfg.get("postgres_user", "") or "").strip(),
            "postgres_db": str(cfg.get("postgres_db", "") or "").strip(),
            "postgres_pass": str(cfg.get("postgres_pass", "") or "").strip(),
            "postgres_external_port": str(cfg.get("postgres_external_port", "") or "").strip(),
            "env": cfg.get("env") if isinstance(cfg.get("env"), dict) else {},
        })
    return services


# --- Post-deploy hook ---

def run_post_deploy_hook(name: str, branch: str, hook_script: str) -> None:
    if not hook_script:
        return
    script_path = resolve_in_deploy_dir(hook_script)
    if not script_path.is_file():
        service_die(name, branch, f"script configurado pero no existe: {script_path}")
    if subprocess.run(["bash", str(script_path)], cwd=str(DEPLOY_DIR)).returncode != 0:
        service_die(name, branch, f"el script post-despliegue fallo: {script_path}")


# --- Wait for container ---

def wait_for_compose_service(name, branch, compose_service, start_time, compose_file_args):
    segundos_restantes = 20
    print(f"Comprobando contenedor Docker durante 20 segundos ({name}, {compose_service})...")
    r = subprocess.run(["docker", "compose"] + compose_file_args + ["ps", "-q", compose_service],
                       capture_output=True, text=True, cwd=str(DEPLOY_DIR))
    cid = r.stdout.strip() if r.returncode == 0 else ""
    if not cid:
        dump_docker_app_diagnostics(name, "", compose_service, compose_file_args)
        service_die(name, branch, f"no hay contenedor para el servicio {compose_service} en compose")
    while segundos_restantes > 0:
        ri = subprocess.run(["docker", "inspect", "-f", "{{.State.Running}}", cid],
                            capture_output=True, text=True)
        if ri.stdout.strip() != "true":
            total_duration = int(time.time()) - start_time
            dump_docker_app_diagnostics(name, cid, compose_service, compose_file_args)
            die_discord(f"{name} Be {branch} {DEPLOY_ENV} - End ERROR contenedor no en ejecucion - Duracion: {total_duration}s")
        print(f"  [{name}] Faltan {segundos_restantes:2d} s")
        time.sleep(1)
        segundos_restantes -= 1


# --- Deploy one service ---

def deploy_one_service(row: dict) -> None:
    global CURRENT_SERVICE, CURRENT_BRANCH

    name = row["name"]
    stack = row["stack"] or "spring"
    compose_service = row["compose_service"] or "app"
    port = row["port"]
    branch = row["branch"]
    deploy_path = row["path"]
    domain = row["domain"]
    composer = row["composer_file"]
    nginx_src = row["nginx_config"]
    app_props = row["application_properties"]
    hook_script = row["script"]
    svc_environment = row["environment"]
    mysql_user = row["mysql_user"]
    mysql_db = row["mysql_db"]
    mysql_pass = row["mysql_pass"]
    mysql_external_port = row["mysql_external_port"]
    postgres_user = row["postgres_user"]
    postgres_db = row["postgres_db"]
    postgres_pass = row["postgres_pass"]
    postgres_external_port = row["postgres_external_port"]

    CURRENT_SERVICE = name
    CURRENT_BRANCH = branch
    start_time = int(time.time())

    require_non_empty(name, branch, "branch", branch)
    checkout_service_branch(name, branch, REPO_ROOT)
    discord_notify(f"{name} Docker {branch} {DEPLOY_ENV} ({stack}) - Start")

    if is_spring_stack(stack):
        composer, nginx_src = apply_spring_stack_defaults(stack, composer, nginx_src)

    require_non_empty(name, branch, "composer_file", composer)
    require_non_empty(name, branch, "port", port)
    validate_stack_config(name, branch, stack, app_props,
                          mysql_user, mysql_db, mysql_pass, mysql_external_port,
                          postgres_user, postgres_db, postgres_pass, postgres_external_port)
    configure_frontend_build_env(stack, svc_environment)

    if nginx_src:
        require_cmd("sudo")

    composer_path = resolve_in_deploy_dir(composer)
    if not composer_path.is_file():
        service_die(name, branch, f"no existe composer_file: {composer_path}")

    if is_spring_stack(stack):
        prepare_spring_application_properties(name, branch, app_props)

    ensure_default_dockerignore(name, branch)
    write_runtime_env_file(name, branch, row)
    export_service_port_vars(name, port, stack, deploy_path)

    if stack in ("spring_maria", "spring_postgres", "python_postgres"):
        compose_file_args = configure_embedded_db_compose(
            stack, composer,
            mysql_user, mysql_db, mysql_pass, mysql_external_port,
            postgres_user, postgres_db, postgres_pass, postgres_external_port,
        )
    else:
        compose_file_args = ["-f", composer]
        clear_embedded_db_env()

    if is_spring_stack(stack):
        ensure_host_log_directory_for_compose(name)

    remove_conflicting_compose_containers(name, branch, stack, compose_service)

    env = os.environ.copy()
    env["DOCKER_BUILDKIT"] = "1"
    env["COMPOSE_DOCKER_CLI_BUILD"] = "1"
    r = subprocess.run(["docker", "compose"] + compose_file_args + ["up", "-d", "--build"],
                       cwd=str(DEPLOY_DIR), env=env)
    if r.returncode != 0:
        service_die(name, branch, "docker compose fallo (up -d --build)")

    configure_nginx_for_service(name, branch, port, deploy_path, nginx_src, domain)
    run_post_deploy_hook(name, branch, hook_script)
    wait_for_compose_service(name, branch, compose_service, start_time, compose_file_args)

    print("")
    print(f"---------- Logs recientes del contenedor ({name}, servicio {compose_service}) ----------")
    subprocess.run(["docker", "compose"] + compose_file_args + ["logs", "--no-color", "--tail=300", compose_service],
                   cwd=str(DEPLOY_DIR))
    print("---------- fin logs ----------")
    print("")

    print("El script se ejecutó exitosamente.")
    print("Logs del contenedor:")
    print(f' docker logs -f "{name}"')
    print("")

    total_duration = int(time.time()) - start_time
    discord_notify(f"{name} Be {branch} {DEPLOY_ENV} ({stack}) - End - Duracion: {total_duration}s")

    CURRENT_SERVICE = ""
    CURRENT_BRANCH = ""


# --- read env / repo root ---

def read_deploy_env_from_home() -> None:
    global DEPLOY_ENV
    if not HOME_ENV_FILE.is_file():
        die_discord(f"deploy.py: no existe {HOME_ENV_FILE}; no se puede determinar el entorno (env).")
    content = HOME_ENV_FILE.read_text(encoding="utf-8", errors="replace")
    m = re.search(r'^\s*[Ee][Nn][Vv]=(.+)$', content, re.MULTILINE)
    if not m:
        die_discord(f"deploy.py: en {HOME_ENV_FILE} no se encontro la variable env= (o ENV=) con valor no vacio.")
    val = m.group(1).strip().strip('"').strip("'").strip()
    if not val:
        die_discord(f"deploy.py: en {HOME_ENV_FILE} no se encontro la variable env= (o ENV=) con valor no vacio.")
    DEPLOY_ENV = val


def resolve_repo_root() -> None:
    global REPO_ROOT
    r = subprocess.run(["git", "-C", str(DEPLOY_DIR), "rev-parse", "--show-toplevel"],
                       capture_output=True, text=True)
    if r.returncode == 0:
        REPO_ROOT = r.stdout.strip()
    else:
        REPO_ROOT = str(DEPLOY_DIR.parent)
    validate_repo_root_for_destructive_git(REPO_ROOT)


# --- main ---

def main() -> None:
    require_cmd("docker")
    require_cmd("git")

    read_deploy_env_from_home()
    resolve_repo_root()

    os.environ["DEPLOY_DIR"] = str(DEPLOY_DIR)
    os.environ["DEPLOY_ENV"] = DEPLOY_ENV
    os.environ["REPO_ROOT"] = REPO_ROOT

    deploy_yml = Path(REPO_ROOT) / "deploy.yml"
    if not deploy_yml.is_file():
        die_discord(f"deploy.py: no existe deploy.yml en {REPO_ROOT}")

    services = list_services_from_deploy_yml()
    require_sudo_for_nginx_work(services)
    for row in services:
        deploy_one_service(row)

    install_ssl_environment_fragments()


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception as exc:
        context = "deploy.py"
        if CURRENT_SERVICE:
            context = f"deploy.py: servicio={CURRENT_SERVICE}"
            if CURRENT_BRANCH:
                context += f" branch={CURRENT_BRANCH}"
            if DEPLOY_ENV:
                context += f" env={DEPLOY_ENV}"
        msg = f"{context}: ERROR inesperado: {exc}"
        print(msg, file=sys.stderr)
        traceback.print_exc()
        discord_notify(msg)
        sys.exit(1)

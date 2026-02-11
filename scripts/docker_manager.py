#!/usr/bin/env python3
"""
Docker Manager for GridAPPS-D Development and Testing

Manages Docker containers for local development and testing.
Supports version tagging for backport testing against different releases.

Usage:
    python3 scripts/docker_manager.py up [--version VERSION]
    python3 scripts/docker_manager.py down
    python3 scripts/docker_manager.py logs [SERVICE]
    python3 scripts/docker_manager.py versions
    python3 scripts/docker_manager.py status
"""

import argparse
import json
import os
import socket
import subprocess
import sys
import time
import urllib.request
import urllib.error
import urllib.parse
from pathlib import Path

# ANSI color codes
class Colors:
    RED = '\033[91m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

def color(text: str, color_code: str) -> str:
    """Apply color to text if stdout is a terminal."""
    if sys.stdout.isatty():
        return f"{color_code}{text}{Colors.RESET}"
    return text

def info(msg: str):
    print(color(f"[INFO] {msg}", Colors.CYAN))

def success(msg: str):
    print(color(f"[OK] {msg}", Colors.GREEN))

def warn(msg: str):
    print(color(f"[WARN] {msg}", Colors.YELLOW))

def error(msg: str):
    print(color(f"[ERROR] {msg}", Colors.RED), file=sys.stderr)

def get_script_dir() -> Path:
    """Get the directory containing this script."""
    return Path(__file__).parent.resolve()

def get_project_root() -> Path:
    """Get the project root directory (parent of scripts/)."""
    return get_script_dir().parent

def get_docker_dir() -> Path:
    """Get the docker directory."""
    return get_project_root() / "docker"

def get_compose_command() -> list:
    """Detect whether to use 'docker compose' or 'docker-compose'."""
    # Try 'docker compose' first (newer)
    result = subprocess.run(
        ["docker", "compose", "version"],
        capture_output=True,
        text=True
    )
    if result.returncode == 0:
        return ["docker", "compose"]

    # Fall back to 'docker-compose'
    result = subprocess.run(
        ["docker-compose", "version"],
        capture_output=True,
        text=True
    )
    if result.returncode == 0:
        return ["docker-compose"]

    error("Neither 'docker compose' nor 'docker-compose' found. Please install Docker.")
    sys.exit(1)

def run_compose(args: list, env: dict = None, check: bool = True) -> subprocess.CompletedProcess:
    """Run a docker compose command."""
    compose_cmd = get_compose_command()
    docker_dir = get_docker_dir()

    cmd = compose_cmd + ["-f", str(docker_dir / "docker-compose.yml")] + args

    # Merge environment
    full_env = os.environ.copy()
    if env:
        full_env.update(env)

    return subprocess.run(cmd, env=full_env, check=check)

def cmd_up(args):
    """Start containers."""
    version = args.version
    tag = f":{version}" if version else ":develop"

    info(f"Starting GridAPPS-D containers with dependency version: {version or 'develop'}")
    info(f"GridAPPS-D container will use: gridappsd/gridappsd:local")

    # Check if local image exists
    result = subprocess.run(
        ["docker", "images", "-q", "gridappsd/gridappsd:local"],
        capture_output=True,
        text=True
    )
    if not result.stdout.strip():
        warn("gridappsd/gridappsd:local image not found.")
        warn("Build it first with: make docker")
        if not args.force:
            error("Use --force to start anyway (will fail if image doesn't exist)")
            sys.exit(1)

    env = {"GRIDAPPSD_TAG": tag}

    # Set autostart mode
    if args.autostart:
        env["AUTOSTART"] = "1"
        info("AUTOSTART=1 enabled - GridAPPS-D will start automatically")
    else:
        env["AUTOSTART"] = "0"
        info("AUTOSTART=0 - container will wait (use 'docker exec -it gridappsd bash' to start manually)")

    # Pull images if requested
    if args.pull:
        info("Pulling latest images...")
        run_compose(["pull"], env=env, check=False)

    # Start containers
    info("Starting containers...")
    run_compose(["up", "-d"], env=env)

    # Read port mappings from compose config
    config = get_compose_config(env)
    gridappsd_ports = get_service_ports(config, "gridappsd")
    blazegraph_ports = get_service_ports(config, "blazegraph")
    proven_ports = get_service_ports(config, "proven")

    # Wait for STOMP and OpenWire ports to become reachable
    stomp_port = gridappsd_ports.get(61613)
    openwire_port = gridappsd_ports.get(61616)
    check_ports = []
    if stomp_port:
        check_ports.append(("STOMP", stomp_port))
    if openwire_port:
        check_ports.append(("OpenWire", openwire_port))

    if check_ports:
        info("Waiting for services to be ready...")
        all_ready = wait_for_ports(check_ports, timeout=120)
        if all_ready:
            success("All services are ready!")
        else:
            warn("Some services did not become ready. Check logs with: make docker-logs")
    else:
        warn("Could not determine gridappsd ports from compose config.")

    # Print endpoints using configured ports
    print()
    print("Available endpoints:")
    if gridappsd_ports.get(61613):
        print(f"  STOMP:      tcp://localhost:{gridappsd_ports[61613]}")
    if gridappsd_ports.get(61614):
        print(f"  WebSocket:  ws://localhost:{gridappsd_ports[61614]}")
    if gridappsd_ports.get(61616):
        print(f"  OpenWire:   tcp://localhost:{gridappsd_ports[61616]}")
    if blazegraph_ports.get(8080):
        print(f"  Blazegraph: http://localhost:{blazegraph_ports[8080]}/bigdata/")
    if proven_ports.get(8080):
        print(f"  Proven:     http://localhost:{proven_ports[8080]}/")
    print()
    print("Check status: make docker-status")
    print("View logs:    make docker-logs")
    print("Stop:         make docker-down")


def get_compose_config(env: dict = None) -> dict:
    """Get the resolved docker compose config as a dict."""
    compose_cmd = get_compose_command()
    docker_dir = get_docker_dir()
    cmd = compose_cmd + ["-f", str(docker_dir / "docker-compose.yml"), "config", "--format", "json"]

    full_env = os.environ.copy()
    if env:
        full_env.update(env)

    result = subprocess.run(cmd, capture_output=True, text=True, env=full_env)
    if result.returncode != 0:
        return {}
    return json.loads(result.stdout)


def get_service_ports(config: dict, service: str) -> dict[int, int]:
    """Extract port mappings for a service from resolved compose config.

    Returns a dict mapping container_port -> host_port.
    """
    ports = {}
    svc = config.get("services", {}).get(service, {})
    for mapping in svc.get("ports", []):
        if isinstance(mapping, dict):
            # Compose config format: {"published": 61613, "target": 61613, ...}
            container_port = int(mapping.get("target", 0))
            host_port = int(mapping.get("published", 0))
            if container_port and host_port:
                ports[container_port] = host_port
        elif isinstance(mapping, str):
            # Simple "host:container" format
            parts = str(mapping).split(":")
            if len(parts) == 2:
                ports[int(parts[1])] = int(parts[0])
    return ports


def wait_for_ports(ports: list[tuple[str, int]], timeout: int = 120) -> bool:
    """Wait for TCP ports to become reachable on localhost."""
    start_time = time.time()
    pending = dict(ports)  # name -> port

    while pending and (time.time() - start_time) < timeout:
        for name, port in list(pending.items()):
            try:
                with socket.create_connection(("localhost", port), timeout=2):
                    elapsed = int(time.time() - start_time)
                    info(f"{name} (port {port}) is ready  [{elapsed}s]")
                    del pending[name]
            except (ConnectionRefusedError, OSError):
                pass
        if pending:
            time.sleep(3)

    for name, port in pending.items():
        warn(f"{name} (port {port}) not reachable after {timeout}s")

    return len(pending) == 0


def cmd_down(args):
    """Stop containers."""
    info("Stopping GridAPPS-D containers...")

    # We need to provide the tag even for down, use develop as default
    env = {"GRIDAPPSD_TAG": ":develop"}

    if args.volumes:
        run_compose(["down", "-v"], env=env)
        success("Containers stopped and volumes removed.")
    else:
        run_compose(["down"], env=env)
        success("Containers stopped.")

def cmd_logs(args):
    """Tail container logs."""
    service = args.service or "gridappsd"
    info(f"Tailing logs for: {service}")

    env = {"GRIDAPPSD_TAG": ":develop"}

    try:
        cmd = ["logs", "-f", "--tail", str(args.tail)]
        if service != "all":
            cmd.append(service)
        run_compose(cmd, env=env)
    except KeyboardInterrupt:
        pass

def cmd_status(args):
    """Show container status."""
    env = {"GRIDAPPSD_TAG": ":develop"}
    run_compose(["ps"], env=env)

def cmd_versions(args):
    """List available Docker Hub versions."""
    info("Fetching available versions from Docker Hub...")

    repos = [
        "gridappsd/gridappsd",
        "gridappsd/blazegraph",
        "gridappsd/proven",
        "gridappsd/influxdb",
    ]

    for repo in repos:
        print(f"\n{color(repo, Colors.BOLD)}:")
        try:
            tags = fetch_docker_tags(repo, limit=args.limit)
            for tag in tags:
                print(f"  {tag}")
        except Exception as e:
            warn(f"  Failed to fetch tags: {e}")

def fetch_docker_tags(repo: str, limit: int = 10) -> list:
    """Fetch tags for a Docker Hub repository using urllib (stdlib)."""
    params = urllib.parse.urlencode({
        "page_size": limit,
        "ordering": "-last_updated"
    })
    url = f"https://hub.docker.com/v2/repositories/{repo}/tags?{params}"

    req = urllib.request.Request(url, headers={"User-Agent": "GridAPPS-D Docker Manager"})

    with urllib.request.urlopen(req, timeout=10) as response:
        data = json.loads(response.read().decode("utf-8"))

    return [result["name"] for result in data.get("results", [])]

def main():
    parser = argparse.ArgumentParser(
        description="Docker Manager for GridAPPS-D Development and Testing",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s up                          Start with default (develop) version
  %(prog)s up --version v2025.09.0     Start with specific version for backport testing
  %(prog)s down                        Stop all containers
  %(prog)s down --volumes              Stop and remove volumes
  %(prog)s logs                        Tail gridappsd logs
  %(prog)s logs blazegraph             Tail blazegraph logs
  %(prog)s status                      Show container status
  %(prog)s versions                    List available Docker Hub versions
"""
    )

    subparsers = parser.add_subparsers(dest="command", help="Command to run")

    # up command
    up_parser = subparsers.add_parser("up", help="Start containers")
    up_parser.add_argument(
        "--version", "-v",
        default=None,
        help="Version tag for dependency containers (default: develop)"
    )
    up_parser.add_argument(
        "--pull", "-p",
        action="store_true",
        help="Pull latest images before starting"
    )
    up_parser.add_argument(
        "--force", "-f",
        action="store_true",
        help="Start even if gridappsd:local image is missing"
    )
    up_parser.add_argument(
        "--autostart", "-a",
        action="store_true",
        help="Auto-start GridAPPS-D when container starts (sets AUTOSTART=1)"
    )
    up_parser.set_defaults(func=cmd_up)

    # down command
    down_parser = subparsers.add_parser("down", help="Stop containers")
    down_parser.add_argument(
        "--volumes", "-v",
        action="store_true",
        help="Remove volumes as well"
    )
    down_parser.set_defaults(func=cmd_down)

    # logs command
    logs_parser = subparsers.add_parser("logs", help="Tail container logs")
    logs_parser.add_argument(
        "service",
        nargs="?",
        default="gridappsd",
        help="Service to show logs for (default: gridappsd, use 'all' for all)"
    )
    logs_parser.add_argument(
        "--tail", "-n",
        type=int,
        default=100,
        help="Number of lines to show (default: 100)"
    )
    logs_parser.set_defaults(func=cmd_logs)

    # status command
    status_parser = subparsers.add_parser("status", help="Show container status")
    status_parser.set_defaults(func=cmd_status)

    # versions command
    versions_parser = subparsers.add_parser("versions", help="List available Docker Hub versions")
    versions_parser.add_argument(
        "--limit", "-l",
        type=int,
        default=10,
        help="Number of tags to show per repository (default: 10)"
    )
    versions_parser.set_defaults(func=cmd_versions)

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    args.func(args)

if __name__ == "__main__":
    main()

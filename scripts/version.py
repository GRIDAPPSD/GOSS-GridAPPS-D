#!/usr/bin/env python3
"""
GridAPPS-D Version Management Script

Commands:
  show         - Display versions of all bundles
  release      - Set release version (removes -SNAPSHOT)
  snapshot     - Set snapshot version (adds -SNAPSHOT)
  bump-patch   - Bump patch version (x.y.Z) and set as snapshot
  bump-minor   - Bump minor version (x.Y.0) and set as snapshot
  bump-major   - Bump major version (X.0.0) and set as snapshot
  next-snapshot - Bump patch version after a release
"""

import argparse
import re
import sys
from pathlib import Path


# ANSI Colors
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    CYAN = '\033[0;36m'
    MAGENTA = '\033[0;35m'
    NC = '\033[0m'  # No Color


def log_info(msg: str) -> None:
    print(f"{Colors.GREEN}[INFO]{Colors.NC} {msg}")


def log_warn(msg: str) -> None:
    print(f"{Colors.YELLOW}[WARN]{Colors.NC} {msg}")


def log_error(msg: str) -> None:
    print(f"{Colors.RED}[ERROR]{Colors.NC} {msg}")


def find_bnd_files(root: Path) -> list[Path]:
    """Find all .bnd files that contain Bundle-Version."""
    bnd_files = []
    for bnd_file in root.rglob('*.bnd'):
        # Skip directories (some tools create .bnd directories)
        if not bnd_file.is_file():
            continue
        # Skip cnf/ext directory (these are config files, not bundles)
        if 'cnf/ext' in str(bnd_file):
            continue
        # Skip generated directories
        if '/generated/' in str(bnd_file):
            continue
        # Skip cnf/build.bnd and cnf/bnd.bnd (workspace config)
        if bnd_file.parent.name == 'cnf' and bnd_file.name in ('build.bnd', 'bnd.bnd'):
            continue
        # Check if file contains Bundle-Version
        content = bnd_file.read_text()
        if 'Bundle-Version:' in content:
            bnd_files.append(bnd_file)
    return sorted(bnd_files)


def extract_bundle_info(bnd_file: Path) -> tuple[str, str] | None:
    """Extract bundle name and version from a .bnd file."""
    content = bnd_file.read_text()

    # Extract Bundle-Version
    version_match = re.search(r'Bundle-Version:\s*(.+)', content)
    if not version_match:
        return None

    version = version_match.group(1).strip()

    # Derive bundle name from file path
    parent_dir = bnd_file.parent.name
    bundle_name = bnd_file.stem

    if bundle_name == 'bnd':
        full_name = parent_dir
    else:
        full_name = f"{parent_dir}.{bundle_name}"

    return (full_name, version)


def show_versions(gridappsd_root: Path) -> None:
    """Display versions of all GridAPPS-D bundles."""

    print(f"\n{Colors.CYAN}{'=' * 70}{Colors.NC}")
    print(f"{Colors.CYAN}GridAPPS-D Bundle Versions{Colors.NC}")
    print(f"{Colors.CYAN}{'=' * 70}{Colors.NC}")

    gridappsd_files = find_bnd_files(gridappsd_root)
    gridappsd_versions: dict[str, list[str]] = {}

    for bnd_file in gridappsd_files:
        info = extract_bundle_info(bnd_file)
        if info:
            name, version = info
            if version not in gridappsd_versions:
                gridappsd_versions[version] = []
            gridappsd_versions[version].append(name)

    for version in sorted(gridappsd_versions.keys()):
        is_snapshot = '-SNAPSHOT' in version or '${tstamp}' in version
        version_color = Colors.YELLOW if is_snapshot else Colors.GREEN
        print(f"\n{version_color}{version}{Colors.NC}:")
        for name in sorted(gridappsd_versions[version]):
            print(f"  - {name}")

    gridappsd_total = sum(len(v) for v in gridappsd_versions.values())
    print(f"\n  Total: {gridappsd_total} bundle(s)")
    print()


def update_version(bnd_file: Path, new_version: str) -> bool:
    """Update Bundle-Version in a .bnd file."""
    content = bnd_file.read_text()

    # Replace Bundle-Version line
    new_content, count = re.subn(
        r'(Bundle-Version:\s*).+',
        f'\\g<1>{new_version}',
        content
    )

    if count > 0:
        bnd_file.write_text(new_content)
        return True
    return False


def get_current_version(gridappsd_root: Path) -> str | None:
    """Get the current version from GridAPPS-D .bnd files (returns base version without -SNAPSHOT)."""
    bnd_files = find_bnd_files(gridappsd_root)

    versions: set[str] = set()
    for bnd_file in bnd_files:
        info = extract_bundle_info(bnd_file)
        if info:
            _, version = info
            # Strip -SNAPSHOT suffix and .${tstamp} qualifier for comparison
            base_version = version.replace('-SNAPSHOT', '').replace('.${tstamp}', '')
            # Must be a valid x.y.z version
            if re.match(r'^\d+\.\d+\.\d+$', base_version):
                versions.add(base_version)

    if len(versions) == 0:
        return None
    if len(versions) > 1:
        log_warn(f"Multiple versions found: {sorted(versions)}")
        # Return the highest version
        return sorted(versions, key=lambda v: [int(x) for x in v.split('.')])[-1]

    return versions.pop()


def bump_version(version: str, bump_type: str) -> str:
    """Bump a version string by the specified type (major, minor, patch)."""
    parts = [int(x) for x in version.split('.')]

    if bump_type == 'major':
        parts[0] += 1
        parts[1] = 0
        parts[2] = 0
    elif bump_type == 'minor':
        parts[1] += 1
        parts[2] = 0
    elif bump_type == 'patch':
        parts[2] += 1

    return '.'.join(str(p) for p in parts)


def set_version(gridappsd_root: Path, version: str, snapshot: bool = False) -> None:
    """Set version for all GridAPPS-D bundles."""
    # Validate version format
    if not re.match(r'^\d+\.\d+\.\d+$', version):
        log_error(f"Invalid version format: {version}")
        log_error("Expected format: x.y.z (e.g., 2.0.0)")
        sys.exit(1)

    # Add or remove -SNAPSHOT suffix
    if snapshot:
        full_version = f"{version}-SNAPSHOT"
    else:
        full_version = version

    action = "snapshot" if snapshot else "release"
    log_info(f"Setting {action} version: {full_version}")
    print()

    print(f"{Colors.CYAN}GridAPPS-D bundles:{Colors.NC}")
    gridappsd_files = find_bnd_files(gridappsd_root)
    gridappsd_count = 0

    for bnd_file in gridappsd_files:
        info = extract_bundle_info(bnd_file)
        if info:
            name, old_version = info
            if update_version(bnd_file, full_version):
                print(f"  {Colors.GREEN}✓{Colors.NC} {name}: {old_version} -> {full_version}")
                gridappsd_count += 1

    print()
    log_info(f"Updated {gridappsd_count} GridAPPS-D bundle(s)")

    if not snapshot:
        print()
        log_info("Next steps for release:")
        print(f"  1. Build:    make build (or ./gradlew build)")
        print(f"  2. Test:     make test")
        print(f"  3. Commit:   git commit -am 'Release version {version}'")
        print(f"  4. Tag:      git tag -a v{version} -m 'Version {version}'")
        print(f"  5. Push:     git push && git push --tags")
        print()


def do_bump(gridappsd_root: Path, bump_type: str) -> int:
    """Bump version and set as snapshot."""
    current = get_current_version(gridappsd_root)
    if not current:
        log_error("Could not determine current version")
        return 1

    new_version = bump_version(current, bump_type)
    log_info(f"Bumping {bump_type} version: {current} -> {new_version}-SNAPSHOT")
    set_version(gridappsd_root, new_version, snapshot=True)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description='GridAPPS-D Version Management',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  %(prog)s show                    # Show all bundle versions
  %(prog)s release 2.0.0           # Set release version 2.0.0
  %(prog)s snapshot 2.1.0          # Set snapshot version 2.1.0-SNAPSHOT
  %(prog)s bump-patch              # 2.0.0 -> 2.0.1-SNAPSHOT
  %(prog)s bump-minor              # 2.0.0 -> 2.1.0-SNAPSHOT
  %(prog)s bump-major              # 2.0.0 -> 3.0.0-SNAPSHOT
  %(prog)s next-snapshot           # After release: bump patch to next snapshot

Typical release workflow:
  1. %(prog)s show                  # Verify current version (e.g., 2.0.0-SNAPSHOT)
  2. %(prog)s release 2.0.0        # Remove -SNAPSHOT for release
  3. make build && make test       # Build and test
  4. git tag v2.0.0 && git push    # Tag and push
  5. %(prog)s next-snapshot        # Bump to 2.0.1-SNAPSHOT for next development
'''
    )

    subparsers = parser.add_subparsers(dest='command', help='Command to run')

    # show command
    subparsers.add_parser('show', help='Show versions of all bundles')

    # release command
    release_parser = subparsers.add_parser('release', help='Set release version (removes -SNAPSHOT)')
    release_parser.add_argument('version', help='Version number (e.g., 2.0.0)')

    # snapshot command
    snapshot_parser = subparsers.add_parser('snapshot', help='Set snapshot version (adds -SNAPSHOT)')
    snapshot_parser.add_argument('version', help='Version number (e.g., 2.1.0)')

    # bump commands
    subparsers.add_parser('bump-patch', help='Bump patch version (x.y.Z) and set as snapshot')
    subparsers.add_parser('bump-minor', help='Bump minor version (x.Y.0) and set as snapshot')
    subparsers.add_parser('bump-major', help='Bump major version (X.0.0) and set as snapshot')
    subparsers.add_parser('next-snapshot', help='Bump patch version after a release (alias for bump-patch)')

    args = parser.parse_args()

    # Find root directory
    script_dir = Path(__file__).parent.resolve()
    gridappsd_root = script_dir.parent

    if not args.command:
        parser.print_help()
        return 1

    if args.command == 'show':
        show_versions(gridappsd_root)
    elif args.command == 'release':
        set_version(gridappsd_root, args.version, snapshot=False)
    elif args.command == 'snapshot':
        set_version(gridappsd_root, args.version, snapshot=True)
    elif args.command in ('bump-patch', 'next-snapshot'):
        return do_bump(gridappsd_root, 'patch')
    elif args.command == 'bump-minor':
        return do_bump(gridappsd_root, 'minor')
    elif args.command == 'bump-major':
        return do_bump(gridappsd_root, 'major')

    return 0


if __name__ == '__main__':
    sys.exit(main())

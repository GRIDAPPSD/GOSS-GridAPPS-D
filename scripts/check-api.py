#!/usr/bin/env python3
"""
GridAPPS-D API Change Detector

Analyzes Java class files to detect API changes and suggest appropriate version bumps:
- Major: Interface changes, removed public methods, breaking changes
- Minor: New public methods on classes, new classes
- Patch: Implementation-only changes

Uses javap to extract public API signatures from JAR files.
"""

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional


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


@dataclass
class ClassInfo:
    """Information about a Java class's public API."""
    name: str
    is_interface: bool = False
    is_abstract: bool = False
    is_enum: bool = False
    superclass: Optional[str] = None
    interfaces: list[str] = field(default_factory=list)
    public_methods: list[str] = field(default_factory=list)
    public_fields: list[str] = field(default_factory=list)

    def signature_hash(self) -> str:
        """Generate a hash of the public API signature."""
        sig = f"{self.name}|{self.is_interface}|{self.superclass}|"
        sig += "|".join(sorted(self.interfaces))
        sig += "|".join(sorted(self.public_methods))
        sig += "|".join(sorted(self.public_fields))
        return hashlib.md5(sig.encode()).hexdigest()[:12]


def extract_class_info(jar_path: Path, class_name: str) -> Optional[ClassInfo]:
    """Extract public API information from a class using javap."""
    try:
        result = subprocess.run(
            ['javap', '-public', '-classpath', str(jar_path), class_name],
            capture_output=True,
            text=True,
            timeout=10
        )
        if result.returncode != 0:
            return None

        output = result.stdout
        info = ClassInfo(name=class_name)

        # Parse class declaration
        class_match = re.search(
            r'(public\s+)?(abstract\s+)?(interface|class|enum)\s+\S+',
            output
        )
        if class_match:
            info.is_interface = 'interface' in class_match.group(0)
            info.is_abstract = 'abstract' in (class_match.group(0) or '')
            info.is_enum = 'enum' in class_match.group(0)

        # Parse extends
        extends_match = re.search(r'extends\s+(\S+)', output)
        if extends_match:
            info.superclass = extends_match.group(1)

        # Parse implements
        implements_match = re.search(r'implements\s+([^{]+)', output)
        if implements_match:
            info.interfaces = [i.strip() for i in implements_match.group(1).split(',')]

        # Parse public methods (simplified)
        for line in output.split('\n'):
            line = line.strip()
            if line.startswith('public') and '(' in line and ')' in line:
                # Extract method signature
                method_sig = re.sub(r'\s+', ' ', line.rstrip(';'))
                info.public_methods.append(method_sig)
            elif line.startswith('public') and '(' not in line and ';' in line:
                # Public field
                info.public_fields.append(line.rstrip(';'))

        return info
    except Exception as e:
        return None


def list_classes_in_jar(jar_path: Path) -> list[str]:
    """List all class files in a JAR."""
    classes = []
    try:
        with zipfile.ZipFile(jar_path, 'r') as zf:
            for name in zf.namelist():
                if name.endswith('.class') and not name.startswith('META-INF/'):
                    # Convert path to class name
                    class_name = name[:-6].replace('/', '.')
                    # Skip inner classes for now
                    if '$' not in class_name:
                        classes.append(class_name)
    except Exception:
        pass
    return sorted(classes)


def analyze_jar(jar_path: Path) -> dict[str, ClassInfo]:
    """Analyze all public APIs in a JAR file."""
    apis = {}
    classes = list_classes_in_jar(jar_path)

    for class_name in classes:
        info = extract_class_info(jar_path, class_name)
        if info:
            apis[class_name] = info

    return apis


@dataclass
class ApiChange:
    """Represents a single API change."""
    change_type: str  # 'major', 'minor', 'patch'
    category: str     # 'interface', 'class', 'method', 'field'
    description: str
    class_name: str


def compare_apis(old_apis: dict[str, ClassInfo], new_apis: dict[str, ClassInfo]) -> list[ApiChange]:
    """Compare two API snapshots and return list of changes."""
    changes = []

    old_classes = set(old_apis.keys())
    new_classes = set(new_apis.keys())

    # Removed classes = MAJOR (breaking change)
    for removed in old_classes - new_classes:
        old_info = old_apis[removed]
        change_type = 'major' if old_info.is_interface else 'major'
        changes.append(ApiChange(
            change_type=change_type,
            category='interface' if old_info.is_interface else 'class',
            description=f"Removed: {removed}",
            class_name=removed
        ))

    # Added classes = MINOR (backward compatible addition)
    for added in new_classes - old_classes:
        new_info = new_apis[added]
        changes.append(ApiChange(
            change_type='minor',
            category='interface' if new_info.is_interface else 'class',
            description=f"Added: {added}",
            class_name=added
        ))

    # Changed classes
    for class_name in old_classes & new_classes:
        old_info = old_apis[class_name]
        new_info = new_apis[class_name]

        # Interface changes are always MAJOR
        if old_info.is_interface or new_info.is_interface:
            old_methods = set(old_info.public_methods)
            new_methods = set(new_info.public_methods)

            # Removed methods from interface = MAJOR
            for removed in old_methods - new_methods:
                changes.append(ApiChange(
                    change_type='major',
                    category='interface',
                    description=f"Interface method removed: {removed}",
                    class_name=class_name
                ))

            # Added methods to interface = MAJOR (breaks implementors)
            for added in new_methods - old_methods:
                changes.append(ApiChange(
                    change_type='major',
                    category='interface',
                    description=f"Interface method added: {added}",
                    class_name=class_name
                ))
        else:
            # Class changes
            old_methods = set(old_info.public_methods)
            new_methods = set(new_info.public_methods)

            # Removed public methods = MAJOR
            for removed in old_methods - new_methods:
                changes.append(ApiChange(
                    change_type='major',
                    category='method',
                    description=f"Public method removed: {removed}",
                    class_name=class_name
                ))

            # Added public methods = MINOR
            for added in new_methods - old_methods:
                changes.append(ApiChange(
                    change_type='minor',
                    category='method',
                    description=f"Public method added: {added}",
                    class_name=class_name
                ))

        # Check superclass changes = MAJOR
        if old_info.superclass != new_info.superclass:
            changes.append(ApiChange(
                change_type='major',
                category='class',
                description=f"Superclass changed: {old_info.superclass} -> {new_info.superclass}",
                class_name=class_name
            ))

        # Check interface changes = MAJOR (for classes)
        old_interfaces = set(old_info.interfaces)
        new_interfaces = set(new_info.interfaces)

        for removed in old_interfaces - new_interfaces:
            changes.append(ApiChange(
                change_type='major',
                category='class',
                description=f"Interface removed: {removed}",
                class_name=class_name
            ))

    return changes


def find_baseline_jar(bundle_name: str, release_repo: Path) -> Optional[Path]:
    """Find the baseline JAR for a bundle in the release repository."""
    bundle_dir = release_repo / bundle_name
    if not bundle_dir.is_dir():
        return None

    # Find the latest JAR
    jars = list(bundle_dir.glob('*.jar'))
    if not jars:
        return None

    # Sort by version (simple string sort works for semver)
    jars.sort(key=lambda p: p.name, reverse=True)
    return jars[0]


def find_current_jar(bundle_name: str, goss_root: Path) -> Optional[Path]:
    """Find the current built JAR for a bundle."""
    for generated in goss_root.rglob('generated'):
        for jar in generated.glob(f'{bundle_name}*.jar'):
            if jar.is_file():
                return jar
    return None


def get_bundle_name_from_jar(jar_path: Path) -> Optional[str]:
    """Extract Bundle-SymbolicName from JAR manifest."""
    try:
        with zipfile.ZipFile(jar_path, 'r') as zf:
            manifest = zf.read('META-INF/MANIFEST.MF').decode('utf-8')
            for line in manifest.replace('\r\n ', '').replace('\n ', '').split('\n'):
                if line.startswith('Bundle-SymbolicName:'):
                    bsn = line.split(':', 1)[1].strip()
                    if ';' in bsn:
                        bsn = bsn.split(';')[0].strip()
                    return bsn
    except Exception:
        pass
    return None


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Analyze API changes and suggest version bump type',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Version Bump Rules:
  MAJOR (X.0.0): Interface changes, removed public methods, breaking changes
  MINOR (x.Y.0): New public methods on classes, new classes (backward compatible)
  PATCH (x.y.Z): Implementation-only changes, no public API changes

Examples:
  %(prog)s                    # Analyze all bundles
  %(prog)s --bundle pnnl.goss.core.core-api  # Analyze specific bundle
  %(prog)s --verbose          # Show detailed change information
'''
    )

    parser.add_argument('--bundle', '-b', help='Specific bundle to analyze')
    parser.add_argument('--verbose', '-v', action='store_true', help='Show detailed changes')
    parser.add_argument('--baseline', help='Path to baseline repository (default: cnf/releaserepo)')

    args = parser.parse_args()

    script_dir = Path(__file__).parent.resolve()
    gridappsd_root = script_dir.parent

    # Determine baseline repository
    if args.baseline:
        baseline_repo = Path(args.baseline)
    else:
        baseline_repo = gridappsd_root / 'cnf' / 'releaserepo'

    if not baseline_repo.is_dir():
        log_warn(f"Baseline repository not found: {baseline_repo}")
        log_warn("No baseline to compare against. All changes will be considered MINOR.")
        log_warn("Run './gradlew release' to populate the baseline repository.")
        print()

    # Find all current JARs (GridAPPS-D bundles use gov.pnnl.goss.gridappsd prefix)
    current_jars = []
    for generated in gridappsd_root.rglob('generated'):
        for jar in generated.glob('gov.pnnl.goss.gridappsd*.jar'):
            if jar.is_file():
                current_jars.append(jar)

    if not current_jars:
        log_error("No built JARs found. Run './gradlew build' first.")
        return 1

    # Filter to specific bundle if requested
    if args.bundle:
        current_jars = [j for j in current_jars if args.bundle in j.name]
        if not current_jars:
            log_error(f"Bundle not found: {args.bundle}")
            return 1

    print(f"\n{Colors.CYAN}API Change Analysis{Colors.NC}")
    print("=" * 60)

    overall_bump = 'patch'
    all_changes: list[ApiChange] = []

    for current_jar in sorted(current_jars):
        bundle_name = get_bundle_name_from_jar(current_jar)
        if not bundle_name:
            continue

        baseline_jar = find_baseline_jar(bundle_name, baseline_repo)

        print(f"\n{Colors.BLUE}{bundle_name}{Colors.NC}")

        if not baseline_jar:
            print(f"  {Colors.YELLOW}No baseline found{Colors.NC} - treating as new bundle (MINOR)")
            if overall_bump == 'patch':
                overall_bump = 'minor'
            continue

        # Analyze both JARs
        old_apis = analyze_jar(baseline_jar)
        new_apis = analyze_jar(current_jar)

        if not old_apis and not new_apis:
            print(f"  {Colors.YELLOW}Could not analyze APIs{Colors.NC}")
            continue

        # Compare
        changes = compare_apis(old_apis, new_apis)
        all_changes.extend(changes)

        if not changes:
            # Check if implementation changed (hash comparison)
            old_hashes = {k: v.signature_hash() for k, v in old_apis.items()}
            new_hashes = {k: v.signature_hash() for k, v in new_apis.items()}

            if old_hashes == new_hashes:
                print(f"  {Colors.GREEN}No API changes{Colors.NC}")
            else:
                print(f"  {Colors.GREEN}Implementation changes only{Colors.NC} (PATCH)")
        else:
            # Categorize changes
            major_changes = [c for c in changes if c.change_type == 'major']
            minor_changes = [c for c in changes if c.change_type == 'minor']

            if major_changes:
                print(f"  {Colors.RED}MAJOR changes detected:{Colors.NC}")
                overall_bump = 'major'
                if args.verbose:
                    for c in major_changes[:5]:
                        print(f"    - {c.description}")
                    if len(major_changes) > 5:
                        print(f"    ... and {len(major_changes) - 5} more")
                else:
                    print(f"    {len(major_changes)} breaking change(s)")

            if minor_changes:
                print(f"  {Colors.YELLOW}MINOR changes detected:{Colors.NC}")
                if overall_bump == 'patch':
                    overall_bump = 'minor'
                if args.verbose:
                    for c in minor_changes[:5]:
                        print(f"    - {c.description}")
                    if len(minor_changes) > 5:
                        print(f"    ... and {len(minor_changes) - 5} more")
                else:
                    print(f"    {len(minor_changes)} addition(s)")

    # Summary
    print("\n" + "=" * 60)
    print(f"{Colors.CYAN}Recommended Version Bump:{Colors.NC}")

    if overall_bump == 'major':
        print(f"  {Colors.RED}MAJOR{Colors.NC} - Breaking API changes detected")
        print(f"  Run: {Colors.CYAN}make bump-major{Colors.NC}")
    elif overall_bump == 'minor':
        print(f"  {Colors.YELLOW}MINOR{Colors.NC} - New API additions (backward compatible)")
        print(f"  Run: {Colors.CYAN}make bump-minor{Colors.NC}")
    else:
        print(f"  {Colors.GREEN}PATCH{Colors.NC} - Implementation changes only")
        print(f"  Run: {Colors.CYAN}make bump-patch{Colors.NC} or {Colors.CYAN}make next-snapshot{Colors.NC}")

    print()
    return 0


if __name__ == '__main__':
    sys.exit(main())

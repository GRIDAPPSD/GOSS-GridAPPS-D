#!/bin/bash
#
# Full Clean Script for GridAPPS-D
# Removes all cached bundles, class files, and build artifacts
#
# Usage: ./full-clean.sh [--include-m2]
#   --include-m2  Also remove ~/.m2/repository (Maven cache)

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLEAN_M2=false

# Parse arguments
for arg in "$@"; do
    case $arg in
        --include-m2)
            CLEAN_M2=true
            ;;
    esac
done

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}GridAPPS-D Full Clean${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

cd "$SCRIPT_DIR"

# Clean BND cache
log_info "Removing BND cache (cnf/cache)..."
rm -rf cnf/cache
rm -rf cnf/Local/*
rm -rf cnf/buildrepo/*

# Clean generated directories in all subprojects
log_info "Removing generated directories..."
find . -type d -name "generated" -exec rm -rf {} + 2>/dev/null || true

# Clean bin directories (compiled classes)
log_info "Removing bin directories..."
find . -type d -name "bin" -not -path "./.git/*" -exec rm -rf {} + 2>/dev/null || true

# Clean build directories
log_info "Removing build directories..."
find . -type d -name "build" -not -path "./.git/*" -exec rm -rf {} + 2>/dev/null || true

# Clean .gradle cache
log_info "Removing .gradle cache..."
rm -rf .gradle

# Clean class files (in case any are outside standard directories)
log_info "Removing stray .class files..."
find . -name "*.class" -not -path "./.git/*" -delete 2>/dev/null || true

# Clean Maven .m2 cache if requested
if [ "$CLEAN_M2" = true ]; then
    log_warn "Removing Maven cache (~/.m2/repository)..."
    rm -rf ~/.m2/repository
else
    log_info "Skipping Maven cache (use --include-m2 to remove ~/.m2/repository)"
fi

# Run gradle clean
log_info "Running gradle clean..."
./gradlew clean --quiet 2>/dev/null || true

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Full Clean Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
log_info "Run './gradlew build' to rebuild"

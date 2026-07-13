#!/usr/bin/env bash
# Build the Lambda deployment package: install dependencies and copy source
# code into backend/build/. Run this before `terraform apply`.
#
# Dependencies with native extensions (bcrypt) must be built for the Lambda
# runtime. This script targets manylinux2014 / Python 3.12.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${HERE}/build"

echo "Cleaning build directory..."
rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"

echo "Installing dependencies for Python 3.12 (manylinux2014)..."
pip install \
  --platform manylinux2014_x86_64 \
  --implementation cp \
  --python-version 3.12 \
  --only-binary=:all: \
  --target "${BUILD_DIR}" \
  -r "${HERE}/requirements.txt"

echo "Copying source code..."
cp -r "${HERE}/shared" "${BUILD_DIR}/shared"
cp -r "${HERE}/functions" "${BUILD_DIR}/functions"

# Remove caches to keep the package small.
find "${BUILD_DIR}" -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find "${BUILD_DIR}" -type d -name "*.dist-info" -prune -o -name "*.pyc" -exec rm -f {} + 2>/dev/null || true

echo "Build complete: ${BUILD_DIR}"

#!/usr/bin/env bash
# Install PlanWise Git Flow hooks for this clone. Run once after cloning:
#   bash scripts/install-hooks.sh
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$ROOT_DIR"

chmod +x scripts/git-hooks/*
git config core.hooksPath scripts/git-hooks

echo "✔ Đã cài git hooks (core.hooksPath = $(git config core.hooksPath))"
echo "  - pre-commit : chặn commit lên main/develop, chặn .env / key / build output"
echo "  - commit-msg : bắt buộc Conventional Commits"
echo "  - pre-push   : chặn push lên main/develop, kiểm tra tên nhánh"

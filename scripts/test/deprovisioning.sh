#!/usr/bin/env bash
# =============================================================================
# deprovisioning.sh — Full teardown for Awwotations (test environment)
#
# Usage:
#   cd scripts/test
#   ./deprovisioning.sh
#
# WARNING: This will permanently delete all AWS resources (DynamoDB data,
#          S3 objects, Lambda functions, CloudFront distribution, etc.).
#          There is a confirmation prompt before anything is destroyed.
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INFRA_DIR="${ROOT_DIR}/infrastructure"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Safety confirmation
# ---------------------------------------------------------------------------
echo ""
echo "  ╔══════════════════════════════════════════════════════╗"
echo "  ║          ⚠  AWWOTATIONS DEPROVISIONING  ⚠            ║"
echo "  ║                                                      ║"
echo "  ║  This will destroy ALL AWS resources:                ║"
echo "  ║    • Lambda functions  • API Gateway                 ║"
echo "  ║    • DynamoDB table    • S3 bucket (+ all objects)   ║"
echo "  ║    • CloudFront dist   • SSM Parameter               ║"
echo "  ║    • IAM roles         • CloudWatch log groups       ║"
echo "  ║                                                      ║"
echo "  ║  DynamoDB data will be PERMANENTLY LOST.             ║"
echo "  ╚══════════════════════════════════════════════════════╝"
echo ""
read -r -p "  Type YES to confirm: " CONFIRM
[[ "${CONFIRM}" == "YES" ]] || { log "Aborted."; exit 0; }
echo ""

# ---------------------------------------------------------------------------
# 1. Retrieve outputs before destroying infra
# ---------------------------------------------------------------------------
log "=== Collecting Terraform outputs ==="
cd "${INFRA_DIR}"
[[ -f "terraform.tfvars" ]] || fail "infrastructure/terraform.tfvars not found."
terraform init -input=false -reconfigure 2>/dev/null || true

BUCKET=""
if terraform output -raw frontend_bucket_name &>/dev/null; then
  BUCKET=$(terraform output -raw frontend_bucket_name)
fi

# ---------------------------------------------------------------------------
# 2. Empty S3 bucket (terraform destroy fails if bucket is not empty)
# ---------------------------------------------------------------------------
if [[ -n "${BUCKET}" ]]; then
  log "=== Step 1/2 — Emptying S3 bucket: ${BUCKET} ==="
  aws s3 rm "s3://${BUCKET}" --recursive || log "  Bucket already empty or does not exist."
else
  log "  Could not read frontend_bucket_name from Terraform outputs — skipping S3 cleanup."
fi

# ---------------------------------------------------------------------------
# 3. Terraform destroy
# ---------------------------------------------------------------------------
log "=== Step 2/2 — Running terraform destroy ==="
terraform destroy -input=false -auto-approve
cd "${ROOT_DIR}"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
log "=== Deprovisioning complete. All resources have been destroyed. ==="

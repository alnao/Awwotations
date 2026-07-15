#!/usr/bin/env bash
# =============================================================================
# deploy.sh — Full deployment for Awwotations (test environment)
#
# Usage:
#   cd scripts/test
#   ./deploy.sh
#
# Prerequisites:
#   - AWS CLI configured (aws configure / env vars / SSO)
#   - Terraform >= 1.5
#   - Python 3.12 with venv support
#   - Node.js >= 18 + npm
#   - infrastructure/terraform.tfvars must exist (copy from *.example and fill in)
#
# The .env file (VITE_API_BASE_URL) is generated in this folder (scripts/test/)
# and then copied to frontend/ before the React build.
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Paths (resolved relative to this script, so it can be called from anywhere)
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INFRA_DIR="${ROOT_DIR}/infrastructure"
BACKEND_DIR="${ROOT_DIR}/backend"
FRONTEND_DIR="${ROOT_DIR}/frontend"
ENV_FILE="${SCRIPT_DIR}/.env"          # stored here, in scripts/test/

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 1. Build Lambda package
# ---------------------------------------------------------------------------
log "=== Step 1/5 — Building Lambda package ==="
cd "${BACKEND_DIR}"
[[ -x "./build.sh" ]] || fail "backend/build.sh not found or not executable."
./build.sh
cd "${ROOT_DIR}"

# ---------------------------------------------------------------------------
# 2. Terraform init + apply
# ---------------------------------------------------------------------------
log "=== Step 2/5 — Terraform init & apply ==="
cd "${INFRA_DIR}"
[[ -f "terraform.tfvars" ]] || fail "infrastructure/terraform.tfvars not found. Copy terraform.tfvars.example and fill in your values."
terraform init -input=false
terraform apply -input=false -auto-approve
cd "${ROOT_DIR}"

# ---------------------------------------------------------------------------
# 3. Generate .env (saved in scripts/test/)
# ---------------------------------------------------------------------------
log "=== Step 3/5 — Generating .env ==="
API_URL=$(terraform -chdir="${INFRA_DIR}" output -raw api_endpoint)
BUCKET=$(terraform -chdir="${INFRA_DIR}" output -raw frontend_bucket_name)
CF_ID=$(terraform -chdir="${INFRA_DIR}" output -raw frontend_cloudfront_distribution_id)

cat > "${ENV_FILE}" <<EOF
VITE_API_BASE_URL=${API_URL}
EOF

log "  .env written to: ${ENV_FILE}"
log "  VITE_API_BASE_URL=${API_URL}"

# Copy .env to frontend/ so Vite can read it at build time
cp "${ENV_FILE}" "${FRONTEND_DIR}/.env"
log "  .env copied to:  ${FRONTEND_DIR}/.env"

# ---------------------------------------------------------------------------
# 4. Build React SPA and sync to S3
# ---------------------------------------------------------------------------
log "=== Step 4/5 — Building and deploying frontend ==="
cd "${FRONTEND_DIR}"
npm install --silent
npm run build
aws s3 sync dist/ "s3://${BUCKET}" --delete
cd "${ROOT_DIR}"

# ---------------------------------------------------------------------------
# 5. CloudFront invalidation
# ---------------------------------------------------------------------------
log "=== Step 5/5 — Invalidating CloudFront cache ==="
INVALIDATION_ID=$(aws cloudfront create-invalidation \
  --distribution-id "${CF_ID}" \
  --paths "/*" \
  --query 'Invalidation.Id' \
  --output text)
log "  Invalidation created: ${INVALIDATION_ID}"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
CF_DOMAIN=$(terraform -chdir="${INFRA_DIR}" output -raw frontend_cloudfront_domain)
log "=== Deployment complete ==="
log "  Frontend URL : https://${CF_DOMAIN}"
log "  API endpoint : ${API_URL}"

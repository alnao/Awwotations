#!/usr/bin/env bash
# =============================================================================
# manage_ip.sh — Manage allowed IP addresses in the DynamoDB table
#
# Usage:
#   cd scripts/test
#   ./manage_ip.sh add       # Adds your current public IP to the allowlist
#   ./manage_ip.sh remove    # Removes your current public IP from the allowlist
#   ./manage_ip.sh list      # Lists all currently allowed IP addresses
#   ./manage_ip.sh add <ip>  # Adds a specific IP address
#   ./manage_ip.sh remove <ip> # Removes a specific IP address
# =============================================================================

set -euo pipefail

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INFRA_DIR="${ROOT_DIR}/infrastructure"

# Helpers
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

# Check dependencies
command -v aws >/dev/null 2>&1 || fail "AWS CLI is required but not installed."
command -v terraform >/dev/null 2>&1 || fail "Terraform is required but not installed."

# Retrieve DynamoDB table name and region from Terraform
log "Retrieving table name from Terraform..."
cd "${INFRA_DIR}"
TABLE_NAME=$(terraform output -raw dynamodb_table_name 2>/dev/null || true)
if [[ -z "${TABLE_NAME}" ]]; then
  fail "Could not find 'dynamodb_table_name' output in Terraform. Make sure you have run 'terraform apply'."
fi
log "Using DynamoDB table: ${TABLE_NAME}"

# Determine action
ACTION=${1:-"add"}
SPECIFIED_IP=${2:-""}

# Get IP address to use
if [[ -n "${SPECIFIED_IP}" ]]; then
  IP_ADDRESS="${SPECIFIED_IP}"
else
  log "Detecting current public IP..."
  # Try multiple public IP services in order of reliability
  IP_ADDRESS=$(curl -s --max-time 5 https://checkip.amazonaws.com | tr -d '[:space:]' || true)
  if [[ -z "${IP_ADDRESS}" ]]; then
    IP_ADDRESS=$(curl -s --max-time 5 https://ipinfo.io/ip | tr -d '[:space:]' || true)
  fi
  if [[ -z "${IP_ADDRESS}" ]]; then
    IP_ADDRESS=$(curl -s --max-time 5 https://ifconfig.me | tr -d '[:space:]' || true)
  fi
  if [[ -z "${IP_ADDRESS}" ]]; then
    fail "Could not detect public IP. Please specify it manually: ./manage_ip.sh ${ACTION} <ip>"
  fi
fi

# Execute action
case "${ACTION}" in
  add)
    log "Adding IP ${IP_ADDRESS} to the allowlist..."
    aws dynamodb update-item \
      --table-name "${TABLE_NAME}" \
      --key '{"PK": {"S": "CONFIG#IPLIST"}, "SK": {"S": "META"}}' \
      --update-expression "ADD allowedIps :ip" \
      --expression-attribute-values '{":ip": {"SS": ["'"${IP_ADDRESS}"'"]}}'
    log "Successfully added ${IP_ADDRESS} to the allowed IP list."
    ;;

  remove)
    log "Removing IP ${IP_ADDRESS} from the allowlist..."
    aws dynamodb update-item \
      --table-name "${TABLE_NAME}" \
      --key '{"PK": {"S": "CONFIG#IPLIST"}, "SK": {"S": "META"}}' \
      --update-expression "DELETE allowedIps :ip" \
      --expression-attribute-values '{":ip": {"SS": ["'"${IP_ADDRESS}"'"]}}'
    log "Successfully removed ${IP_ADDRESS} from the allowed IP list."
    ;;

  list)
    log "Listing currently allowed IP addresses..."
    RESULT=$(aws dynamodb get-item \
      --table-name "${TABLE_NAME}" \
      --key '{"PK": {"S": "CONFIG#IPLIST"}, "SK": {"S": "META"}}' \
      --projection-expression "allowedIps" \
      --output json || true)
    
    # Parse and display
    if [[ -n "${RESULT}" ]] && echo "${RESULT}" | grep -q "allowedIps"; then
      echo "${RESULT}" | grep -oE '"[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+"' | tr -d '"' | sort
    else
      echo "No IP addresses are currently in the allowlist. Access to protected API endpoints is blocked."
    fi
    ;;

  *)
    fail "Unknown action: ${ACTION}. Supported actions: add, remove, list"
    ;;
esac

"""JWT Lambda Authorizer for API Gateway HTTP API (payload format 2.0).

Returns a simple response ({"isAuthorized": bool}) plus a context object with
the authenticated userId and email, which downstream Lambdas read via
response.get_user_id().
"""
from __future__ import annotations

from shared import auth, db


def handler(event: dict, context: object) -> dict:
    # 1. Verify Client Source IP
    source_ip = event.get("requestContext", {}).get("http", {}).get("sourceIp")
    if not source_ip:
        print("Authorization denied: missing source IP in request context.")
        return {"isAuthorized": False, "context": {}}

    allowed_ips = db.get_allowed_ips()
    if source_ip not in allowed_ips:
        print(f"Authorization denied: IP {source_ip} is not allowed.")
        return {"isAuthorized": False, "context": {}}

    # 2. Verify JWT Bearer Token
    headers = event.get("headers", {}) or {}
    try:
        token = auth.extract_bearer_token(headers)
        payload = auth.decode_token(token, expected_type="access")
    except auth.AuthError as exc:
        print(f"Authorization denied: {exc}")
        return {"isAuthorized": False, "context": {}}

    return {
        "isAuthorized": True,
        "context": {
            "userId": payload["sub"],
            "email": payload.get("email", ""),
        },
    }

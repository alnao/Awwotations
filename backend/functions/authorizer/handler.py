"""JWT Lambda Authorizer for API Gateway HTTP API (payload format 2.0).

Returns a simple response ({"isAuthorized": bool}) plus a context object with
the authenticated userId and email, which downstream Lambdas read via
response.get_user_id().
"""
from __future__ import annotations

from shared import auth


def handler(event: dict, context: object) -> dict:
    headers = event.get("headers", {}) or {}
    try:
        token = auth.extract_bearer_token(headers)
        payload = auth.decode_token(token, expected_type="access")
    except auth.AuthError:
        return {"isAuthorized": False, "context": {}}

    return {
        "isAuthorized": True,
        "context": {
            "userId": payload["sub"],
            "email": payload.get("email", ""),
        },
    }

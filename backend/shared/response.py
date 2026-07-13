"""HTTP response helpers for API Gateway (HTTP API / payload format 2.0)."""
from __future__ import annotations

import json
from typing import Any

from pydantic import ValidationError

DEFAULT_HEADERS = {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type,Authorization",
    "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
}


def _serialize(obj: Any) -> Any:
    """Fallback serializer for objects json cannot handle by default."""
    from decimal import Decimal

    if isinstance(obj, Decimal):
        return int(obj) if obj % 1 == 0 else float(obj)
    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")


def response(status_code: int, body: Any) -> dict:
    return {
        "statusCode": status_code,
        "headers": DEFAULT_HEADERS,
        "body": json.dumps(body, default=_serialize),
    }


def ok(body: Any) -> dict:
    return response(200, body)


def created(body: Any) -> dict:
    return response(201, body)


def no_content() -> dict:
    return {"statusCode": 204, "headers": DEFAULT_HEADERS, "body": ""}


def error(status_code: int, message: str, details: Any = None) -> dict:
    payload: dict[str, Any] = {"error": message}
    if details is not None:
        payload["details"] = details
    return response(status_code, payload)


def bad_request(message: str = "Bad request", details: Any = None) -> dict:
    return error(400, message, details)


def unauthorized(message: str = "Unauthorized") -> dict:
    return error(401, message)


def forbidden(message: str = "Forbidden") -> dict:
    return error(403, message)


def not_found(message: str = "Not found") -> dict:
    return error(404, message)


def conflict(message: str = "Conflict") -> dict:
    return error(409, message)


def internal_error(message: str = "Internal server error") -> dict:
    return error(500, message)


def validation_error(exc: ValidationError) -> dict:
    """Format a Pydantic ValidationError into a 400 response."""
    details = [
        {"field": ".".join(str(p) for p in e["loc"]), "message": e["msg"]}
        for e in exc.errors()
    ]
    return bad_request("Validation failed", details)


def parse_body(event: dict) -> dict:
    """Parse the JSON body of an API Gateway event, handling base64 encoding."""
    import base64

    raw = event.get("body") or "{}"
    if event.get("isBase64Encoded"):
        raw = base64.b64decode(raw).decode("utf-8")
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise ValueError("Request body must be valid JSON") from exc
    if not isinstance(parsed, dict):
        raise ValueError("Request body must be a JSON object")
    return parsed


def get_user_id(event: dict) -> str:
    """Extract the authenticated userId injected by the Lambda Authorizer."""
    ctx = (
        event.get("requestContext", {})
        .get("authorizer", {})
        .get("lambda", {})
    )
    user_id = ctx.get("userId")
    if not user_id:
        raise ValueError("Missing authenticated user context")
    return user_id


def path_param(event: dict, name: str) -> str:
    value = (event.get("pathParameters") or {}).get(name)
    if not value:
        raise ValueError(f"Missing path parameter: {name}")
    return value

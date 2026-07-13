"""Access token renewal Lambda handler."""
from __future__ import annotations

from pydantic import ValidationError

from shared import auth, db, response
from shared.models import RefreshRequest


def handler(event: dict, context: object) -> dict:
    try:
        body = response.parse_body(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    try:
        req = RefreshRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    try:
        payload = auth.decode_token(req.refreshToken, expected_type="refresh")
    except auth.AuthError as exc:
        return response.unauthorized(str(exc))

    user = db.get_user(payload["sub"])
    if not user:
        return response.unauthorized("User no longer exists")

    access = auth.create_access_token(user["userId"], user["email"])
    return response.ok({"accessToken": access, "tokenType": "Bearer"})

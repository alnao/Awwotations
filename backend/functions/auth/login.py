"""Login Lambda handler. Emits an access token and a refresh token."""
from __future__ import annotations

from pydantic import ValidationError

from shared import auth, db, response
from shared.models import LoginRequest


def handler(event: dict, context: object) -> dict:
    try:
        body = response.parse_body(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    try:
        req = LoginRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    user = db.get_user_by_email(req.email)
    if not user or not auth.verify_password(req.password, user["passwordHash"]):
        return response.unauthorized("Invalid email or password")

    access = auth.create_access_token(user["userId"], user["email"])
    refresh = auth.create_refresh_token(user["userId"], user["email"])

    return response.ok(
        {
            "accessToken": access,
            "refreshToken": refresh,
            "tokenType": "Bearer",
            "user": {
                "userId": user["userId"],
                "email": user["email"],
                "name": user["name"],
            },
        }
    )

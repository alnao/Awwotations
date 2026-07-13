"""User registration Lambda handler."""
from __future__ import annotations

import uuid

from pydantic import ValidationError

from shared import auth, db, response
from shared.models import RegisterRequest, utcnow_iso


def handler(event: dict, context: object) -> dict:
    try:
        body = response.parse_body(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    try:
        req = RegisterRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    if db.get_user_by_email(req.email):
        return response.conflict("An account with this email already exists")

    user_id = str(uuid.uuid4())
    now = utcnow_iso()
    item = {
        "PK": db.user_pk(user_id),
        "SK": "PROFILE",
        "GSI1PK": db.email_gsi1pk(req.email),
        "GSI1SK": db.user_pk(user_id),
        "entityType": "USER",
        "userId": user_id,
        "email": req.email.lower(),
        "passwordHash": auth.hash_password(req.password),
        "name": req.name,
        "createdAt": now,
    }

    try:
        db.put_user(item)
    except db.get_table().meta.client.exceptions.ConditionalCheckFailedException:
        return response.conflict("An account with this email already exists")

    return response.created(
        {"userId": user_id, "email": req.email.lower(), "name": req.name}
    )

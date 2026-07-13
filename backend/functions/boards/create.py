"""Create board Lambda handler."""
from __future__ import annotations

import uuid

from pydantic import ValidationError

from shared import db, response
from shared.models import BoardCreateRequest, utcnow_iso


def handler(event: dict, context: object) -> dict:
    try:
        user_id = response.get_user_id(event)
        body = response.parse_body(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    try:
        req = BoardCreateRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    board_id = str(uuid.uuid4())
    now = utcnow_iso()
    item = {
        "PK": db.board_pk(board_id),
        "SK": "META",
        "GSI1PK": db.user_pk(user_id),
        "GSI1SK": db.board_pk(board_id),
        "entityType": "BOARD",
        "boardId": board_id,
        "ownerId": user_id,
        "title": req.title,
        "color": req.color,
        "createdAt": now,
        "updatedAt": now,
    }
    db.put_board(item)

    return response.created(
        {
            "boardId": board_id,
            "ownerId": user_id,
            "title": req.title,
            "color": req.color,
            "createdAt": now,
            "updatedAt": now,
        }
    )

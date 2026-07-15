"""Update board Lambda handler. Only the board owner may update."""
from __future__ import annotations

from pydantic import ValidationError

from shared import db, response
from shared.models import BoardUpdateRequest, utcnow_iso


def handler(event: dict, context: object) -> dict:
    try:
        user_id = response.get_user_id(event)
        board_id = response.path_param(event, "boardId")
        body = response.parse_body(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    try:
        req = BoardUpdateRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    board = db.get_board(board_id)
    if not board:
        return response.not_found("Board not found")
    if board["ownerId"] != user_id:
        return response.forbidden("Only the board owner can update this board")

    updates = {"updatedAt": utcnow_iso()}
    if req.title is not None:
        updates["title"] = req.title
    if req.color is not None:
        updates["color"] = req.color
    if req.order is not None:
        updates["order"] = req.order
    if req.favorite is not None:
        updates["favorite"] = req.favorite

    updated = db.update_board(board_id, updates)
    return response.ok(
        {
            "boardId": updated["boardId"],
            "ownerId": updated["ownerId"],
            "title": updated["title"],
            "color": updated["color"],
            "order": updated.get("order", 0),
            "favorite": updated.get("favorite", False),
            "createdAt": updated["createdAt"],
            "updatedAt": updated["updatedAt"],
        }
    )

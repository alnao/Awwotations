"""List notes of a board Lambda handler."""
from __future__ import annotations

from shared import db, response

_PUBLIC_FIELDS = (
    "noteId boardId title text textType userDateTime links iconMain "
    "iconSecondary color posX posY width height status pinned favorite "
    "createdAt updatedAt statusChangedAt"
).split()


def handler(event: dict, context: object) -> dict:
    try:
        user_id = response.get_user_id(event)
        board_id = response.path_param(event, "boardId")
    except ValueError as exc:
        return response.bad_request(str(exc))

    board = db.get_board(board_id)
    if not board:
        return response.not_found("Board not found")
    if board["ownerId"] != user_id:
        return response.forbidden("You do not have access to this board")

    notes = db.list_notes_by_board(board_id)
    public = [{k: n.get(k) for k in _PUBLIC_FIELDS} for n in notes]
    return response.ok({"notes": public, "count": len(public)})

"""Delete board Lambda handler. Cascades to all notes. Owner only."""
from __future__ import annotations

from shared import db, response


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
        return response.forbidden("Only the board owner can delete this board")

    db.delete_board(board_id)
    return response.no_content()

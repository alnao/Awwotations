"""Delete note Lambda handler."""
from __future__ import annotations

from shared import db, response


def handler(event: dict, context: object) -> dict:
    try:
        user_id = response.get_user_id(event)
        board_id = response.path_param(event, "boardId")
        note_id = response.path_param(event, "noteId")
    except ValueError as exc:
        return response.bad_request(str(exc))

    board = db.get_board(board_id)
    if not board:
        return response.not_found("Board not found")
    if board["ownerId"] != user_id:
        return response.forbidden("You do not have access to this board")

    note = db.get_note(board_id, note_id)
    if not note:
        return response.not_found("Note not found")

    db.delete_note(board_id, note_id)
    return response.no_content()

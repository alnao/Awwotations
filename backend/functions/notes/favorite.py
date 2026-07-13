"""Toggle favorite Lambda handler.

Favorite is a boolean toggle available for all statuses. Any user with access
to the board may toggle it (here: the board owner, since sharing is owner-based).
"""
from __future__ import annotations

from shared import db, response
from shared.models import utcnow_iso

_PUBLIC_FIELDS = (
    "noteId boardId title text textType userDateTime links iconMain "
    "iconSecondary color posX posY width height status pinned favorite "
    "createdAt updatedAt statusChangedAt"
).split()


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
    # Any user with access to the board may toggle favorite.
    if board["ownerId"] != user_id:
        return response.forbidden("You do not have access to this board")

    note = db.get_note(board_id, note_id)
    if not note:
        return response.not_found("Note not found")

    new_favorite = not bool(note.get("favorite"))
    updated = db.update_note(
        board_id, note_id, {"favorite": new_favorite, "updatedAt": utcnow_iso()}
    )
    return response.ok({k: updated.get(k) for k in _PUBLIC_FIELDS})

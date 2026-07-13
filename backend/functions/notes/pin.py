"""Toggle pin Lambda handler.

Only the board owner may pin/unpin. A board can have at most one pinned note;
pinning a note automatically unpins the previously pinned one. Available for
all statuses.
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
    if board["ownerId"] != user_id:
        return response.forbidden("Only the board owner can pin or unpin notes")

    note = db.get_note(board_id, note_id)
    if not note:
        return response.not_found("Note not found")

    now = utcnow_iso()
    new_pinned = not bool(note.get("pinned"))

    if new_pinned:
        # Unpin any other currently pinned note on this board.
        for other in db.list_notes_by_board(board_id):
            if other["noteId"] != note_id and other.get("pinned"):
                db.update_note(
                    board_id, other["noteId"], {"pinned": False, "updatedAt": now}
                )

    updated = db.update_note(
        board_id, note_id, {"pinned": new_pinned, "updatedAt": now}
    )
    return response.ok({k: updated.get(k) for k in _PUBLIC_FIELDS})

"""Update note content Lambda handler.

Content editing is blocked when the note status is DONE or REJECTED; in that
case the handler returns HTTP 403. To edit such a note the user must first move
it back to MODIFIED via the dedicated status change API.
"""
from __future__ import annotations

from pydantic import ValidationError

from shared import db, response
from shared.models import NoteStatus, NoteUpdateRequest, is_content_locked, utcnow_iso

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
        body = response.parse_body(event)
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

    current_status = NoteStatus(note["status"])
    if is_content_locked(current_status):
        return response.forbidden(
            "Note content is locked in status "
            f"{current_status.value}. Move it back to MODIFIED to edit."
        )

    try:
        req = NoteUpdateRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    updates = {
        "title": req.title,
        "text": req.text,
        "textType": req.textType,
        "userDateTime": req.userDateTime,
        "links": [link.model_dump() for link in req.links],
        "iconMain": req.iconMain,
        "iconSecondary": req.iconSecondary,
        "color": req.color,
        "posX": req.posX,
        "posY": req.posY,
        "width": req.width,
        "height": req.height,
        "updatedAt": utcnow_iso(),
    }

    updated = db.update_note(board_id, note_id, updates)
    return response.ok({k: updated.get(k) for k in _PUBLIC_FIELDS})

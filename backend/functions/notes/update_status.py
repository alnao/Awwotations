"""Dedicated note status change Lambda handler.

Validates the requested transition against the centralized transition matrix
in shared/models.py and updates statusChangedAt on every successful change.
"""
from __future__ import annotations

from pydantic import ValidationError

from shared import db, response
from shared.models import (
    NoteStatus,
    StatusUpdateRequest,
    can_transition,
    utcnow_iso,
)

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

    try:
        req = StatusUpdateRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    current = NoteStatus(note["status"])
    target = req.status

    if current == target:
        return response.bad_request(f"Note is already in status {target.value}")

    if not can_transition(current, target):
        return response.bad_request(
            f"Transition {current.value} -> {target.value} is not allowed"
        )

    now = utcnow_iso()
    updated = db.update_note(
        board_id,
        note_id,
        {"status": target.value, "statusChangedAt": now, "updatedAt": now},
    )
    return response.ok({k: updated.get(k) for k in _PUBLIC_FIELDS})

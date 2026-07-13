"""Create note Lambda handler. Initial status is always CREATED."""
from __future__ import annotations

import uuid

from pydantic import ValidationError

from shared import db, response
from shared.models import NoteCreateRequest, NoteStatus, utcnow_iso


def handler(event: dict, context: object) -> dict:
    try:
        user_id = response.get_user_id(event)
        board_id = response.path_param(event, "boardId")
        body = response.parse_body(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    board = db.get_board(board_id)
    if not board:
        return response.not_found("Board not found")
    if board["ownerId"] != user_id:
        return response.forbidden("You do not have access to this board")

    try:
        req = NoteCreateRequest(**body)
    except ValidationError as exc:
        return response.validation_error(exc)

    note_id = str(uuid.uuid4())
    now = utcnow_iso()

    links = [link.model_dump() for link in req.links]

    # If the new note is pinned, unpin the previously pinned note (max 1 per board).
    if req.pinned:
        _unpin_existing(board_id)

    item = {
        "PK": db.board_pk(board_id),
        "SK": db.note_sk(note_id),
        "GSI2PK": db.board_pk(board_id),
        "GSI2SK": db.note_sk(note_id),
        "entityType": "NOTE",
        "noteId": note_id,
        "boardId": board_id,
        "title": req.title,
        "text": req.text,
        "textType": req.textType,
        "userDateTime": req.userDateTime,
        "links": links,
        "iconMain": req.iconMain,
        "iconSecondary": req.iconSecondary,
        "color": req.color,
        "posX": req.posX,
        "posY": req.posY,
        "width": req.width,
        "height": req.height,
        "status": NoteStatus.CREATED.value,
        "pinned": req.pinned,
        "favorite": req.favorite,
        "createdAt": now,
        "updatedAt": now,
        "statusChangedAt": now,
    }
    db.put_note(item)

    return response.created(_public(item))


def _unpin_existing(board_id: str) -> None:
    for note in db.list_notes_by_board(board_id):
        if note.get("pinned"):
            db.update_note(board_id, note["noteId"], {"pinned": False})


def _public(item: dict) -> dict:
    fields = (
        "noteId boardId title text textType userDateTime links iconMain "
        "iconSecondary color posX posY width height status pinned favorite "
        "createdAt updatedAt statusChangedAt"
    ).split()
    return {k: item.get(k) for k in fields}

"""List boards owned by the authenticated user."""
from __future__ import annotations

from shared import db, response


def handler(event: dict, context: object) -> dict:
    try:
        user_id = response.get_user_id(event)
    except ValueError as exc:
        return response.bad_request(str(exc))

    boards = db.list_boards_by_owner(user_id)
    public = [
        {
            "boardId": b["boardId"],
            "ownerId": b["ownerId"],
            "title": b["title"],
            "color": b["color"],
            "order": b.get("order", 0),
            "favorite": b.get("favorite", False),
            "createdAt": b["createdAt"],
            "updatedAt": b["updatedAt"],
        }
        for b in boards
    ]
    return response.ok({"boards": public, "count": len(public)})

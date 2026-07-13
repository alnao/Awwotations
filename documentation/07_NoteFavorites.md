# 07 — Favorite Notes

Favorites let users flag notes they care about, independently of pinning.

## Rules

- Simple **boolean toggle** on the note (`favorite`).
- Any user with **access to the board** can toggle favorite.
- Available for **all statuses**.

## API

```
PATCH /boards/{boardId}/notes/{noteId}/favorite
```

Toggle with no body; flips the `favorite` flag in
`functions/notes/favorite.py`.

## Access model

In this version access is owner-based, so effectively the board owner toggles
favorite. The handler is intentionally written around "any user with board
access" so that when board sharing is introduced, favorites work for all members
without code changes (unlike pin, which is owner-only).

## Responses

- `200` — returns the full updated note
- `403` — caller has no access to the board
- `404` — board or note not found

## Notes

- `favorite` is independent of `pinned` and `status`.
- `updatedAt` is refreshed on every toggle.

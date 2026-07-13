# 06 — Pinned Notes

Pinning highlights a single important note per board.

## Rules

- **Maximum 1 pinned note per board.**
- Pinning a note automatically **unpins** the previously pinned note.
- Only the **board owner** can pin or unpin.
- Available for **all statuses**.

## API

```
PATCH /boards/{boardId}/notes/{noteId}/pin
```

The endpoint is a **toggle** with no body. It flips the note's `pinned` flag.
When toggling a note to pinned, `functions/notes/pin.py` iterates the board's
notes and unpins any other currently-pinned note before setting this one.

## Responses

- `200` — returns the full updated note
- `403` — caller is not the board owner
- `404` — board or note not found

## Notes

- Unpinning simply sets `pinned = false`; it does not affect other notes.
- Pinning is independent from status: a `DONE` note can still be pinned.
- `updatedAt` is refreshed on every pin/unpin. `statusChangedAt` is not touched.

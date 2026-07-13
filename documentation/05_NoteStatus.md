# 05 — Note Status

Every note has a lifecycle `status`. All status logic is centralized in
`shared/models.py` using the `NoteStatus` enum, the `STATUS_TRANSITIONS` matrix,
and the `LOCKED_STATUSES` set.

## Statuses

`DREAM`, `CREATED`, `TODO`, `MODIFIED`, `DONE`, `REJECTED`.

A newly created note always starts in **CREATED**.

## Transition matrix

| From \ To    | DREAM | CREATED | TODO | MODIFIED | DONE | REJECTED |
| ------------ | :---: | :-----: | :--: | :------: | :--: | :------: |
| **DREAM**    |   —   |   ✅    |  ❌  |    ✅    |  ❌  |    ❌    |
| **CREATED**  |   ❌  |   —     |  ✅  |    ✅    |  ❌  |    ❌    |
| **TODO**     |   ❌  |   ❌    |  —   |    ✅    |  ❌  |    ❌    |
| **MODIFIED** |   ❌  |   ❌    |  ❌  |    —     |  ✅  |    ✅    |
| **DONE**     |   ❌  |   ❌    |  ❌  |    ✅    |  —   |    ❌    |
| **REJECTED** |   ❌  |   ❌    |  ❌  |    ✅    |  ❌  |    —     |

Any transition not marked ✅ is rejected with `400`.

## Content edit lock

- Editable: `DREAM`, `CREATED`, `TODO`, `MODIFIED`
- Locked: `DONE`, `REJECTED`

Calling `PUT /boards/{boardId}/notes/{noteId}` on a locked note returns `403`.
To unlock, first move it back to `MODIFIED` via the status API.

## Status change API

Status changes are a **separate** API from content updates:

```
PATCH /boards/{boardId}/notes/{noteId}/status
{ "status": "TODO" }
```

Rules enforced by `functions/notes/update_status.py`:

1. The requested transition must be allowed by the matrix (`can_transition`).
2. On success, both `statusChangedAt` and `updatedAt` are set to now.
3. A no-op (same status) returns `400`.

## Responses

- `200` — status changed, returns the full note
- `400` — disallowed transition or same status
- `403` — no access to the board
- `404` — board or note not found

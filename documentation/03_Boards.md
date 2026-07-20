# 03 — Boards

A **board** is a container of notes owned by a single user. Every board
operation requires a valid access token; write operations require the caller to
be the board **owner**.

## Entity

| Field        | Type     | Notes                                        |
| ------------ | -------- | -------------------------------------------- |
| `boardId`    | string   | UUID, generated server-side                  |
| `ownerId`    | string   | userId of the creator                        |
| `title`      | string   | required, 1–256 chars                        |
| `color`      | string   | hex color, e.g. `#ffd966`                    |
| `order`      | integer  | display order, ≥ 0; auto-assigned as max+1   |
| `favorite`   | boolean  | `false` by default                           |
| `orderNotes` | string   | sort order (CREATE_DESC/CREATE_ASC/USER_DATE_DESC/USER_DATE_ASC/TITLE/POS_X/POS_Y), defaults to `POS_X` |
| `createdAt`  | datetime | auto                                         |
| `updatedAt`  | datetime | auto                                         |

## Endpoints

## GET /boards

Lists boards owned by the authenticated user (via GSI1). Returns
`{ boards: [...], count: N }`.

### POST /boards

```json
{ "title": "My board", "color": "#ffd966", "order": 3, "favorite": true, "orderNotes": "POS_X" }
```

Returns `201` with the created board.

- `color` defaults to `#ffd966` and must be a valid hex color.
- `order` is optional: when omitted, the server auto-assigns it as the current
  maximum order among the owner's boards + 1 (first board gets 0).
- `favorite` defaults to `false`.
- `orderNotes` defaults to `POS_X` and must be one of the allowed sort keys.

### PUT /boards/{boardId}

Partial update of `title`, `color`, `order`, `favorite`, and/or `orderNotes`. Owner only.

- `403` if the caller is not the owner
- `404` if the board does not exist

### DELETE /boards/{boardId}

Deletes the board **and all its notes** (cascade). Owner only. Returns `204`.

## Access model

Access is owner-based: only the owner can list-write a board or its notes.
Sharing between users is out of scope for this version; the favorite endpoint is
intentionally written to allow "any user with board access" for future sharing.

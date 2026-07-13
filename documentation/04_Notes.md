# 04 — Notes

A **note** is a post-it that belongs to a board. Notes support rich content,
positioning on a board canvas, icons, links, and a lifecycle (status, pin,
favorite — documented separately).

## Fields

| Field             | Type     | Req | Notes                                                    |
| ----------------- | -------- | --- | -------------------------------------------------------- |
| `noteId`          | string   | ✅  | UUID, server-side                                        |
| `boardId`         | string   | ✅  | board reference                                          |
| `title`           | string   | ✅  | plain text only                                          |
| `text`            | string   | ✅  | long content; may contain code and special characters    |
| `textType`        | enum     | ✅  | `MD`, `HTML`, `TEXT`, or `CODE_XXXX` (see below)          |
| `userDateTime`    | datetime | ✅  | date required, time optional (single field)              |
| `links`           | list     | ⬜  | max 10 items; each `{ url (req), label (opt) }`          |
| `iconMain`        | string   | ⬜  | Font Awesome 5 Free class, e.g. `fas fa-star`            |
| `iconSecondary`   | string   | ⬜  | Font Awesome 5 Free class, e.g. `fas fa-flag`            |
| `color`           | string   | ✅  | hex color                                                |
| `posX`            | number   | ✅  | X position on the board                                  |
| `posY`            | number   | ✅  | Y position on the board                                  |
| `width`           | number   | ✅  | width (> 0)                                              |
| `height`          | number   | ✅  | height (> 0)                                             |
| `status`          | enum     | ✅  | see `05_NoteStatus.md`                                    |
| `pinned`          | boolean  | ✅  | default `false`                                          |
| `favorite`        | boolean  | ✅  | default `false`                                          |
| `createdAt`       | datetime | ✅  | auto                                                     |
| `updatedAt`       | datetime | ✅  | auto                                                     |
| `statusChangedAt` | datetime | ✅  | auto, updated on every status change                     |

## textType

Fixed values: `MD`, `HTML`, `TEXT`. Additionally, any language-tagged code type
matching the pattern `^CODE_[A-Z]+$` is accepted, e.g. `CODE_JAVA`, `CODE_JS`,
`CODE_JSON`, `CODE_YAML`. Validation is centralized in
`shared/models.py::validate_text_type`.

## links

A list of up to 10 objects: `{ "url": "https://...", "label": "optional" }`.
`url` is required, `label` is optional.

## Endpoints

### GET /boards/{boardId}/notes

Lists notes of the board (via GSI2). Returns `{ notes: [...], count: N }`.

### POST /boards/{boardId}/notes

Creates a note. The initial `status` is always **CREATED** regardless of the
payload. If `pinned: true` is sent, any previously pinned note on the board is
automatically unpinned. Returns `201`.

### PUT /boards/{boardId}/notes/{noteId}

Updates note **content** (all writable fields except status/pin/favorite).

- Returns `403` if the note status is `DONE` or `REJECTED` (content is locked).
  Move it back to `MODIFIED` via the status API first.
- `pinned` and `favorite` are **not** changed here — use the dedicated APIs.

### DELETE /boards/{boardId}/notes/{noteId}

Deletes the note. Returns `204`.

## Example create payload

```json
{
  "title": "Refactor auth",
  "text": "def handler(event, context): ...",
  "textType": "CODE_PYTHON",
  "userDateTime": "2026-01-15",
  "links": [{ "url": "https://jwt.io", "label": "JWT" }],
  "iconMain": "fas fa-lock",
  "color": "#ffd966",
  "posX": 40, "posY": 40, "width": 240, "height": 200
}
```

# 08 — DynamoDB Single-Table Design

All entities live in one DynamoDB table (`PAY_PER_REQUEST`) with two global
secondary indexes. Keys and access patterns are implemented in `shared/db.py`.

## Primary keys

| Attribute | Role       |
| --------- | ---------- |
| `PK`      | Hash key   |
| `SK`      | Range key  |

## Item shapes

| Entity | PK                | SK             | GSI1PK            | GSI1SK          | GSI2PK           | GSI2SK         |
| ------ | ----------------- | -------------- | ----------------- | --------------- | ---------------- | -------------- |
| User   | `USER#<userId>`   | `PROFILE`      | `EMAIL#<email>`   | `USER#<userId>` | —                | —              |
| Board  | `BOARD#<boardId>` | `META`         | `USER#<ownerId>`  | `BOARD#<id>`    | —                | —              |
| Note   | `BOARD#<boardId>` | `NOTE#<noteId>`| —                 | —               | `BOARD#<boardId>`| `NOTE#<noteId>`|

## Indexes

- **GSI1** (`GSI1PK` / `GSI1SK`, projection ALL):
  - Unique email lookup for login/registration (`EMAIL#<email>`).
  - List boards by owner (`USER#<ownerId>` + `begins_with(BOARD#)`).
- **GSI2** (`GSI2PK` / `GSI2SK`, projection ALL):
  - List notes by board (`BOARD#<boardId>` + `begins_with(NOTE#)`).

## Access patterns

| Pattern                    | Operation                                         |
| -------------------------- | ------------------------------------------------- |
| Get user by id             | `GetItem(PK=USER#id, SK=PROFILE)`                 |
| Get user by email          | `Query(GSI1, GSI1PK=EMAIL#email)`                 |
| Register (unique email)    | `PutItem` with `attribute_not_exists(PK)`         |
| Get board                  | `GetItem(PK=BOARD#id, SK=META)`                   |
| List boards by owner       | `Query(GSI1, GSI1PK=USER#owner, begins_with SK)`  |
| Get note                   | `GetItem(PK=BOARD#id, SK=NOTE#noteId)`            |
| List notes by board        | `Query(GSI2, GSI2PK=BOARD#id, begins_with SK)`    |
| Delete board (cascade)     | Query notes, then `BatchWriteItem` deletes        |

## Number handling

DynamoDB stores numbers as `Decimal`. `shared/db.py` provides `to_dynamo`
(float → Decimal) and `from_dynamo` (Decimal → int/float) to convert
recursively when writing and reading items, keeping JSON responses clean.

## Data integrity

- Registration uses a conditional put to prevent duplicate emails.
- Board and note updates use `attribute_exists(PK)` to avoid resurrecting
  deleted items.
- Point-in-time recovery is enabled on the table.

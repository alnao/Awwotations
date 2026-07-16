# Awwotations Frontend FX

Desktop client (JavaFX 21) to manage boards and notes. It supports two backends,
selectable from the configuration file without code changes:

- **php** — `AwwotazioniBoard.php` / `AwwotazioniNotes.php` scripts (MySQL, numeric ids)
- **aws** — the workspace API Gateway + Lambda backend (DynamoDB, UUID ids)

## Configuration

The `.env` file (working directory or `frontend-fx/`) only points to the real
configuration file:

```
CONFIG_FILE=~/.config/jawwotations.config
```

The config file holds the actual settings:

```
# php style (default)
API_STYLE=php
API_BOARD_URL=http://localhost/Php/AwwotazioniBoard.php
API_NOTES_URL=http://localhost/Php/AwwotazioniNotes.php
API_BASE_URL=
JWT_TOKEN=<fixed JWT token>

# or aws style
API_STYLE=aws
API_BASE_URL=https://xxx.execute-api.eu-west-1.amazonaws.com
JWT_TOKEN=<access token signed with the Terraform JWT secret>
```

On first run, if the config file does not exist it is created with defaults and
the administration dialog opens automatically. The same dialog is available
anytime from **File → Config file...**. System environment variables override
file values.

The token is sent as `Authorization: Bearer <token>` on every request: the php
backend currently ignores it, the aws backend validates it (and it expires after
`ACCESS_TOKEN_TTL`, default 1 hour — with no login/refresh flow it must be
regenerated manually).

Depending on the style, the client automatically switches routing
(`?boardId=` / `?action=` vs nested REST routes) and the status transition
matrix (permissive PHP vs the strict workflow of `models.py`).

## Run

```bash
mvn javafx:run
```

(requires Java 21 and Maven)

## UI

- **Menu bar**:
  - *File* → Config file..., Reload (F5), Close
  - *Board* → Add..., Edit active..., Delete active
- **Board selector** on top, between two ☆/★ toggles: the left one shows all
  boards or favorites only, the right one shows all notes of the active board
  or favorite notes only
- **Notes list**, compact (`ul`-like), two lines per note: header with color
  dot, 📌 pin / ★ favorite, title, type, date (dd/MM/yy) and status, then the
  full note text in a read-only textbox below
  - double click on the header → edit note
  - `⋮` menu or right click → edit / pin / favorite / delete / status transitions
- **`+ note`** at the bottom: creates a note on the active board

Notes in `DONE`/`REJECTED` status are locked for editing (per the API);
layout fields (`posX`, `posY`, `width`, `height`), required by the API but
unused by the list UI, are preserved on edit and defaulted on create.



# AlNao.com
All source codes and information present in this repository are the result of careful and patient development work by AlNao, who has committed to verifying their correctness to the maximum extent possible. If any part of the code or content has been drawn from external sources, its origin is always cited, in respect of transparency and intellectual property. 

Some content and portions of code in this repository have also been created with the support of artificial intelligence tools, whose contribution made it possible to enrich and speed up the production of the material. However, each piece of information and code fragment has been carefully verified and validated, with the aim of ensuring the maximum quality and reliability of the content offered. 

For further details, in-depth information, or requests for clarification, please visit the website [alnao.com](https://www.alnao.com/).

## License
Public projects 
<a href="https://en.wikipedia.org/wiki/GNU_General_Public_License"  valign="middle"><img src="https://img.shields.io/badge/License-GNU-blue" style="height:22px;"  valign="middle"></a> 
*Free Software!*

Permission is granted to copy, distribute and/or modify this document under the terms of the GNU Free Documentation License, Version 3 or any later version published by the Free Software Foundation.


Create a project called AlNaoAwwotations from scratch. It is a post-it style annotation system organized in boards. Everything must be in English: code, comments, documentation, MD files, commit messages.



TECH STACK:
- Backend: AWS Lambda with Python 3.12
- Database: Amazon DynamoDB (Single Table Design)
- Infrastructure: Terraform (separate modules)
- Auth: Custom JWT with Lambda Authorizer (no Cognito) — Python libraries: PyJWT, bcrypt
- API: Amazon API Gateway (HTTP API)
- Frontend: React + Vite (on S3 + CloudFront)
- Terraform State: S3 backend + DynamoDB lock table

DOMAIN STRUCTURE:
User → has many Boards → each Board has many Notes

ENTITIES:

User: userId, email, passwordHash, name, createdAt
Board: boardId, ownerId, title, color, createdAt, updatedAt
Note fields:
- noteId (string, required): UUID
- boardId (string, required): board reference
- title (string, required): plain text only
- text (string, required): long content, may contain code and special characters
- textType (enum, required): MD, HTML, TEXT, CODE_XXXX (e.g. CODE_JAVA, CODE_JS, CODE_JSON, CODE_YAML — extensible pattern CODE_XXXX, validated with regex ^CODE_[A-Z]+$)
- userDateTime (datetime, required): date required, time optional (single datetime field)
- links (list, optional): max 10 items, each link has url (required) + label (optional)
- iconMain (string, optional): Font Awesome 5 Free class name e.g. "fas fa-star"
- iconSecondary (string, optional): Font Awesome 5 Free class name e.g. "fas fa-flag"
- color (string, required): hex color
- posX (number, required): X position on board
- posY (number, required): Y position on board
- width (number, required): width
- height (number, required): height
- status (enum, required): DREAM/CREATED/TODO/MODIFIED/DONE/REJECTED
- pinned (boolean, required): default false
- favorite (boolean, required): default false
- createdAt (datetime, required): auto
- updatedAt (datetime, required): auto
- statusChangedAt (datetime, required): auto

NOTE STATUS MANAGEMENT:

Possible statuses: DREAM, CREATED, TODO, MODIFIED, DONE, REJECTED

Allowed state transitions matrix:
- DREAM     → CREATED ✅, MODIFIED ✅, all others ❌
- CREATED   → TODO ✅, MODIFIED ✅, all others ❌
- TODO      → MODIFIED ✅, all others ❌
- MODIFIED  → DONE ✅, REJECTED ✅, all others ❌
- DONE      → MODIFIED ✅, all others ❌
- REJECTED  → MODIFIED ✅, all others ❌

Content edit lock:
- DREAM, CREATED, TODO, MODIFIED → editable ✅
- DONE, REJECTED → NOT editable ❌ (update API returns 403)

Rules:
- To unlock a DONE or REJECTED note, the user must first call the status change API to move it back to MODIFIED
- Status changes are a separate API from content updates
- Every status change updates the statusChangedAt field
- Initial status of a newly created note is CREATED
- Status logic must be centralized in shared/models.py using an Enum and a transition matrix

PINNED AND FAVORITES:

Pinned rules:
- Maximum 1 pinned note per board
- If a new note is pinned, the previously pinned note is automatically unpinned
- Only the board owner can pin/unpin
- Available for ALL statuses
- Dedicated API → PATCH /boards/{boardId}/notes/{noteId}/pin

Favorites rules:
- Boolean toggle on the note
- Any user with access to the board can toggle favorite
- Available for ALL statuses
- Dedicated API → PATCH /boards/{boardId}/notes/{noteId}/favorite

DYNAMODB SINGLE TABLE DESIGN:
- PK / SK as primary keys
- GSI1 for ownerId queries → list boards by user
- GSI2 for boardId queries → list notes by board
- Billing mode: PAY_PER_REQUEST

LAMBDA FUNCTIONS TO CREATE:
- auth/register.py — user registration
- auth/login.py — login + JWT emission (access + refresh token)
- auth/refresh.py — access token renewal
- authorizer/handler.py — JWT Lambda Authorizer
- boards/create.py, list.py, update.py, delete.py
- notes/create.py — create note with initial status CREATED
- notes/list.py — list notes of a board
- notes/update.py — update note content (blocked if DONE or REJECTED, returns 403)
- notes/delete.py — delete note
- notes/update_status.py — dedicated status change API with transition validation
- notes/pin.py — toggle pin (board owner only, max 1 per board, auto-unpins previous)
- notes/favorite.py — toggle favorite (any user with board access)
- shared/db.py — shared DynamoDB client
- shared/auth.py — JWT helper
- shared/response.py — HTTP responses helper
- shared/models.py — Pydantic v2 models + status Enum + transition matrix + textType Enum with CODE_XXXX pattern

API ENDPOINTS:
POST   /auth/register
POST   /auth/login
POST   /auth/refresh

GET    /boards
POST   /boards
PUT    /boards/{boardId}
DELETE /boards/{boardId}

GET    /boards/{boardId}/notes
POST   /boards/{boardId}/notes
PUT    /boards/{boardId}/notes/{noteId}              ← edit content (403 if DONE or REJECTED)
DELETE /boards/{boardId}/notes/{noteId}
PATCH  /boards/{boardId}/notes/{noteId}/status       ← dedicated status change
PATCH  /boards/{boardId}/notes/{noteId}/pin          ← toggle pin (owner only)
PATCH  /boards/{boardId}/notes/{noteId}/favorite     ← toggle favorite (all users)

FOLDER STRUCTURE:
AlNaoAwwotations/
├── frontend/                        # React + Vite
├── backend/
│   ├── functions/
│   │   ├── auth/
│   │   ├── authorizer/
│   │   ├── boards/
│   │   └── notes/
│   ├── shared/
│   └── requirements.txt             # PyJWT, bcrypt, boto3, pydantic
├── infrastructure/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── terraform.tfvars
│   ├── providers.tf
│   └── modules/
│       ├── dynamodb/
│       ├── lambda/
│       ├── api_gateway/
│       ├── s3_cloudfront/
│       └── iam/
└── documentation/
    ├── openapi.yaml                 # Full OpenAPI 3.0 specification
    ├── 01_StartProject.md           # Setup, prerequisites, deploy
    ├── 02_Authentication.md         # Register, login, refresh, JWT
    ├── 03_Boards.md                 # CRUD boards
    ├── 04_Notes.md                  # CRUD notes, fields, textType
    ├── 05_NoteStatus.md             # Statuses, transition matrix, rules
    ├── 06_NotePinned.md             # Pin rules, who can pin, auto-unpin
    ├── 07_NoteFavorites.md          # Favorite rules, who can toggle
    ├── 08_DynamoDB.md               # Single table design, GSI, patterns
    ├── 09_Terraform.md              # Modules, variables, deploy guide
    └── 10_OpenAPI.md                # How to use openapi.yaml

WHAT I WANT YOU TO DO:
1. Create the GitHub repository AlNaoAwwotations with the full structure above
2. Write all Python Lambda code (complete, not truncated)
3. Write all Terraform modules (complete, not truncated)
4. Write all documentation MD files in documentation/ (complete, in English)
5. Write the full openapi.yaml (OpenAPI 3.0 spec) covering all endpoints
6. Add a README.md with setup and deploy instructions
7. Add .gitignore for Python and Terraform
8. Code must be production-ready with full error handling

IMPORTANT NOTES:
- NO Cognito
- NO Serverless Framework or SAM, Terraform only
- Lambda runtime: Python 3.12
- Use pydantic v2 for all models
- Sensitive variables (JWT_SECRET, etc.) via AWS SSM Parameter Store
- All Terraform modules must have variables.tf and outputs.tf
- AWS Region: eu-west-1
- Status logic must be centralized in shared/models.py with an Enum and transition matrix
- update.py must always check note status before proceeding
- update_status.py must always validate the transition before updating
- pin.py must verify the user is the board owner and auto-unpin the previous pinned note
- favorite.py can be called by any user with access to the board
- textType must use an Enum in shared/models.py supporting fixed values (MD, HTML, TEXT) and an extensible CODE_XXXX pattern — validate with regex ^CODE_[A-Z]+$
- links is a list of {"url": "...", "label": "..."} objects with max 10 items, label optional
- userDateTime is a datetime with required date and optional time
- iconMain and iconSecondary are Font Awesome 5 Free class strings (e.g. "fas fa-star"), both optional
- Everything must be in English: code, comments, documentation, MD files, commit messages


# Commit
cd /home/albertonao/workspaceAlNao/Awwotazioni/AlNaoAwwotations
- 1. Imposta identità git (solo per questo repo)
  git config user.name "a"
  git config user.email "b@c.it"
- 2. aaaaaaaaaaaa
    git branch -M master
    git remote add origin https://github.com/alnao/Awwotations.git
    # unisce la history remota con la tua locale
    git pull origin master --allow-unrelated-histories
    # (risolvi eventuali conflitti, es. su README.md, poi git add + git commit)
    git push -u origin master
- 3. 
  git commit --amend --reset-author --no-edit
  git branch -M main --allow-unrelated-histories
  git remote add origin https://github.com/alnao/AlNaoAwwotations.git 
  git push -u origin main

# comando per zip
cd /home/albertonao/workspaceAlNao/Awwotazioni
zip -r AlNaoAwwotations.zip AlNaoAwwotations \
  -x '*/.git/*' '*/node_modules/*' '*/dist/*' '*/.terraform/*' '*/build/*' '*/__pycache__/*'
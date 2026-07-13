# 10 — OpenAPI

The complete API contract is described in
[`openapi.yaml`](./openapi.yaml) (OpenAPI 3.0.3). It covers every endpoint,
request/response schema, and security scheme.

## What it contains

- **Security**: `bearerAuth` (HTTP bearer, JWT). Applied globally; the three
  `/auth/*` endpoints override it with `security: []` (public).
- **Paths**: all auth, board, and note endpoints, including the dedicated
  `status`, `pin`, and `favorite` PATCH routes.
- **Schemas**: `RegisterRequest`, `LoginResponse`, `Board`, `Note`,
  `NoteWriteRequest`, `StatusUpdateRequest`, `Link`, `NoteStatus`, `TextType`,
  `Error`, and more.

## View it locally

Use any OpenAPI viewer, e.g. Swagger UI via Docker:

```bash
docker run --rm -p 8080:8080 \
  -e SWAGGER_JSON=/spec/openapi.yaml \
  -v "$(pwd)/documentation:/spec" \
  swaggerapi/swagger-ui
# open http://localhost:8080
```

Or Redocly CLI:

```bash
npx @redocly/cli preview-docs documentation/openapi.yaml
```

## Validate it

```bash
npx @redocly/cli lint documentation/openapi.yaml
```

## Generate a client

```bash
npx @openapitools/openapi-generator-cli generate \
  -i documentation/openapi.yaml -g typescript-fetch -o ./generated-client
```

## Keep it in sync

The spec mirrors the behavior implemented in `backend/`. When you change a
handler's contract (fields, status codes, validation), update `openapi.yaml`
accordingly so the documentation stays authoritative.

# 02 — Authentication

Authentication is a custom JWT implementation. There is **no Cognito**.
Passwords are hashed with **bcrypt**; tokens are signed with **PyJWT** (HS256).
The signing secret lives in **SSM Parameter Store** and is injected into the
Lambda environment as `JWT_SECRET`.

## Tokens

- **Access token**: short-lived (default 1 hour, `ACCESS_TOKEN_TTL`). Sent as
  `Authorization: Bearer <token>` on every protected request.
- **Refresh token**: long-lived (default 30 days, `REFRESH_TOKEN_TTL`). Used to
  obtain a new access token.

Each token carries `sub` (userId), `email`, `type` (`access`/`refresh`), `iat`
and `exp`. The Lambda Authorizer only accepts `access` tokens; `/auth/refresh`
only accepts `refresh` tokens.

## Endpoints

### POST /auth/register

```json
{ "email": "user@example.com", "password": "at-least-8-chars", "name": "Ada" }
```

Returns `201` with `{ userId, email, name }`. Returns `409` if the email is
already registered.

### POST /auth/login

```json
{ "email": "user@example.com", "password": "at-least-8-chars" }
```

Returns `200`:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "user": { "userId": "...", "email": "...", "name": "..." }
}
```

Returns `401` on invalid credentials.

### POST /auth/refresh

```json
{ "refreshToken": "..." }
```

Returns `200` with a new `{ accessToken, tokenType }`. Returns `401` if the
refresh token is invalid or expired.

## Lambda Authorizer

`functions/authorizer/handler.py` is a REQUEST authorizer with simple responses
(payload format 2.0). On success it returns:

```json
{ "isAuthorized": true, "context": { "userId": "...", "email": "..." } }
```

Downstream Lambdas read `userId` from the request context via
`response.get_user_id(event)`. Authorizer results are cached for 300 seconds.

## Security notes

- Never commit the real `jwt_secret`. Use a long random value in
  `terraform.tfvars` (which is git-ignored) or pass it via `-var`.
- Rotate the secret by updating the SSM parameter and redeploying the Lambdas;
  this invalidates all existing tokens.

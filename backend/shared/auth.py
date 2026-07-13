"""JWT and password helpers.

Uses PyJWT for token handling and bcrypt for password hashing. The JWT secret
is read from the environment (injected from AWS SSM Parameter Store by Lambda).
"""
from __future__ import annotations

import os
import time
from typing import Any, Optional

import bcrypt
import jwt

JWT_SECRET = os.environ.get("JWT_SECRET", "")
JWT_ALGORITHM = "HS256"
ACCESS_TOKEN_TTL = int(os.environ.get("ACCESS_TOKEN_TTL", "3600"))  # 1 hour
REFRESH_TOKEN_TTL = int(os.environ.get("REFRESH_TOKEN_TTL", "2592000"))  # 30 days


class AuthError(Exception):
    """Raised when authentication or token validation fails."""


def hash_password(password: str) -> str:
    """Hash a plain password with bcrypt and return a UTF-8 string."""
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(password: str, password_hash: str) -> bool:
    """Verify a plain password against a bcrypt hash."""
    try:
        return bcrypt.checkpw(
            password.encode("utf-8"), password_hash.encode("utf-8")
        )
    except (ValueError, TypeError):
        return False


def _encode(payload: dict, ttl: int, token_type: str) -> str:
    now = int(time.time())
    body = {
        **payload,
        "type": token_type,
        "iat": now,
        "exp": now + ttl,
    }
    return jwt.encode(body, JWT_SECRET, algorithm=JWT_ALGORITHM)


def create_access_token(user_id: str, email: str) -> str:
    return _encode({"sub": user_id, "email": email}, ACCESS_TOKEN_TTL, "access")


def create_refresh_token(user_id: str, email: str) -> str:
    return _encode({"sub": user_id, "email": email}, REFRESH_TOKEN_TTL, "refresh")


def decode_token(token: str, expected_type: Optional[str] = None) -> dict[str, Any]:
    """Decode and validate a JWT. Raises AuthError on failure."""
    if not JWT_SECRET:
        raise AuthError("JWT secret is not configured")
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except jwt.ExpiredSignatureError as exc:
        raise AuthError("Token has expired") from exc
    except jwt.InvalidTokenError as exc:
        raise AuthError("Invalid token") from exc
    if expected_type and payload.get("type") != expected_type:
        raise AuthError(f"Expected a {expected_type} token")
    return payload


def extract_bearer_token(headers: dict) -> str:
    """Extract a Bearer token from request headers (case-insensitive)."""
    if not headers:
        raise AuthError("Missing Authorization header")
    auth = None
    for key, value in headers.items():
        if key.lower() == "authorization":
            auth = value
            break
    if not auth:
        raise AuthError("Missing Authorization header")
    parts = auth.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise AuthError("Authorization header must be 'Bearer <token>'")
    return parts[1]

"""Shared domain models, enums, status transition matrix and Pydantic v2 schemas.

This module is the single source of truth for:
- Note status enum and the allowed transition matrix.
- Note textType enum supporting fixed values (MD, HTML, TEXT) and the
  extensible CODE_XXXX pattern validated with the regex ^CODE_[A-Z]+$.
- Pydantic v2 models used across every Lambda function.
"""
from __future__ import annotations

import re
from datetime import datetime, timezone
from enum import Enum
from typing import Optional

from pydantic import BaseModel, EmailStr, Field, field_validator, model_validator

CODE_TEXT_TYPE_REGEX = re.compile(r"^CODE_[A-Z]+$")
HEX_COLOR_REGEX = re.compile(r"^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")
FONT_AWESOME_REGEX = re.compile(r"^(fas|far|fab|fal|fad)\s+fa-[a-z0-9-]+$")

MAX_LINKS = 10


def utcnow_iso() -> str:
    """Return the current UTC time as an ISO 8601 string."""
    return datetime.now(timezone.utc).isoformat()


class NoteStatus(str, Enum):
    """Lifecycle status of a note."""

    DREAM = "DREAM"
    CREATED = "CREATED"
    TODO = "TODO"
    MODIFIED = "MODIFIED"
    DONE = "DONE"
    REJECTED = "REJECTED"


# Allowed status transitions. Any transition not listed here is forbidden.
STATUS_TRANSITIONS: dict[NoteStatus, set[NoteStatus]] = {
    NoteStatus.DREAM: {NoteStatus.CREATED, NoteStatus.MODIFIED},
    NoteStatus.CREATED: {NoteStatus.TODO, NoteStatus.MODIFIED},
    NoteStatus.TODO: {NoteStatus.MODIFIED},
    NoteStatus.MODIFIED: {NoteStatus.DONE, NoteStatus.REJECTED},
    NoteStatus.DONE: {NoteStatus.MODIFIED},
    NoteStatus.REJECTED: {NoteStatus.MODIFIED},
}

# Statuses in which the note content is locked (not editable).
LOCKED_STATUSES: set[NoteStatus] = {NoteStatus.DONE, NoteStatus.REJECTED}


def can_transition(current: NoteStatus, target: NoteStatus) -> bool:
    """Return True if moving from ``current`` to ``target`` is allowed."""
    return target in STATUS_TRANSITIONS.get(current, set())


def is_content_locked(status: NoteStatus) -> bool:
    """Return True if the note content cannot be edited in this status."""
    return status in LOCKED_STATUSES


class TextType(str, Enum):
    """Fixed textType values. CODE_XXXX values are validated separately."""

    MD = "MD"
    HTML = "HTML"
    TEXT = "TEXT"


def validate_text_type(value: str) -> str:
    """Validate a textType value.

    Accepts the fixed values MD, HTML, TEXT or any value matching the
    extensible pattern ^CODE_[A-Z]+$ (e.g. CODE_JAVA, CODE_JSON).
    """
    if value in (TextType.MD.value, TextType.HTML.value, TextType.TEXT.value):
        return value
    if CODE_TEXT_TYPE_REGEX.match(value):
        return value
    raise ValueError(
        "textType must be one of MD, HTML, TEXT or match the pattern ^CODE_[A-Z]+$"
    )


class Link(BaseModel):
    """A single link attached to a note."""

    url: str = Field(..., min_length=1, max_length=2048)
    label: Optional[str] = Field(default=None, max_length=256)


# ---------------------------------------------------------------------------
# Auth models
# ---------------------------------------------------------------------------
class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8, max_length=128)
    name: str = Field(..., min_length=1, max_length=128)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1, max_length=128)


class RefreshRequest(BaseModel):
    refreshToken: str = Field(..., min_length=1)


# ---------------------------------------------------------------------------
# Board models
# ---------------------------------------------------------------------------
class BoardCreateRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=256)
    color: str = Field(default="#ffd966")

    @field_validator("color")
    @classmethod
    def _validate_color(cls, v: str) -> str:
        if not HEX_COLOR_REGEX.match(v):
            raise ValueError("color must be a valid hex color, e.g. #ffd966")
        return v


class BoardUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=256)
    color: Optional[str] = None

    @field_validator("color")
    @classmethod
    def _validate_color(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not HEX_COLOR_REGEX.match(v):
            raise ValueError("color must be a valid hex color, e.g. #ffd966")
        return v


# ---------------------------------------------------------------------------
# Note models
# ---------------------------------------------------------------------------
class NoteBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=512)
    text: str = Field(..., min_length=1)
    textType: str = Field(...)
    userDateTime: str = Field(..., description="ISO date, optionally with time")
    links: list[Link] = Field(default_factory=list)
    iconMain: Optional[str] = None
    iconSecondary: Optional[str] = None
    color: str = Field(...)
    posX: float = Field(...)
    posY: float = Field(...)
    width: float = Field(..., gt=0)
    height: float = Field(..., gt=0)
    pinned: bool = Field(default=False)
    favorite: bool = Field(default=False)

    @field_validator("textType")
    @classmethod
    def _validate_text_type(cls, v: str) -> str:
        return validate_text_type(v)

    @field_validator("color")
    @classmethod
    def _validate_color(cls, v: str) -> str:
        if not HEX_COLOR_REGEX.match(v):
            raise ValueError("color must be a valid hex color, e.g. #ffd966")
        return v

    @field_validator("iconMain", "iconSecondary")
    @classmethod
    def _validate_icon(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not FONT_AWESOME_REGEX.match(v):
            raise ValueError('icon must be a Font Awesome class, e.g. "fas fa-star"')
        return v

    @field_validator("links")
    @classmethod
    def _validate_links(cls, v: list[Link]) -> list[Link]:
        if len(v) > MAX_LINKS:
            raise ValueError(f"links may contain at most {MAX_LINKS} items")
        return v

    @field_validator("userDateTime")
    @classmethod
    def _validate_user_datetime(cls, v: str) -> str:
        # Accept a full ISO datetime or a plain date (time optional).
        try:
            datetime.fromisoformat(v)
            return v
        except ValueError:
            pass
        try:
            datetime.strptime(v, "%Y-%m-%d")
            return v
        except ValueError as exc:
            raise ValueError(
                "userDateTime must be an ISO date (YYYY-MM-DD) or datetime"
            ) from exc


class NoteCreateRequest(NoteBase):
    """Payload to create a note. Status is forced to CREATED server-side."""


class NoteUpdateRequest(NoteBase):
    """Payload to update note content. Status is handled by a dedicated API."""


class StatusUpdateRequest(BaseModel):
    status: NoteStatus


class Note(BaseModel):
    """Full note representation returned by the API."""

    noteId: str
    boardId: str
    title: str
    text: str
    textType: str
    userDateTime: str
    links: list[Link] = Field(default_factory=list)
    iconMain: Optional[str] = None
    iconSecondary: Optional[str] = None
    color: str
    posX: float
    posY: float
    width: float
    height: float
    status: NoteStatus
    pinned: bool
    favorite: bool
    createdAt: str
    updatedAt: str
    statusChangedAt: str

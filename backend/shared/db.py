"""Shared DynamoDB helpers implementing the single-table design.

Key schema:
- USER:      PK=USER#<userId>            SK=PROFILE
             GSI1PK=EMAIL#<email>        GSI1SK=USER#<userId>   (unique email lookup)
- BOARD:     PK=BOARD#<boardId>          SK=META
             GSI1PK=USER#<ownerId>       GSI1SK=BOARD#<boardId> (list boards by owner)
- NOTE:      PK=BOARD#<boardId>          SK=NOTE#<noteId>
             GSI2PK=BOARD#<boardId>      GSI2SK=NOTE#<noteId>   (list notes by board)
"""
from __future__ import annotations

import os
from decimal import Decimal
from typing import Any, Optional

import boto3
from boto3.dynamodb.conditions import Key

TABLE_NAME = os.environ.get("TABLE_NAME", "AlNaoAwwotations")

GSI1 = "GSI1"
GSI2 = "GSI2"

_dynamodb = None


def get_table():
    """Return a cached DynamoDB Table resource."""
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.resource("dynamodb")
    return _dynamodb.Table(TABLE_NAME)


# ---------------------------------------------------------------------------
# Key builders
# ---------------------------------------------------------------------------
def user_pk(user_id: str) -> str:
    return f"USER#{user_id}"


def board_pk(board_id: str) -> str:
    return f"BOARD#{board_id}"


def note_sk(note_id: str) -> str:
    return f"NOTE#{note_id}"


def email_gsi1pk(email: str) -> str:
    return f"EMAIL#{email.lower()}"


# ---------------------------------------------------------------------------
# Number conversion helpers (DynamoDB stores numbers as Decimal)
# ---------------------------------------------------------------------------
def to_dynamo(value: Any) -> Any:
    """Recursively convert floats to Decimal for DynamoDB compatibility."""
    if isinstance(value, float):
        return Decimal(str(value))
    if isinstance(value, list):
        return [to_dynamo(v) for v in value]
    if isinstance(value, dict):
        return {k: to_dynamo(v) for k, v in value.items()}
    return value


def from_dynamo(value: Any) -> Any:
    """Recursively convert Decimal back to int/float for JSON serialization."""
    if isinstance(value, Decimal):
        return int(value) if value % 1 == 0 else float(value)
    if isinstance(value, list):
        return [from_dynamo(v) for v in value]
    if isinstance(value, dict):
        return {k: from_dynamo(v) for k, v in value.items()}
    return value


# ---------------------------------------------------------------------------
# User access
# ---------------------------------------------------------------------------
def put_user(item: dict) -> None:
    get_table().put_item(
        Item=to_dynamo(item),
        ConditionExpression="attribute_not_exists(PK)",
    )


def get_user_by_email(email: str) -> Optional[dict]:
    resp = get_table().query(
        IndexName=GSI1,
        KeyConditionExpression=Key("GSI1PK").eq(email_gsi1pk(email)),
        Limit=1,
    )
    items = resp.get("Items", [])
    return from_dynamo(items[0]) if items else None


def get_user(user_id: str) -> Optional[dict]:
    resp = get_table().get_item(Key={"PK": user_pk(user_id), "SK": "PROFILE"})
    item = resp.get("Item")
    return from_dynamo(item) if item else None


# ---------------------------------------------------------------------------
# Board access
# ---------------------------------------------------------------------------
def put_board(item: dict) -> None:
    get_table().put_item(Item=to_dynamo(item))


def get_board(board_id: str) -> Optional[dict]:
    resp = get_table().get_item(Key={"PK": board_pk(board_id), "SK": "META"})
    item = resp.get("Item")
    return from_dynamo(item) if item else None


def list_boards_by_owner(owner_id: str) -> list[dict]:
    resp = get_table().query(
        IndexName=GSI1,
        KeyConditionExpression=Key("GSI1PK").eq(user_pk(owner_id))
        & Key("GSI1SK").begins_with("BOARD#"),
    )
    return [from_dynamo(i) for i in resp.get("Items", [])]


def update_board(board_id: str, updates: dict) -> dict:
    expr_names = {}
    expr_values = {}
    set_parts = []
    for idx, (key, value) in enumerate(updates.items()):
        name = f"#f{idx}"
        val = f":v{idx}"
        expr_names[name] = key
        expr_values[val] = to_dynamo(value)
        set_parts.append(f"{name} = {val}")
    resp = get_table().update_item(
        Key={"PK": board_pk(board_id), "SK": "META"},
        UpdateExpression="SET " + ", ".join(set_parts),
        ExpressionAttributeNames=expr_names,
        ExpressionAttributeValues=expr_values,
        ConditionExpression="attribute_exists(PK)",
        ReturnValues="ALL_NEW",
    )
    return from_dynamo(resp["Attributes"])


def delete_board(board_id: str) -> None:
    table = get_table()
    # Delete all notes of the board first.
    notes = list_notes_by_board(board_id)
    with table.batch_writer() as batch:
        for note in notes:
            batch.delete_item(
                Key={"PK": board_pk(board_id), "SK": note_sk(note["noteId"])}
            )
        batch.delete_item(Key={"PK": board_pk(board_id), "SK": "META"})


# ---------------------------------------------------------------------------
# Note access
# ---------------------------------------------------------------------------
def put_note(item: dict) -> None:
    get_table().put_item(Item=to_dynamo(item))


def get_note(board_id: str, note_id: str) -> Optional[dict]:
    resp = get_table().get_item(
        Key={"PK": board_pk(board_id), "SK": note_sk(note_id)}
    )
    item = resp.get("Item")
    return from_dynamo(item) if item else None


def list_notes_by_board(board_id: str) -> list[dict]:
    resp = get_table().query(
        IndexName=GSI2,
        KeyConditionExpression=Key("GSI2PK").eq(board_pk(board_id))
        & Key("GSI2SK").begins_with("NOTE#"),
    )
    return [from_dynamo(i) for i in resp.get("Items", [])]


def update_note(board_id: str, note_id: str, updates: dict) -> dict:
    expr_names = {}
    expr_values = {}
    set_parts = []
    for idx, (key, value) in enumerate(updates.items()):
        name = f"#f{idx}"
        val = f":v{idx}"
        expr_names[name] = key
        expr_values[val] = to_dynamo(value)
        set_parts.append(f"{name} = {val}")
    resp = get_table().update_item(
        Key={"PK": board_pk(board_id), "SK": note_sk(note_id)},
        UpdateExpression="SET " + ", ".join(set_parts),
        ExpressionAttributeNames=expr_names,
        ExpressionAttributeValues=expr_values,
        ConditionExpression="attribute_exists(PK)",
        ReturnValues="ALL_NEW",
    )
    return from_dynamo(resp["Attributes"])


def delete_note(board_id: str, note_id: str) -> None:
    get_table().delete_item(
        Key={"PK": board_pk(board_id), "SK": note_sk(note_id)}
    )

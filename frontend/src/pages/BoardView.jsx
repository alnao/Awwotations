import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../api.js";
import NoteCard from "./NoteCard.jsx";
import NoteEditor from "./NoteEditor.jsx";
import BoardEditor from "./BoardEditor.jsx";

function findFreePosition(note, placedNotes, step = 30) {
  const x = note.posX || 0;
  const y = note.posY || 0;
  const w = note.width || 220;
  const h = note.height || 180;

  const hasOverlap = (tx, ty) => {
    return placedNotes.some((other) => {
      return (
        tx < other.posX + other.width &&
        tx + w > other.posX &&
        ty < other.posY + other.height &&
        ty + h > other.posY
      );
    });
  };

  if (!hasOverlap(x, y)) {
    return { posX: x, posY: y };
  }

  // Spiral search: check rings of radius r = 1, 2, 3...
  for (let r = 1; r < 100; r++) {
    // Top and bottom edges of the ring
    for (let i = -r; i <= r; i++) {
      for (const j of [-r, r]) {
        const tx = Math.max(10, x + i * step);
        const ty = Math.max(10, y + j * step);
        if (!hasOverlap(tx, ty)) {
          return { posX: tx, posY: ty };
        }
      }
    }
    // Left and right edges of the ring
    for (let j = -r + 1; j < r; j++) {
      for (const i of [-r, r]) {
        const tx = Math.max(10, x + i * step);
        const ty = Math.max(10, y + j * step);
        if (!hasOverlap(tx, ty)) {
          return { posX: tx, posY: ty };
        }
      }
    }
  }

  return { posX: x + 40, posY: y + 40 };
}

function sortNotesList(notes, orderNotes) {
  const sorted = [...notes];
  switch (orderNotes) {
    case "CREATE_DESC":
      return sorted.sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""));
    case "CREATE_ASC":
      return sorted.sort((a, b) => (a.createdAt || "").localeCompare(b.createdAt || ""));
    case "USER_DATE_DESC":
      return sorted.sort((a, b) => (b.userDateTime || "").localeCompare(a.userDateTime || ""));
    case "USER_DATE_ASC":
      return sorted.sort((a, b) => (a.userDateTime || "").localeCompare(b.userDateTime || ""));
    case "TITLE":
      return sorted.sort((a, b) => (a.title || "").localeCompare(b.title || ""));
    case "POS_X":
      return sorted.sort((a, b) => (a.posX || 0) - (b.posX || 0));
    case "POS_Y":
      return sorted.sort((a, b) => (a.posY || 0) - (b.posY || 0));
    default:
      return sorted;
  }
}

function resolveOverlaps(notes) {
  const sorted = [...notes].sort((a, b) => {
    const tA = a.updatedAt || "";
    const tB = b.updatedAt || "";
    return tB.localeCompare(tA);
  });

  const resolved = [];
  for (const n of sorted) {
    const { posX, posY } = findFreePosition(n, resolved);
    resolved.push({ ...n, posX, posY });
  }

  const resolvedMap = new Map(resolved.map((n) => [n.noteId, n]));
  return notes.map((n) => resolvedMap.get(n.noteId) || n);
}

export default function BoardView() {
  const { boardId } = useParams();
  const [board, setBoard] = useState(null);
  const [notes, setNotes] = useState([]);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(null); // note object or {} for new
  const [editingBoard, setEditingBoard] = useState(false);

  async function load() {
    try {
      const boardsRes = await api.listBoards();
      const currentBoard = boardsRes.boards?.find((b) => b.boardId === boardId);
      setBoard(currentBoard || null);

      const res = await api.listNotes(boardId);
      const rawNotes = res.notes || [];

      const orderNotes = currentBoard?.orderNotes || "POS_X";
      const sorted = sortNotesList(rawNotes, orderNotes);

      const isPosBased = orderNotes === "POS_X" || orderNotes === "POS_Y";
      const resolved = isPosBased ? resolveOverlaps(sorted) : sorted;
      setNotes(resolved);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    load();
  }, [boardId]);

  async function onSave(payload, noteId) {
    setError("");
    try {
      if (noteId) {
        await api.updateNote(boardId, noteId, payload);
      } else {
        await api.createNote(boardId, payload);
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function withReload(fn) {
    setError("");
    try {
      await fn();
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const isPosBased = !board || board.orderNotes === "POS_X" || board.orderNotes === "POS_Y";

  return (
    <div className="container">
      <div className="row" style={{ justifyContent: "space-between", flexWrap: "wrap", gap: "12px" }}>
        <div className="row" style={{ flexWrap: "wrap", gap: "16px" }}>
          <h2>
            <Link to="/">← Boards</Link>
            {board && (
              <>
                <span style={{ marginLeft: 8, color: board.color }}>/ {board.title}</span>
                <button
                  className="icon-btn"
                  onClick={() => setEditingBoard(true)}
                  style={{ marginLeft: 8, display: "inline-flex", alignItems: "center" }}
                  title="Edit Board Settings"
                >
                  <i className="fas fa-cog" />
                </button>
              </>
            )}
          </h2>
          {board && (
            <div className="row" style={{ gap: "6px" }}>
              <label htmlFor="order-notes-select" style={{ fontSize: 13, color: "var(--muted)" }}>
                Sort notes:
              </label>
              <select
                id="order-notes-select"
                value={board.orderNotes || "POS_X"}
                onChange={async (e) => {
                  const newOrder = e.target.value;
                  try {
                    await api.updateBoard(boardId, { orderNotes: newOrder });
                    load();
                  } catch (err) {
                    setError(err.message);
                  }
                }}
                style={{
                  padding: "4px 8px",
                  borderRadius: "4px",
                  border: "1px solid var(--border)",
                  fontSize: 13,
                }}
              >
                <option value="POS_X">Position X</option>
                <option value="POS_Y">Position Y</option>
                <option value="CREATE_ASC">Created (Oldest first)</option>
                <option value="CREATE_DESC">Created (Newest first)</option>
                <option value="USER_DATE_ASC">User Date (Ascending)</option>
                <option value="USER_DATE_DESC">User Date (Descending)</option>
                <option value="TITLE">Title (Alphabetical)</option>
              </select>
            </div>
          )}
        </div>
        <button
          className="btn"
          onClick={() => {
            const nextPosX = 450 + (notes.length * 30) % 300;
            const nextPosY = 150 + (notes.length * 30) % 300;
            setEditing({ posX: nextPosX, posY: nextPosY });
          }}
        >
          <i className="fas fa-plus" /> New note
        </button>
      </div>
      {error && <div className="error">{error}</div>}

      <div
        className="notes-canvas"
        style={
          !isPosBased
            ? {
                display: "flex",
                flexWrap: "wrap",
                gap: "16px",
                padding: "16px",
                alignContent: "flex-start",
              }
            : undefined
        }
      >
        {notes.map((n) => (
          <NoteCard
            key={n.noteId}
            note={n}
            isPosBased={isPosBased}
            onEdit={() => setEditing(n)}
            onDelete={() =>
              withReload(() => api.deleteNote(boardId, n.noteId))
            }
            onStatus={(status) =>
              withReload(() => api.changeStatus(boardId, n.noteId, status))
            }
            onPin={() => withReload(() => api.togglePin(boardId, n.noteId))}
            onFavorite={() =>
              withReload(() => api.toggleFavorite(boardId, n.noteId))
            }
            onMove={(x, y) =>
              withReload(() =>
                api.updateNote(boardId, n.noteId, {
                  title: n.title,
                  text: n.text,
                  textType: n.textType,
                  userDateTime: n.userDateTime,
                  links: n.links,
                  iconMain: n.iconMain,
                  iconSecondary: n.iconSecondary,
                  color: n.color,
                  posX: x,
                  posY: y,
                  width: n.width,
                  height: n.height,
                  pinned: n.pinned,
                  favorite: n.favorite,
                })
              )
            }
          />
        ))}
      </div>

      {editing && (
        <NoteEditor
          note={editing}
          onCancel={() => setEditing(null)}
          onSave={onSave}
        />
      )}

      {editingBoard && (
        <BoardEditor
          board={board}
          onCancel={() => setEditingBoard(false)}
          onSave={async (payload) => {
            setError("");
            try {
              await api.updateBoard(boardId, payload);
              setEditingBoard(false);
              load();
            } catch (err) {
              setError(err.message);
            }
          }}
        />
      )}
    </div>
  );
}

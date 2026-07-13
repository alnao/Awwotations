import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../api.js";
import NoteCard from "./NoteCard.jsx";
import NoteEditor from "./NoteEditor.jsx";

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
  const [notes, setNotes] = useState([]);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(null); // note object or {} for new

  async function load() {
    try {
      const res = await api.listNotes(boardId);
      const resolved = resolveOverlaps(res.notes || []);
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

  return (
    <div className="container">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h2>
          <Link to="/">← Boards</Link>
        </h2>
        <button
          className="btn"
          onClick={() => {
            const nextPosX = 40 + (notes.length * 30) % 300;
            const nextPosY = 40 + (notes.length * 30) % 300;
            setEditing({ posX: nextPosX, posY: nextPosY });
          }}
        >
          <i className="fas fa-plus" /> New note
        </button>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="notes-canvas">
        {notes.map((n) => (
          <NoteCard
            key={n.noteId}
            note={n}
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
    </div>
  );
}

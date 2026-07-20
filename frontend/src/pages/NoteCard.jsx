import { useEffect, useState } from "react";

// Allowed status transitions, mirroring the backend matrix in shared/models.py.
export const STATUS_TRANSITIONS = {
  DREAM: ["CREATED", "MODIFIED"],
  CREATED: ["TODO", "MODIFIED"],
  TODO: ["MODIFIED"],
  MODIFIED: ["DONE", "REJECTED"],
  DONE: ["MODIFIED"],
  REJECTED: ["MODIFIED"],
};

export const LOCKED_STATUSES = ["DONE", "REJECTED"];

export default function NoteCard({
  note,
  onEdit,
  onDelete,
  onStatus,
  onPin,
  onFavorite,
  onMove,
  isPosBased = true,
}) {
  const locked = LOCKED_STATUSES.includes(note.status);
  const dragLocked = locked || !isPosBased;
  const transitions = STATUS_TRANSITIONS[note.status] || [];

  const [pos, setPos] = useState({ x: note.posX || 0, y: note.posY || 0 });
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    setPos({ x: note.posX || 0, y: note.posY || 0 });
  }, [note.posX, note.posY]);

  const handleMouseDown = (e) => {
    if (e.button !== 0 || dragLocked) return; // Only left click, only if not locked and pos-based
    if (e.target.closest("button") || e.target.closest("select") || e.target.closest("a")) return;

    e.preventDefault();
    setIsDragging(true);

    const startX = e.clientX;
    const startY = e.clientY;
    const initialX = pos.x;
    const initialY = pos.y;

    const handleMouseMove = (moveEvent) => {
      const dx = moveEvent.clientX - startX;
      const dy = moveEvent.clientY - startY;
      const newX = Math.max(0, Math.round(initialX + dx));
      const newY = Math.max(0, Math.round(initialY + dy));
      setPos({ x: newX, y: newY });
    };

    const handleMouseUp = (upEvent) => {
      setIsDragging(false);
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);

      const dx = upEvent.clientX - startX;
      const dy = upEvent.clientY - startY;
      const finalX = Math.max(0, Math.round(initialX + dx));
      const finalY = Math.max(0, Math.round(initialY + dy));

      if ((finalX !== initialX || finalY !== initialY) && onMove) {
        onMove(finalX, finalY);
      }
    };

    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
  };

  return (
    <div
      className="note"
      style={{
        background: note.color,
        position: isPosBased ? "absolute" : "relative",
        left: isPosBased ? `${pos.x}px` : "auto",
        top: isPosBased ? `${pos.y}px` : "auto",
        width: `${note.width}px`,
        height: `${note.height}px`,
        zIndex: isDragging ? 100 : 1,
        userSelect: isDragging ? "none" : "auto",
      }}
    >
      <div
        className="note-header"
        onMouseDown={handleMouseDown}
        style={{ cursor: dragLocked ? "default" : isDragging ? "grabbing" : "grab" }}
      >
        {note.iconMain && <i className={note.iconMain} />}
        <span style={{ flex: 1, userSelect: "none" }}>{note.title}</span>
        {note.iconSecondary && <i className={note.iconSecondary} />}
        {note.pinned && <i className="fas fa-thumbtack" title="Pinned" />}
        {note.favorite && <i className="fas fa-star" title="Favorite" />}
      </div>

      <div className="note-body">
        {note.textType && note.textType.startsWith("CODE_") ? (
          <pre>{note.text}</pre>
        ) : (
          note.text
        )}
      </div>

      <div className="row">
        <span className="badge">{note.status}</span>
        <span className="badge">{note.textType}</span>
      </div>

      <div className="note-toolbar">
        <button
          className="icon-btn"
          onClick={onEdit}
          disabled={locked}
          title={locked ? "Locked: move to MODIFIED to edit" : "Edit"}
        >
          <i className="fas fa-pen" />
        </button>
        <button className="icon-btn" onClick={onPin} title="Toggle pin">
          <i className="fas fa-thumbtack" />
        </button>
        <button
          className="icon-btn"
          onClick={onFavorite}
          title="Toggle favorite"
        >
          <i className="fas fa-star" />
        </button>
        <button className="icon-btn" onClick={onDelete} title="Delete">
          <i className="fas fa-trash" />
        </button>
        {transitions.length > 0 && (
          <select
            className="icon-btn"
            value=""
            onChange={(e) => e.target.value && onStatus(e.target.value)}
            title="Change status"
          >
            <option value="">→ status</option>
            {transitions.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        )}
      </div>
    </div>
  );
}

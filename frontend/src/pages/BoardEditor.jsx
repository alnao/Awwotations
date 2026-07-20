import { useState } from "react";

export default function BoardEditor({ board, onCancel, onSave }) {
  const [form, setForm] = useState({
    title: board.title || "",
    color: board.color || "#ffd966",
    order: board.order ?? 0,
    favorite: board.favorite ?? false,
    orderNotes: board.orderNotes || "POS_X",
  });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function submit(e) {
    e.preventDefault();
    const payload = {
      title: form.title,
      color: form.color,
      order: Number(form.order),
      favorite: form.favorite,
      orderNotes: form.orderNotes,
    };
    onSave(payload);
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Edit Board</h3>
        <form onSubmit={submit}>
          <div className="field">
            <label>Title</label>
            <input
              value={form.title}
              onChange={(e) => set("title", e.target.value)}
              required
            />
          </div>

          <div className="row">
            <div className="field" style={{ flex: 1 }}>
              <label>Color</label>
              <input
                type="color"
                value={form.color}
                onChange={(e) => set("color", e.target.value)}
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Display Order</label>
              <input
                type="number"
                min="0"
                value={form.order}
                onChange={(e) => set("order", e.target.value)}
                required
              />
            </div>
          </div>

          <div className="field">
            <label>Sort notes order</label>
            <select
              value={form.orderNotes}
              onChange={(e) => set("orderNotes", e.target.value)}
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

          <div className="row" style={{ marginBottom: 16 }}>
            <label style={{ display: "flex", alignItems: "center", gap: "6px", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={form.favorite}
                onChange={(e) => set("favorite", e.target.checked)}
              />
              Favorite Board
            </label>
          </div>

          <div className="row" style={{ justifyContent: "flex-end" }}>
            <button type="button" className="btn secondary" onClick={onCancel}>
              Cancel
            </button>
            <button type="submit" className="btn">
              Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

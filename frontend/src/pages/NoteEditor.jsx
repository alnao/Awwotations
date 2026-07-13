import { useState } from "react";

const TEXT_TYPES = ["TEXT", "MD", "HTML", "CODE_JAVA", "CODE_JS", "CODE_JSON", "CODE_YAML"];

function todayDate() {
  return new Date().toISOString().slice(0, 10);
}

export default function NoteEditor({ note, onCancel, onSave }) {
  const isNew = !note.noteId;
  const [form, setForm] = useState({
    title: note.title || "",
    text: note.text || "",
    textType: note.textType || "TEXT",
    userDateTime: note.userDateTime || todayDate(),
    color: note.color || "#ffd966",
    posX: note.posX ?? 40,
    posY: note.posY ?? 40,
    width: note.width ?? 220,
    height: note.height ?? 180,
    iconMain: note.iconMain || "",
    iconSecondary: note.iconSecondary || "",
    pinned: note.pinned ?? false,
    favorite: note.favorite ?? false,
    links: note.links || [],
  });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function submit(e) {
    e.preventDefault();
    const payload = {
      ...form,
      posX: Number(form.posX),
      posY: Number(form.posY),
      width: Number(form.width),
      height: Number(form.height),
      iconMain: form.iconMain || null,
      iconSecondary: form.iconSecondary || null,
      links: form.links.filter((l) => l.url),
    };
    onSave(payload, note.noteId);
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>{isNew ? "New note" : "Edit note"}</h3>
        <form onSubmit={submit}>
          <div className="field">
            <label>Title</label>
            <input
              value={form.title}
              onChange={(e) => set("title", e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label>Text</label>
            <textarea
              rows={5}
              value={form.text}
              onChange={(e) => set("text", e.target.value)}
              required
            />
          </div>
          <div className="row">
            <div className="field" style={{ flex: 1 }}>
              <label>Text type</label>
              <select
                value={form.textType}
                onChange={(e) => set("textType", e.target.value)}
              >
                {TEXT_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Date</label>
              <input
                type="date"
                value={form.userDateTime.slice(0, 10)}
                onChange={(e) => set("userDateTime", e.target.value)}
              />
            </div>
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
              <label>Main icon (Font Awesome)</label>
              <input
                placeholder="fas fa-star"
                value={form.iconMain}
                onChange={(e) => set("iconMain", e.target.value)}
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Secondary icon</label>
              <input
                placeholder="fas fa-flag"
                value={form.iconSecondary}
                onChange={(e) => set("iconSecondary", e.target.value)}
              />
            </div>
          </div>
          <div className="row">
            {["posX", "posY", "width", "height"].map((k) => (
              <div className="field" key={k} style={{ flex: 1 }}>
                <label>{k}</label>
                <input
                  type="number"
                  value={form[k]}
                  onChange={(e) => set(k, e.target.value)}
                />
              </div>
            ))}
          </div>
          <div className="row" style={{ marginBottom: 12 }}>
            <label>
              <input
                type="checkbox"
                checked={form.pinned}
                onChange={(e) => set("pinned", e.target.checked)}
              />{" "}
              Pinned
            </label>
            <label>
              <input
                type="checkbox"
                checked={form.favorite}
                onChange={(e) => set("favorite", e.target.checked)}
              />{" "}
              Favorite
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

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api.js";

export default function Boards() {
  const navigate = useNavigate();
  const [boards, setBoards] = useState([]);
  const [error, setError] = useState("");
  const [title, setTitle] = useState("");
  const [color, setColor] = useState("#ffd966");

  async function load() {
    try {
      const res = await api.listBoards();
      setBoards(res.boards);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function createBoard(e) {
    e.preventDefault();
    setError("");
    try {
      await api.createBoard({ title, color });
      setTitle("");
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function remove(boardId, e) {
    e.stopPropagation();
    if (!confirm("Delete this board and all its notes?")) return;
    try {
      await api.deleteBoard(boardId);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="container">
      <h2>Your boards</h2>
      <form className="row" onSubmit={createBoard} style={{ marginBottom: 20 }}>
        <input
          placeholder="New board title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
        <input
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
        />
        <button className="btn" type="submit">
          <i className="fas fa-plus" /> Add
        </button>
      </form>
      {error && <div className="error">{error}</div>}
      <div className="board-grid">
        {boards.map((b) => (
          <div
            key={b.boardId}
            className="board-card"
            style={{ background: b.color }}
            onClick={() => navigate(`/boards/${b.boardId}`)}
          >
            <div className="row" style={{ justifyContent: "space-between" }}>
              <strong>{b.title}</strong>
              <button
                className="icon-btn"
                onClick={(e) => remove(b.boardId, e)}
                title="Delete board"
              >
                <i className="fas fa-trash" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

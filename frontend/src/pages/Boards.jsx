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
      // Backend sorts by order; defensive client-side sort for older deployments.
      setBoards(
        [...res.boards].sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
      );
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

  async function toggleFavorite(board, e) {
    e.stopPropagation();
    setError("");
    try {
      await api.updateBoard(board.boardId, { favorite: !board.favorite });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function move(index, delta, e) {
    e.stopPropagation();
    const target = index + delta;
    if (target < 0 || target >= boards.length) return;
    setError("");
    const reordered = [...boards];
    [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
    try {
      // Persist order = array index; also normalizes legacy boards without order.
      await Promise.all(
        reordered
          .map((b, i) => ({ b, i }))
          .filter(({ b, i }) => (b.order ?? 0) !== i)
          .map(({ b, i }) => api.updateBoard(b.boardId, { order: i }))
      );
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
        {boards.map((b, i) => (
          <div
            key={b.boardId}
            className="board-card"
            style={{ background: b.color }}
            onClick={() => navigate(`/boards/${b.boardId}`)}
          >
            <div className="row" style={{ justifyContent: "space-between" }}>
              <strong>{b.title}</strong>
              <div className="row">
                <button
                  className="icon-btn"
                  onClick={(e) => move(i, -1, e)}
                  disabled={i === 0}
                  title="Move left"
                >
                  <i className="fas fa-arrow-left" />
                </button>
                <button
                  className="icon-btn"
                  onClick={(e) => move(i, 1, e)}
                  disabled={i === boards.length - 1}
                  title="Move right"
                >
                  <i className="fas fa-arrow-right" />
                </button>
                <button
                  className="icon-btn"
                  onClick={(e) => toggleFavorite(b, e)}
                  title="Toggle favorite"
                >
                  <i
                    className={b.favorite ? "fas fa-star" : "far fa-star"}
                    style={b.favorite ? { color: "#c98a00" } : undefined}
                  />
                </button>
                <button
                  className="icon-btn"
                  onClick={(e) => remove(b.boardId, e)}
                  title="Delete board"
                >
                  <i className="fas fa-trash" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

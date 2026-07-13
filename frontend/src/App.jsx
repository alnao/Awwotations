import { Routes, Route, Navigate, Link, useNavigate } from "react-router-dom";
import { getAccessToken, clearTokens } from "./api.js";
import Login from "./pages/Login.jsx";
import Boards from "./pages/Boards.jsx";
import BoardView from "./pages/BoardView.jsx";

function RequireAuth({ children }) {
  return getAccessToken() ? children : <Navigate to="/login" replace />;
}

function Header() {
  const navigate = useNavigate();
  const authed = !!getAccessToken();
  return (
    <header className="app-header">
      <h1>
        <Link to="/" style={{ textDecoration: "none", color: "inherit" }}>
          <i className="fas fa-sticky-note" /> AlNaoAwwotations
        </Link>
      </h1>
      {authed && (
        <button
          className="btn secondary"
          onClick={() => {
            clearTokens();
            navigate("/login");
          }}
        >
          <i className="fas fa-sign-out-alt" /> Logout
        </button>
      )}
    </header>
  );
}

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <Boards />
            </RequireAuth>
          }
        />
        <Route
          path="/boards/:boardId"
          element={
            <RequireAuth>
              <BoardView />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

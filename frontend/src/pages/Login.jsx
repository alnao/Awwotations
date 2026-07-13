import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, setTokens } from "../api.js";

export default function Login() {
  const navigate = useNavigate();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ email: "", password: "", name: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const update = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      if (mode === "register") {
        await api.register(form);
      }
      const res = await api.login({
        email: form.email,
        password: form.password,
      });
      setTokens(res);
      navigate("/");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">
      <h2>{mode === "login" ? "Sign in" : "Create account"}</h2>
      <form onSubmit={submit}>
        {mode === "register" && (
          <div className="field">
            <label>Name</label>
            <input value={form.name} onChange={update("name")} required />
          </div>
        )}
        <div className="field">
          <label>Email</label>
          <input
            type="email"
            value={form.email}
            onChange={update("email")}
            required
          />
        </div>
        <div className="field">
          <label>Password</label>
          <input
            type="password"
            value={form.password}
            onChange={update("password")}
            required
            minLength={mode === "register" ? 8 : 1}
          />
        </div>
        {error && <div className="error">{error}</div>}
        <button className="btn" type="submit" disabled={loading}>
          {loading ? "..." : mode === "login" ? "Sign in" : "Register"}
        </button>
      </form>
      <p style={{ marginTop: 16, fontSize: 13 }}>
        {mode === "login" ? "No account?" : "Already registered?"}{" "}
        <a
          href="#"
          onClick={(e) => {
            e.preventDefault();
            setError("");
            setMode(mode === "login" ? "register" : "login");
          }}
        >
          {mode === "login" ? "Register" : "Sign in"}
        </a>
      </p>
    </div>
  );
}

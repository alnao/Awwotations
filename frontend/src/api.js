const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

const ACCESS_KEY = "aa_access_token";
const REFRESH_KEY = "aa_refresh_token";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_KEY);
}

export function setTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

async function request(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth) {
    const token = getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null;

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error || `Request failed with ${res.status}`);
  }
  return data;
}

export const api = {
  // Auth
  register: (payload) =>
    request("/auth/register", { method: "POST", body: payload, auth: false }),
  login: (payload) =>
    request("/auth/login", { method: "POST", body: payload, auth: false }),
  refresh: (refreshToken) =>
    request("/auth/refresh", {
      method: "POST",
      body: { refreshToken },
      auth: false,
    }),

  // Boards
  listBoards: () => request("/boards"),
  createBoard: (payload) =>
    request("/boards", { method: "POST", body: payload }),
  updateBoard: (boardId, payload) =>
    request(`/boards/${boardId}`, { method: "PUT", body: payload }),
  deleteBoard: (boardId) =>
    request(`/boards/${boardId}`, { method: "DELETE" }),

  // Notes
  listNotes: (boardId) => request(`/boards/${boardId}/notes`),
  createNote: (boardId, payload) =>
    request(`/boards/${boardId}/notes`, { method: "POST", body: payload }),
  updateNote: (boardId, noteId, payload) =>
    request(`/boards/${boardId}/notes/${noteId}`, {
      method: "PUT",
      body: payload,
    }),
  deleteNote: (boardId, noteId) =>
    request(`/boards/${boardId}/notes/${noteId}`, { method: "DELETE" }),
  changeStatus: (boardId, noteId, status) =>
    request(`/boards/${boardId}/notes/${noteId}/status`, {
      method: "PATCH",
      body: { status },
    }),
  togglePin: (boardId, noteId) =>
    request(`/boards/${boardId}/notes/${noteId}/pin`, { method: "PATCH" }),
  toggleFavorite: (boardId, noteId) =>
    request(`/boards/${boardId}/notes/${noteId}/favorite`, {
      method: "PATCH",
    }),
};

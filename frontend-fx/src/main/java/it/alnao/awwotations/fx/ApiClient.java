package it.alnao.awwotations.fx;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import it.alnao.awwotations.fx.model.Board;
import it.alnao.awwotations.fx.model.Note;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client speaking to either backend, selected via API_STYLE in .env:
 *
 * PHP — AwwotazioniBoard.php / AwwotazioniNotes.php: resource ids go in the
 * URL path after the script name (e.g. .../AwwotazioniNotes.php/42, as expected
 * by extractResourceIdFromUri()), notes are filtered with ?boardId= and PATCH
 * actions use ?action=status|pin|favorite.
 *
 * AWS — API Gateway REST routes: /boards/{boardId}/notes/{noteId} with
 * PATCH .../status|pin|favorite subroutes.
 */
public class ApiClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = new Gson();
    private final ApiStyle style;
    private final String boardUrl;
    private final String notesUrl; // PHP style only
    private final String baseUrl;  // AWS style only
    private final String jwtToken;

    public ApiClient(Config config) {
        this.style = config.apiStyle();
        this.jwtToken = config.jwtToken();
        if (style == ApiStyle.AWS) {
            this.baseUrl = config.baseApiUrl();
            this.boardUrl = baseUrl + "/boards";
            this.notesUrl = null;
        } else {
            this.baseUrl = null;
            this.boardUrl = config.boardApiUrl();
            this.notesUrl = config.notesApiUrl();
        }
    }

    public ApiStyle style() {
        return style;
    }

    public static class ApiException extends RuntimeException {
        public final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    private String request(String method, String url, Object body) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(gson.toJson(body));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .method(method, publisher);
        if (!jwtToken.isBlank()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }

        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException(0, "Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Request interrupted");
        }

        int status = response.statusCode();
        if (status == 204) {
            return null;
        }
        if (status >= 200 && status < 300) {
            return response.body();
        }

        String error = "HTTP " + status;
        try {
            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
            if (obj.has("error")) {
                error = obj.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
            // non-JSON error body, keep the generic message
        }
        throw new ApiException(status, error);
    }

    // ---------- Boards ----------

    public List<Board> listBoards() {
        String json = request("GET", boardUrl, null);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return gson.fromJson(obj.get("boards"), new TypeToken<List<Board>>() {}.getType());
    }

    public Board createBoard(Map<String, Object> payload) {
        String json = request("POST", boardUrl, payload);
        return gson.fromJson(json, Board.class);
    }

    public Board updateBoard(String boardId, Map<String, Object> fields) {
        String json = request("PUT", boardUrl + "/" + boardId, fields);
        return gson.fromJson(json, Board.class);
    }

    public void deleteBoard(String boardId) {
        request("DELETE", boardUrl + "/" + boardId, null);
    }

    // ---------- Notes ----------

    /** Note collection URL for a board (AWS) or the script URL (PHP). */
    private String notesCollectionUrl(String boardId) {
        return style == ApiStyle.AWS
                ? baseUrl + "/boards/" + boardId + "/notes"
                : notesUrl;
    }

    /** Single-note URL, optionally with a PHP ?action= / AWS subroute suffix. */
    private String noteUrl(String boardId, String noteId, String action) {
        if (style == ApiStyle.AWS) {
            String url = notesCollectionUrl(boardId) + "/" + noteId;
            return action == null ? url : url + "/" + action;
        }
        String url = notesUrl + "/" + noteId;
        return action == null ? url : url + "?action=" + action;
    }

    public List<Note> listNotes(String boardId) {
        String url = style == ApiStyle.AWS
                ? notesCollectionUrl(boardId)
                : notesUrl + "?boardId=" + boardId;
        String json = request("GET", url, null);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return gson.fromJson(obj.get("notes"), new TypeToken<List<Note>>() {}.getType());
    }

    public Note createNote(String boardId, Map<String, Object> payload) {
        payload.put("boardId", boardId); // required by PHP, ignored by AWS (path wins)
        String json = request("POST", notesCollectionUrl(boardId), payload);
        return gson.fromJson(json, Note.class);
    }

    public Note updateNote(String boardId, String noteId, Map<String, Object> payload) {
        payload.put("boardId", boardId);
        String json = request("PUT", noteUrl(boardId, noteId, null), payload);
        return gson.fromJson(json, Note.class);
    }

    public Note changeStatus(String boardId, String noteId, String status) {
        String json = request("PATCH", noteUrl(boardId, noteId, "status"), Map.of("status", status));
        return gson.fromJson(json, Note.class);
    }

    public Note togglePin(String boardId, String noteId) {
        String json = request("PATCH", noteUrl(boardId, noteId, "pin"), null);
        return gson.fromJson(json, Note.class);
    }

    public Note toggleFavorite(String boardId, String noteId) {
        String json = request("PATCH", noteUrl(boardId, noteId, "favorite"), null);
        return gson.fromJson(json, Note.class);
    }

    public void deleteNote(String boardId, String noteId) {
        request("DELETE", noteUrl(boardId, noteId, null), null);
    }
}

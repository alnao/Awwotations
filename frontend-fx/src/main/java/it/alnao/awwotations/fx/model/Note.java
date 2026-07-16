package it.alnao.awwotations.fx.model;

import it.alnao.awwotations.fx.ApiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Note as returned by both backends: AwwotazioniNotes.php (numeric ids,
 * coerced to String by Gson) and the AWS API (UUID string ids).
 */
public class Note {
    public String noteId;
    public String boardId;
    public String title;
    public String text;
    public String textType;
    public String userDateTime;
    public List<Map<String, String>> links = new ArrayList<>();
    public String iconMain;
    public String iconSecondary;
    public String color;
    public double posX;
    public double posY;
    public double width;
    public double height;
    public boolean pinned;
    public boolean favorite;
    public String status;
    public String createdAt;
    public String updatedAt;
    public String statusChangedAt;

    /** Statuses whose content is locked server-side (PUT returns 403). */
    public boolean isLocked() {
        return "DONE".equals(status) || "REJECTED".equals(status);
    }

    /**
     * Allowed status transitions for the given backend:
     * PHP mirrors getTransitions() in AwwotazioniNotes.php (permissive),
     * AWS mirrors STATUS_TRANSITIONS in backend/shared/models.py (strict).
     */
    public static List<String> transitionsFrom(String status, ApiStyle style) {
        if (style == ApiStyle.AWS) {
            return switch (status == null ? "" : status) {
                case "DREAM" -> List.of("CREATED", "MODIFIED");
                case "CREATED" -> List.of("TODO", "MODIFIED");
                case "TODO" -> List.of("MODIFIED");
                case "MODIFIED" -> List.of("DONE", "REJECTED");
                case "DONE" -> List.of("MODIFIED");
                case "REJECTED" -> List.of("MODIFIED");
                default -> List.of();
            };
        }
        return switch (status == null ? "" : status) {
            case "DREAM" -> List.of("CREATED", "REJECTED");
            case "CREATED" -> List.of("TODO", "MODIFIED", "DONE", "REJECTED");
            case "TODO" -> List.of("MODIFIED", "DONE", "REJECTED");
            case "MODIFIED" -> List.of("TODO", "DONE", "REJECTED");
            case "DONE" -> List.of("MODIFIED");
            case "REJECTED" -> List.of("DREAM");
            default -> List.of();
        };
    }
}

package it.alnao.awwotations.fx.model;

/**
 * Board as returned by both backends: AwwotazioniBoard.php (numeric ids,
 * coerced to String by Gson) and the AWS API (UUID string ids).
 */
public class Board {
    public String boardId;
    public String ownerId;
    public String title;
    public String color;
    public int order;
    public boolean favorite;
    public String createdAt;
    public String updatedAt;

    @Override
    public String toString() {
        // Shown in the ComboBox
        return title == null ? ("Board " + boardId) : title;
    }
}

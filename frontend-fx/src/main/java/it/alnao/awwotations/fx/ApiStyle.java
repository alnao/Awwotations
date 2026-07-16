package it.alnao.awwotations.fx;

/**
 * Which backend the client talks to. Same JSON entity shapes, different
 * routing, id format and status-transition matrix.
 */
public enum ApiStyle {
    /** PHP scripts (AwwotazioniBoard.php / AwwotazioniNotes.php): script URL + PATH_INFO id, ?action= for PATCH. */
    PHP,
    /** AWS API Gateway (Terraform): REST routes /boards/{boardId}/notes/{noteId}/status|pin|favorite. */
    AWS
}

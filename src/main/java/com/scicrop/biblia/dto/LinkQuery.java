package com.scicrop.biblia.dto;

/**
 * Fields accepted by the visualization endpoint. mode is fan, pairs or lxx; from is the token the
 * reader clicked (required by fan); left is a book code; right is a book code, OT, NT or ALL.
 */
public record LinkQuery(
        String mode,
        Integer lexeme,
        Long from,
        String left,
        String right) {
}

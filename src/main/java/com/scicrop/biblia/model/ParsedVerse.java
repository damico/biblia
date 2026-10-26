package com.scicrop.biblia.model;

import java.util.List;

public record ParsedVerse(ParsedPassage passage, List<ParsedToken> tokens) {
}

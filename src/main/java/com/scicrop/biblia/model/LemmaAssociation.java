package com.scicrop.biblia.model;

import java.util.List;

/**
 * Co-occurrence of a translation word and an original language lemma over corresponding verses:
 * together is the number of original verses holding the lemma among those corresponding to translation
 * verses with the word; dice = 2 * together / (wordVerses + lemmaVerses). A statistic, not a translation.
 */
public record LemmaAssociation(
        int lexemeId,
        String lemma,
        String languageCode,
        long together,
        long wordVerses,
        long lemmaVerses,
        double dice,
        List<String> glosses) {
}

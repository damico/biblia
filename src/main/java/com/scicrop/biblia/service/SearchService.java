package com.scicrop.biblia.service;

import com.scicrop.biblia.dto.SearchQuery;
import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.SearchResult;
import com.scicrop.biblia.model.SearchVerse;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.SearchRepository;
import com.scicrop.biblia.repository.SearchRepository.Counted;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
public class SearchService {

    private static final String CANON = "protestant";
    private static final String TRANSLATION = "blivre-n4";
    private static final String OLD_TESTAMENT = "wlc";
    private static final String NEW_TESTAMENT = "n1904";

    private static final int VERSE_LIMIT = 100;
    private static final int LEMMA_LIMIT = 8;

    /** Letters (any script), spaces, hyphens and apostrophes only: safe inside the search expression. */
    private static final Pattern WORD = Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M}' -]{0,39}$");

    private final EditionRepository editionRepository;
    private final SearchRepository searchRepository;

    public SearchService(EditionRepository editionRepository, SearchRepository searchRepository) {
        this.editionRepository = editionRepository;
        this.searchRepository = searchRepository;
    }

    @Transactional(readOnly = true)
    public SearchResult search(SearchQuery query) {
        String word = query.q() == null ? "" : query.q().strip().replaceAll("\\s+", " ");
        if (!WORD.matcher(word).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Search for one word or a short phrase made of letters (up to 40)");
        }
        int translationId = id(TRANSLATION);
        int oldId = id(OLD_TESTAMENT);
        int newId = id(NEW_TESTAMENT);

        Counted<SearchVerse> verses = searchRepository.findVerses(translationId, word, CANON, VERSE_LIMIT);
        return new SearchResult(
                word,
                edition(TRANSLATION),
                edition(OLD_TESTAMENT),
                edition(NEW_TESTAMENT),
                verses.total(),
                verses.items(),
                searchRepository.findAssociations(translationId, oldId, word, LEMMA_LIMIT),
                searchRepository.findAssociations(translationId, newId, word, LEMMA_LIMIT),
                searchRepository.findGlossMatches(oldId, word, LEMMA_LIMIT),
                searchRepository.findGlossMatches(newId, word, LEMMA_LIMIT));
    }

    private int id(String code) {
        return editionRepository.findIdByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown edition: " + code));
    }

    private Edition edition(String code) {
        return editionRepository.findByCode(code).orElseThrow();
    }
}

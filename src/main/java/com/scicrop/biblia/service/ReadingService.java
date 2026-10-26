package com.scicrop.biblia.service;

import com.scicrop.biblia.model.Chapter;
import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.EditionBook;
import com.scicrop.biblia.model.Passage;
import com.scicrop.biblia.repository.BookRepository;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.PassageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReadingService {

    private static final String CANON = "protestant";

    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;

    public ReadingService(EditionRepository editionRepository, BookRepository bookRepository,
                          PassageRepository passageRepository) {
        this.editionRepository = editionRepository;
        this.bookRepository = bookRepository;
        this.passageRepository = passageRepository;
    }

    @Transactional(readOnly = true)
    public List<Edition> listEditions() {
        return editionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Edition getEdition(String editionCode) {
        return editionRepository.findByCode(editionCode)
                .orElseThrow(() -> notFound("Unknown edition: " + editionCode));
    }

    @Transactional(readOnly = true)
    public List<EditionBook> listBooks(String editionCode) {
        return bookRepository.findByEdition(editionId(editionCode), CANON);
    }

    @Transactional(readOnly = true)
    public Chapter getChapter(String editionCode, String bookCode, int chapter) {
        Edition edition = getEdition(editionCode);
        int editionId = editionId(editionCode);
        EditionBook book = bookRepository.findByEdition(editionId, CANON).stream()
                .filter(b -> b.code().equals(bookCode))
                .findFirst()
                .orElseThrow(() -> notFound("Unknown book in " + editionCode + ": " + bookCode));
        List<Passage> passages = passageRepository.findChapter(editionId, bookCode, chapter);
        if (passages.isEmpty()) {
            throw notFound("Unknown chapter: " + bookCode + " " + chapter);
        }
        return new Chapter(edition, book, chapter, passages);
    }

    private int editionId(String editionCode) {
        return editionRepository.findIdByCode(editionCode)
                .orElseThrow(() -> notFound("Unknown edition: " + editionCode));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

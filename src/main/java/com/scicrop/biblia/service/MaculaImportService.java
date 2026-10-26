package com.scicrop.biblia.service;

import com.scicrop.biblia.model.EditionRef;
import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.model.ParsedVerse;
import com.scicrop.biblia.repository.BookRepository;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.MaintenanceRepository;
import com.scicrop.biblia.repository.PassageRepository;
import com.scicrop.biblia.repository.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class MaculaImportService {

    private static final Logger log = LoggerFactory.getLogger(MaculaImportService.class);

    private static final String CANON = "protestant";

    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final TokenRepository tokenRepository;
    private final MaintenanceRepository maintenanceRepository;

    public MaculaImportService(EditionRepository editionRepository, BookRepository bookRepository,
                               PassageRepository passageRepository, TokenRepository tokenRepository,
                               MaintenanceRepository maintenanceRepository) {
        this.editionRepository = editionRepository;
        this.bookRepository = bookRepository;
        this.passageRepository = passageRepository;
        this.tokenRepository = tokenRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    /**
     * Loads a MACULA Greek or Hebrew word level TSV into an edition: passages, tokens, lexemes and analyses.
     * The edition must be empty unless replace is set, in which case its current passages are deleted first
     * (alignments that point to its tokens are deleted too).
     */
    @Transactional
    public int importEdition(String editionCode, Path tsvFile, boolean replace) throws IOException {
        EditionRef edition = editionRepository.findRefByCode(editionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown edition: " + editionCode));
        if (replace) {
            log.info("Deleted {} passages of {} before import", passageRepository.deleteByEdition(edition.id()),
                    editionCode);
        } else if (passageRepository.countByEdition(edition.id()) > 0) {
            throw new IllegalStateException("Edition " + editionCode + " already has passages (use replace)");
        }

        List<ParsedVerse> verses = MaculaParser.parse(Files.readAllLines(tsvFile, StandardCharsets.UTF_8));
        LinkedHashMap<String, Integer> bookIds = bookRepository.findIdsByUsfmCode(CANON);
        List<String> canonOrder = new ArrayList<>(bookIds.keySet());
        for (ParsedVerse verse : verses) {
            if (!bookIds.containsKey(verse.passage().usfmCode())) {
                throw new IllegalArgumentException("Book " + verse.passage().usfmCode() + " is not in canon " + CANON);
            }
        }

        List<ParsedVerse> ordered = new ArrayList<>(verses);
        ordered.sort(Comparator
                .comparingInt((ParsedVerse v) -> canonOrder.indexOf(v.passage().usfmCode()))
                .thenComparingInt(v -> v.passage().chapter())
                .thenComparingInt(v -> v.passage().verse()));
        List<ParsedPassage> passages = ordered.stream().map(ParsedVerse::passage).toList();

        passageRepository.insertAll(edition.id(), passages, bookIds);
        tokenRepository.insertLexemes(edition, ordered);
        tokenRepository.insertTokens(edition, ordered, bookIds);

        maintenanceRepository.analyzeTextTables();
        long tokens = ordered.stream().mapToLong(v -> v.tokens().size()).sum();
        long analyses = ordered.stream().flatMap(v -> v.tokens().stream()).mapToLong(t -> t.analyses().size()).sum();
        log.info("Imported {} passages, {} tokens and {} analyses into {}",
                passages.size(), tokens, analyses, editionCode);
        return passages.size();
    }
}

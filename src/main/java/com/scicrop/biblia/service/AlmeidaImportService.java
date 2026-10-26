package com.scicrop.biblia.service;

import com.scicrop.biblia.model.CanonBook;
import com.scicrop.biblia.model.ParsedPassage;
import com.scicrop.biblia.repository.BookRepository;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.MaintenanceRepository;
import com.scicrop.biblia.repository.PassageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Adds the Old Testament of the Almeida 1911 (Project Gutenberg transcription) to an edition that may
 * already hold the New Testament. The New Testament passages move after the new ones in reading order.
 */
@Service
public class AlmeidaImportService {

    private static final Logger log = LoggerFactory.getLogger(AlmeidaImportService.class);

    private static final String CANON = "protestant";
    private static final String OLD_TESTAMENT = "OT";

    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final MaintenanceRepository maintenanceRepository;

    public AlmeidaImportService(EditionRepository editionRepository, BookRepository bookRepository,
                                PassageRepository passageRepository, MaintenanceRepository maintenanceRepository) {
        this.editionRepository = editionRepository;
        this.bookRepository = bookRepository;
        this.passageRepository = passageRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional
    public int importOldTestament(String editionCode, Path gutenbergFile) throws IOException {
        int editionId = editionRepository.findIdByCode(editionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown edition: " + editionCode));
        LinkedHashMap<String, Integer> bookIds = bookRepository.findIdsByUsfmCode(CANON);
        long oldTestamentBooks = bookRepository.findByCanon(CANON).stream()
                .filter(b -> OLD_TESTAMENT.equals(b.testament()))
                .count();
        List<String> codes = new ArrayList<>(bookIds.keySet()).subList(0, (int) oldTestamentBooks);
        if (passageRepository.countByBooks(editionId, codes) > 0) {
            throw new IllegalStateException("Edition " + editionCode + " already has Old Testament passages");
        }

        AlmeidaGutenbergParser.Result result = AlmeidaGutenbergParser.parse(
                Files.readString(gutenbergFile, StandardCharsets.UTF_8), codes);
        List<ParsedPassage> passages = result.passages();
        long existing = passageRepository.countByEdition(editionId);
        // existing passages are numbered 1..existing, so shifting by at least that leaves no collision
        passageRepository.shiftSortOrder(editionId, (int) Math.max(passages.size(), existing));
        passageRepository.insertAll(editionId, passages, bookIds);

        maintenanceRepository.analyzeTextTables();
        long annotations = passages.stream().mapToLong(p -> p.annotations().size()).sum();
        log.info("Imported {} Old Testament passages and {} annotations into {} ({} repairs); {} passages moved after"
                + " them", passages.size(), annotations, editionCode, result.repairs(), existing);
        return passages.size();
    }
}

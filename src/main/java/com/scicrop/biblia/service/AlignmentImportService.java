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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Imports a translation from Clear Bible Alignments: its tokens (target TSV) and the word
 * alignment to an original language edition already imported (Scripture Burrito JSON).
 * Original token ids in the alignment are MACULA ids without the leading "n".
 */
@Service
public class AlignmentImportService {

    private static final Logger log = LoggerFactory.getLogger(AlignmentImportService.class);

    private static final String CANON = "protestant";

    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final TokenRepository tokenRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public AlignmentImportService(EditionRepository editionRepository, BookRepository bookRepository,
                                  PassageRepository passageRepository, TokenRepository tokenRepository,
                                  MaintenanceRepository maintenanceRepository) {
        this.editionRepository = editionRepository;
        this.bookRepository = bookRepository;
        this.passageRepository = passageRepository;
        this.tokenRepository = tokenRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional
    public int importTranslation(String editionCode, Path tokenFile, String originalEditionCode, Path alignmentFile)
            throws IOException {
        EditionRef translation = editionRepository.findRefByCode(editionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown edition: " + editionCode));
        EditionRef original = editionRepository.findRefByCode(originalEditionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown edition: " + originalEditionCode));
        if (passageRepository.countByEdition(translation.id()) > 0) {
            throw new IllegalStateException("Edition " + editionCode + " already has passages");
        }
        if (passageRepository.countByEdition(original.id()) == 0) {
            throw new IllegalStateException("Import the original edition " + originalEditionCode + " first");
        }

        LinkedHashMap<String, Integer> bookIds = bookRepository.findIdsByUsfmCode(CANON);
        List<ParsedVerse> verses = ClearTargetParser.parse(
                Files.readAllLines(tokenFile, StandardCharsets.UTF_8), new ArrayList<>(bookIds.keySet()));
        List<ParsedPassage> passages = verses.stream().map(ParsedVerse::passage).toList();

        List<String[]> pairs = new ArrayList<>();
        JsonNode records = jsonMapper.readTree(alignmentFile.toFile()).get("records");
        for (JsonNode record : records) {
            for (JsonNode source : record.get("source")) {
                for (JsonNode target : record.get("target")) {
                    pairs.add(new String[] {"n" + source.asString(), target.asString()});
                }
            }
        }

        passageRepository.insertAll(translation.id(), passages, bookIds);
        tokenRepository.insertTokens(translation, verses, bookIds);
        int stored = tokenRepository.insertAlignments(translation.sourceRecordId(), original, translation, pairs);

        maintenanceRepository.analyzeTextTables();
        long tokens = verses.stream().mapToLong(v -> v.tokens().size()).sum();
        log.info("Imported {} passages and {} tokens into {}; {} of {} alignment pairs linked to {}",
                passages.size(), tokens, editionCode, stored, pairs.size(), originalEditionCode);
        return passages.size();
    }
}

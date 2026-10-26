package com.scicrop.biblia.service;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class BlivreImportService {

    private static final Logger log = LoggerFactory.getLogger(BlivreImportService.class);

    private static final String CANON = "protestant";

    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final MaintenanceRepository maintenanceRepository;

    public BlivreImportService(EditionRepository editionRepository, BookRepository bookRepository,
                               PassageRepository passageRepository,
                               MaintenanceRepository maintenanceRepository) {
        this.editionRepository = editionRepository;
        this.bookRepository = bookRepository;
        this.passageRepository = passageRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    /** Loads every book file of a Biblia Livre f4 directory (e.g. textos/f4/n4) into an empty edition. */
    @Transactional
    public int importEdition(String editionCode, Path sourceDir) throws IOException {
        int editionId = editionRepository.findIdByCode(editionCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown edition: " + editionCode));
        if (passageRepository.countByEdition(editionId) > 0) {
            throw new IllegalStateException("Edition " + editionCode + " already has passages");
        }

        LinkedHashMap<String, Integer> bookIds = bookRepository.findIdsByUsfmCode(CANON);
        Map<String, List<ParsedPassage>> byBook = new LinkedHashMap<>();
        int repairs = 0;

        List<Path> files;
        try (Stream<Path> listing = Files.list(sourceDir)) {
            files = listing.filter(f -> f.toString().endsWith(".txt")).sorted().toList();
        }
        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            BlivreParser.Result result = BlivreParser.parse(file.getFileName().toString(), text);
            if (!bookIds.containsKey(result.usfmCode())) {
                throw new IllegalArgumentException(file + ": book " + result.usfmCode() + " is not in canon " + CANON);
            }
            if (byBook.put(result.usfmCode(), result.passages()) != null) {
                throw new IllegalArgumentException(file + ": book " + result.usfmCode() + " appears twice");
            }
            repairs += result.repairs();
        }
        if (byBook.size() != bookIds.size()) {
            List<String> missing = bookIds.keySet().stream().filter(code -> !byBook.containsKey(code)).toList();
            throw new IllegalArgumentException("Missing books: " + missing);
        }

        List<ParsedPassage> ordered = new ArrayList<>();
        for (String usfmCode : bookIds.keySet()) {
            List<ParsedPassage> passages = new ArrayList<>(byBook.get(usfmCode));
            passages.sort(Comparator.comparingInt(ParsedPassage::chapter).thenComparingInt(ParsedPassage::verse));
            ordered.addAll(passages);
        }

        passageRepository.insertAll(editionId, ordered, bookIds);
        long annotations = ordered.stream().mapToLong(p -> p.annotations().size()).sum();
        maintenanceRepository.analyzeTextTables();
        log.info("Imported {} passages and {} annotations into {} ({} markup repairs)",
                ordered.size(), annotations, editionCode, repairs);
        return ordered.size();
    }
}

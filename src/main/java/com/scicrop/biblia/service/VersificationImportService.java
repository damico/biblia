package com.scicrop.biblia.service;

import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.StandardRef;
import com.scicrop.biblia.model.VerseStat;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.MaintenanceRepository;
import com.scicrop.biblia.repository.VersificationRepository;
import com.scicrop.biblia.repository.VersificationRepository.Row;
import com.scicrop.biblia.service.TvtmsRules.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives every passage of every edition its references in the standard versification, by evaluating
 * the TVTMS rules on the edition's own data, plus the manual corrections file. Verses without a
 * matching rule keep their own number (the edition agrees with the standard there).
 */
@Service
public class VersificationImportService {

    private static final Logger log = LoggerFactory.getLogger(VersificationImportService.class);

    private static final String TVTMS_URL = "https://github.com/STEPBible/STEPBible-Data";

    private final EditionRepository editionRepository;
    private final VersificationRepository versificationRepository;
    private final MaintenanceRepository maintenanceRepository;

    public VersificationImportService(EditionRepository editionRepository,
                                      VersificationRepository versificationRepository,
                                      MaintenanceRepository maintenanceRepository) {
        this.editionRepository = editionRepository;
        this.versificationRepository = versificationRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional
    public void importAll(Path tvtmsFile, Path correctionsFile, String tvtmsVersion) throws IOException {
        List<Rule> rules = TvtmsRules.parse(Files.readAllLines(tvtmsFile, StandardCharsets.UTF_8));
        Map<String, Map<String, String>> corrections = readCorrections(correctionsFile);
        int sourceRecordId = editionRepository.findSourceRecordId(TVTMS_URL, tvtmsVersion)
                .orElseThrow(() -> new IllegalStateException("TVTMS " + tvtmsVersion + " is not in source_records"));

        for (Edition edition : editionRepository.findAll()) {
            int editionId = editionRepository.findIdByCode(edition.code()).orElseThrow();
            List<VerseStat> stats = versificationRepository.findVerseStats(editionId);
            List<Row> rows = map(edition.code(), stats, rules,
                    corrections.getOrDefault(edition.code(), Map.of()), sourceRecordId);
            versificationRepository.replaceForEdition(editionId, rows);
        }
        maintenanceRepository.analyzeTextTables();
    }

    private List<Row> map(String editionCode, List<VerseStat> stats, List<Rule> rules,
                          Map<String, String> corrections, int sourceRecordId) {
        TvtmsRules.Edition edition = new TvtmsRules.Edition(stats);
        Map<String, List<Rule>> applied = new HashMap<>();
        int unreadable = 0;
        for (Rule rule : rules) {
            // Notes for every Bible and rules for empty verses do not change the numbering of texts with content.
            if ("AllBibles".equals(rule.type()) || rule.action().startsWith("IfEmpty")) {
                continue;
            }
            try {
                if (TvtmsRules.applies(rule, edition)) {
                    applied.computeIfAbsent(rule.source(), k -> new ArrayList<>()).add(rule);
                }
            } catch (TvtmsRules.UnreadableTest e) {
                unreadable++;
            }
        }

        List<Row> rows = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        int conflicts = 0;
        for (VerseStat v : stats) {
            String source = TvtmsRules.tvtmsBook(v.usfmCode()) + "." + v.chapter() + ":" + v.verse();
            List<Row> passageRows = new ArrayList<>();
            String manual = corrections.get(source);
            List<Rule> verseRules = applied.getOrDefault(source, List.of());
            Set<String> standards = new LinkedHashSet<>();
            verseRules.forEach(r -> standards.add(r.standard()));

            if (manual != null) {
                for (StandardRef ref : TvtmsRules.standardRefs(manual)) {
                    passageRows.add(row(v, passageRows.size() + 1, ref, "partial", "manual: " + manual, null));
                }
            } else if (standards.size() > 1) {
                // Rules that disagree for the same verse: keep the edition's own number rather than guess.
                conflicts++;
                log.warn("Conflicting TVTMS rules for {} {}: {}", editionCode, source, standards);
                passageRows.add(row(v, 1, new StandardRef(v.usfmCode(), v.chapter(), v.verse(), ""), "same",
                        "conflict", null));
            } else if (!verseRules.isEmpty()) {
                Rule rule = verseRules.getFirst();
                List<StandardRef> refs = TvtmsRules.standardRefs(rule.standard());
                if (refs.isEmpty()) {
                    log.warn("No standard verse in TVTMS rule for {} {}: {}", editionCode, source, rule.standard());
                }
                for (StandardRef ref : refs) {
                    passageRows.add(row(v, passageRows.size() + 1, ref, relation(rule, refs, ref, v),
                            rule.action(), sourceRecordId));
                }
            } else {
                passageRows.add(row(v, 1, new StandardRef(v.usfmCode(), v.chapter(), v.verse(), ""), "same",
                        null, null));
            }

            // A psalm title printed before verse 1 (Portuguese tradition) is standard verse 0.
            boolean titleMapped = passageRows.stream().anyMatch(r -> r.verse() == 0);
            if (v.hasHeading() && !titleMapped) {
                passageRows.add(row(v, passageRows.size() + 1, new StandardRef(v.usfmCode(), v.chapter(), 0, ""),
                        "title", "heading", null));
            }
            passageRows.forEach(r -> counts.merge(r.relation(), 1, Integer::sum));
            rows.addAll(passageRows);
        }
        log.info("Versification of {}: {} passages, standard references by relation {}, {} conflicts, "
                + "{} unreadable rule tests", editionCode, stats.size(), counts, conflicts, unreadable);
        return rows;
    }

    private static String relation(Rule rule, List<StandardRef> refs, StandardRef ref, VerseStat v) {
        if (ref.verse() == 0) {
            return "title";
        }
        if (refs.size() > 1 || !ref.part().isEmpty()) {
            return "partial";
        }
        boolean sameNumber = ref.usfmCode().equals(v.usfmCode()) && ref.chapter() == v.chapter()
                && ref.verse() == v.verse();
        if (rule.action().startsWith("Keep") || rule.action().startsWith("Renumber")) {
            return sameNumber ? "same" : "renumbered";
        }
        return "partial";
    }

    private static Row row(VerseStat v, int seq, StandardRef ref, String relation, String action, Integer source) {
        return new Row(v.passageId(), seq, ref.usfmCode(), ref.chapter(), ref.verse(), ref.part(), relation, action,
                source);
    }

    /** Edition code, then TVTMS source reference, then standard references. Lines starting with # are comments. */
    private static Map<String, Map<String, String>> readCorrections(Path file) throws IOException {
        Map<String, Map<String, String>> corrections = new HashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("\t");
            if (cells.length < 3 || TvtmsRules.standardRefs(cells[2]).isEmpty()) {
                throw new IllegalArgumentException("Invalid correction line: " + line);
            }
            corrections.computeIfAbsent(cells[0].strip(), k -> new HashMap<>()).put(cells[1].strip(), cells[2].strip());
        }
        return corrections;
    }
}

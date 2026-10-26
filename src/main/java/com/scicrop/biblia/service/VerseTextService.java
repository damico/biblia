package com.scicrop.biblia.service;

import com.scicrop.biblia.dto.VerseTextQuery;
import com.scicrop.biblia.model.ParallelVerse;
import com.scicrop.biblia.model.VerseText;
import com.scicrop.biblia.repository.ConcordanceRepository;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.PassageRepository;
import com.scicrop.biblia.repository.PassageRepository.PassageRef;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Texts of several verses at once, each with its corresponding verses in a translation. */
@Service
public class VerseTextService {

    private static final String CANON = "protestant";
    private static final int MAX_REFS = 200;
    private static final Pattern REF = Pattern.compile("^([1-3]?[A-Za-z]+)\\.(\\d+)\\.(\\d+)$");

    private final EditionRepository editionRepository;
    private final PassageRepository passageRepository;
    private final ConcordanceRepository concordanceRepository;

    public VerseTextService(EditionRepository editionRepository, PassageRepository passageRepository,
                            ConcordanceRepository concordanceRepository) {
        this.editionRepository = editionRepository;
        this.passageRepository = passageRepository;
        this.concordanceRepository = concordanceRepository;
    }

    @Transactional(readOnly = true)
    public List<VerseText> find(String editionCode, VerseTextQuery query) {
        int editionId = editionRepository.findIdByCode(editionCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown edition: " + editionCode));
        String translation = query.translation() == null ? editionCode : query.translation();
        if (editionRepository.findIdByCode(translation).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown edition: " + translation);
        }
        String[] refs = query.refs() == null || query.refs().isBlank() ? new String[0] : query.refs().split(",");
        if (refs.length == 0 || refs.length > MAX_REFS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give between 1 and " + MAX_REFS + " refs");
        }
        List<String> books = new ArrayList<>();
        List<Integer> chapters = new ArrayList<>();
        List<Integer> verses = new ArrayList<>();
        for (String ref : refs) {
            Matcher m = REF.matcher(ref.strip());
            if (!m.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ref: " + ref);
            }
            books.add(m.group(1));
            chapters.add(Integer.parseInt(m.group(2)));
            verses.add(Integer.parseInt(m.group(3)));
        }

        List<PassageRef> passages = passageRepository.findByReferences(editionId, books, chapters, verses, CANON);
        Map<Long, List<ParallelVerse>> parallel = translation.equals(editionCode) ? Map.of()
                : concordanceRepository.findParallel(passages.stream().map(PassageRef::id).toList(), translation);
        return passages.stream()
                .map(p -> new VerseText(p.bookCode(), p.bookName(), p.chapter(), p.verse(), p.content(),
                        parallel.getOrDefault(p.id(), List.of())))
                .toList();
    }
}

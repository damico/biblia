package com.scicrop.biblia.service;

import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.Passage;
import com.scicrop.biblia.model.VerseCorrespondence;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.PassageRepository;
import com.scicrop.biblia.repository.VersificationRepository;
import com.scicrop.biblia.repository.VersificationRepository.Match;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/** Verse level correspondence between editions; never word level. */
@Service
public class CorrespondenceService {

    private static final Set<String> WHOLE_VERSE = Set.of("same", "renumbered");

    private final EditionRepository editionRepository;
    private final PassageRepository passageRepository;
    private final VersificationRepository versificationRepository;

    public CorrespondenceService(EditionRepository editionRepository, PassageRepository passageRepository,
                                 VersificationRepository versificationRepository) {
        this.editionRepository = editionRepository;
        this.passageRepository = passageRepository;
        this.versificationRepository = versificationRepository;
    }

    @Transactional(readOnly = true)
    public VerseCorrespondence find(String editionCode, String bookCode, int chapter, int verse, String targetCode) {
        int editionId = editionId(editionCode);
        int targetId = editionId(targetCode);
        Edition target = editionRepository.findByCode(targetCode).orElseThrow();
        long passageId = passageRepository.findId(editionId, bookCode, chapter, verse)
                .orElseThrow(() -> notFound("Unknown verse: " + editionCode + " " + bookCode + " " + chapter + ":"
                        + verse));

        if (!versificationRepository.hasStandardRefs(passageId)) {
            List<Passage> same = passageRepository.findId(targetId, bookCode, chapter, verse)
                    .map(id -> passageRepository.findByIds(List.of(id)))
                    .orElse(List.of());
            return new VerseCorrespondence(target, "reference", false, same);
        }

        List<Match> matches = versificationRepository.findMatches(passageId, targetId);
        List<Long> ids = matches.stream().map(Match::passageId).distinct().toList();
        boolean exact = ids.size() == 1 && matches.stream().allMatch(m ->
                m.sourceRefCount() == 1 && m.targetRefCount() == 1
                        && m.sourcePart().isEmpty() && m.targetPart().isEmpty()
                        && WHOLE_VERSE.contains(m.sourceRelation()) && WHOLE_VERSE.contains(m.targetRelation()));
        return new VerseCorrespondence(target, "standard", exact, passageRepository.findByIds(ids));
    }

    private int editionId(String code) {
        return editionRepository.findIdByCode(code).orElseThrow(() -> notFound("Unknown edition: " + code));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

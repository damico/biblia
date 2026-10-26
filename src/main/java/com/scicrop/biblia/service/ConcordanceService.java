package com.scicrop.biblia.service;

import com.scicrop.biblia.dto.ConcordanceQuery;
import com.scicrop.biblia.model.Concordance;
import com.scicrop.biblia.model.ConcordanceHit;
import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.OccurrenceCounts;
import com.scicrop.biblia.model.ParallelVerse;
import com.scicrop.biblia.model.SegmentDetail;
import com.scicrop.biblia.model.Token;
import com.scicrop.biblia.model.TokenDetail;
import com.scicrop.biblia.repository.ConcordanceRepository;
import com.scicrop.biblia.repository.ConcordanceRepository.PassageRef;
import com.scicrop.biblia.repository.EditionRepository;
import com.scicrop.biblia.repository.TokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ConcordanceService {

    private static final String CANON = "protestant";

    /** Translation shown next to each hit, by verse correspondence (not a word alignment). */
    private static final String PARALLEL_EDITION = "blivre-n4";

    private final EditionRepository editionRepository;
    private final TokenRepository tokenRepository;
    private final ConcordanceRepository concordanceRepository;

    public ConcordanceService(EditionRepository editionRepository, TokenRepository tokenRepository,
                              ConcordanceRepository concordanceRepository) {
        this.editionRepository = editionRepository;
        this.tokenRepository = tokenRepository;
        this.concordanceRepository = concordanceRepository;
    }

    @Transactional(readOnly = true)
    public TokenDetail getToken(long tokenId) {
        TokenDetail token = tokenRepository.findDetail(tokenId, CANON)
                .orElseThrow(() -> notFound("Unknown token: " + tokenId));
        int editionId = editionId(token.editionCode());
        List<SegmentDetail> segments = token.segments().stream()
                .map(s -> s.withLemmaCounts(
                        concordanceRepository.count(editionId, ConcordanceQuery.ofLexeme(s.lexemeId()))))
                .toList();
        OccurrenceCounts formCounts = concordanceRepository.count(editionId, ConcordanceQuery.ofForm(token.normalized()));
        return new TokenDetail(token.id(), token.editionCode(), token.editionName(), token.languageCode(),
                token.bookCode(), token.bookName(), token.chapter(), token.verse(), token.surface(),
                token.normalized(), segments, formCounts);
    }

    @Transactional(readOnly = true)
    public Concordance search(String editionCode, ConcordanceQuery query) {
        boolean byLexeme = query.lexeme() != null;
        boolean byForm = query.form() != null && !query.form().isBlank();
        if (byLexeme == byForm) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give exactly one of lexeme or form");
        }
        Edition edition = editionRepository.findByCode(editionCode)
                .orElseThrow(() -> notFound("Unknown edition: " + editionCode));
        int editionId = editionId(editionCode);
        String lemma = byLexeme
                ? tokenRepository.findLemma(query.lexeme()).orElseThrow(() -> notFound("Unknown lexeme: " + query.lexeme()))
                : null;

        OccurrenceCounts counts = concordanceRepository.count(editionId, query);
        List<PassageRef> refs = concordanceRepository.findPassages(editionId, query, CANON);
        List<Long> passageIds = refs.stream().map(PassageRef::passageId).toList();
        Map<Long, List<Token>> tokens = concordanceRepository.findTokens(passageIds);
        Map<Long, List<Integer>> matched = concordanceRepository.findMatchedPositions(editionId, query, passageIds);
        Map<Long, List<ParallelVerse>> parallel = concordanceRepository.findParallel(passageIds, PARALLEL_EDITION);

        List<ConcordanceHit> hits = refs.stream()
                .map(ref -> new ConcordanceHit(
                        ref.bookCode(),
                        ref.bookName(),
                        ref.chapter(),
                        ref.verse(),
                        tokens.getOrDefault(ref.passageId(), List.of()),
                        matched.getOrDefault(ref.passageId(), List.of()),
                        PARALLEL_EDITION,
                        parallel.getOrDefault(ref.passageId(), List.of())))
                .toList();

        return new Concordance(
                edition,
                query.lexeme(),
                lemma,
                byForm ? query.form() : null,
                query.book(),
                query.morphFilters(),
                counts,
                concordanceRepository.countByBook(editionId, query, CANON),
                hits,
                query.effectiveOffset(),
                query.effectiveLimit());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> filterValues(String editionCode) {
        return concordanceRepository.findFilterValues(editionId(editionCode));
    }

    private int editionId(String editionCode) {
        return editionRepository.findIdByCode(editionCode)
                .orElseThrow(() -> notFound("Unknown edition: " + editionCode));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

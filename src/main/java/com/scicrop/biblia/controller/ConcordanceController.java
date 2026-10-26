package com.scicrop.biblia.controller;

import com.scicrop.biblia.dto.ConcordanceQuery;
import com.scicrop.biblia.dto.LinkQuery;
import com.scicrop.biblia.model.Concordance;
import com.scicrop.biblia.model.LinkGraph;
import com.scicrop.biblia.model.TokenDetail;
import com.scicrop.biblia.service.ConcordanceService;
import com.scicrop.biblia.service.LinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConcordanceController {

    private final ConcordanceService concordanceService;
    private final LinkService linkService;

    public ConcordanceController(ConcordanceService concordanceService, LinkService linkService) {
        this.concordanceService = concordanceService;
        this.linkService = linkService;
    }

    @GetMapping("/tokens/{tokenId}")
    public TokenDetail getToken(@PathVariable long tokenId) {
        return concordanceService.getToken(tokenId);
    }

    @GetMapping("/editions/{editionCode}/filter-values")
    public Map<String, List<String>> filterValues(@PathVariable String editionCode) {
        return concordanceService.filterValues(editionCode);
    }

    @GetMapping("/editions/{editionCode}/concordance/links")
    public LinkGraph links(@PathVariable String editionCode, @ModelAttribute LinkQuery query) {
        return linkService.build(editionCode, query);
    }

    @GetMapping("/editions/{editionCode}/concordance")
    public Concordance search(@PathVariable String editionCode, @ModelAttribute ConcordanceQuery query) {
        return concordanceService.search(editionCode, query);
    }
}

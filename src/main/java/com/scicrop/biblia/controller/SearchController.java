package com.scicrop.biblia.controller;

import com.scicrop.biblia.dto.SearchQuery;
import com.scicrop.biblia.model.SearchResult;
import com.scicrop.biblia.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResult search(@ModelAttribute SearchQuery query) {
        return searchService.search(query);
    }
}

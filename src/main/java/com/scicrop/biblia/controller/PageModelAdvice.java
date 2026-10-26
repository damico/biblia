package com.scicrop.biblia.controller;

import com.scicrop.biblia.model.SourceRecord;
import com.scicrop.biblia.service.SourceRecordService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Data every page shows: the footer lists all data sources recorded in the database. */
@ControllerAdvice(assignableTypes = PageController.class)
public class PageModelAdvice {

    private final SourceRecordService sourceRecordService;

    public PageModelAdvice(SourceRecordService sourceRecordService) {
        this.sourceRecordService = sourceRecordService;
    }

    @ModelAttribute("sources")
    public List<SourceRecord> sources() {
        return sourceRecordService.listAll();
    }
}

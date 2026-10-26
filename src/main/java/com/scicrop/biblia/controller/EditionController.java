package com.scicrop.biblia.controller;

import com.scicrop.biblia.model.Chapter;
import com.scicrop.biblia.model.Edition;
import com.scicrop.biblia.model.EditionBook;
import com.scicrop.biblia.dto.VerseTextQuery;
import com.scicrop.biblia.model.VerseCorrespondence;
import com.scicrop.biblia.model.VerseText;
import com.scicrop.biblia.service.CorrespondenceService;
import com.scicrop.biblia.service.ReadingService;
import com.scicrop.biblia.service.VerseTextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/editions")
public class EditionController {

    private final ReadingService readingService;
    private final CorrespondenceService correspondenceService;
    private final VerseTextService verseTextService;

    public EditionController(ReadingService readingService, CorrespondenceService correspondenceService,
                             VerseTextService verseTextService) {
        this.readingService = readingService;
        this.correspondenceService = correspondenceService;
        this.verseTextService = verseTextService;
    }

    @GetMapping
    public List<Edition> listEditions() {
        return readingService.listEditions();
    }

    @GetMapping("/{editionCode}")
    public Edition getEdition(@PathVariable String editionCode) {
        return readingService.getEdition(editionCode);
    }

    @GetMapping("/{editionCode}/books")
    public List<EditionBook> listBooks(@PathVariable String editionCode) {
        return readingService.listBooks(editionCode);
    }

    @GetMapping("/{editionCode}/books/{bookCode}/chapters/{chapter}")
    public Chapter getChapter(@PathVariable String editionCode, @PathVariable String bookCode,
                              @PathVariable int chapter) {
        return readingService.getChapter(editionCode, bookCode, chapter);
    }

    /** The verses of the target edition that hold the text of this verse (verse level, never word level). */
    @GetMapping("/{editionCode}/books/{bookCode}/chapters/{chapter}/verses/{verse}/correspondence")
    public VerseCorrespondence getCorrespondence(@PathVariable String editionCode, @PathVariable String bookCode,
                                                 @PathVariable int chapter, @PathVariable int verse,
                                                 @RequestParam String target) {
        return correspondenceService.find(editionCode, bookCode, chapter, verse, target);
    }

    /** Texts of up to 200 verses, each with the corresponding verses of a translation. */
    @GetMapping("/{editionCode}/verse-texts")
    public List<VerseText> getVerseTexts(@PathVariable String editionCode, @ModelAttribute VerseTextQuery query) {
        return verseTextService.find(editionCode, query);
    }
}

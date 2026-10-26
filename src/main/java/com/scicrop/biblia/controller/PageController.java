package com.scicrop.biblia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    private static final String DEFAULT_EDITION = "blivre-n4";

    @GetMapping("/")
    public String home(@RequestParam(name = "edition", defaultValue = DEFAULT_EDITION) String editionCode,
                       Model model) {
        model.addAttribute("edition", editionCode);
        return "index";
    }

    @GetMapping("/read/{editionCode}/{bookCode}/{chapter}")
    public String read(@PathVariable String editionCode, @PathVariable String bookCode,
                       @PathVariable int chapter, Model model) {
        model.addAttribute("edition", editionCode);
        model.addAttribute("book", bookCode);
        model.addAttribute("chapter", chapter);
        return "read";
    }

    /** The search criteria stay in the query string and are read by concordance.js. */
    @GetMapping("/concordance/{editionCode}")
    public String concordance(@PathVariable String editionCode, Model model) {
        model.addAttribute("edition", editionCode);
        return "concordance";
    }

    /** The search word stays in the query string and is read by search.js. */
    @GetMapping("/search")
    public String search() {
        return "search";
    }
}

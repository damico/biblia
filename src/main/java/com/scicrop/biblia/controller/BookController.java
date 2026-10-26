package com.scicrop.biblia.controller;

import com.scicrop.biblia.model.CanonBook;
import com.scicrop.biblia.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/canons")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/{canonCode}/books")
    public List<CanonBook> listBooks(@PathVariable String canonCode) {
        return bookService.listBooks(canonCode);
    }
}

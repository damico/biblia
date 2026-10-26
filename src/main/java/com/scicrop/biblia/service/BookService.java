package com.scicrop.biblia.service;

import com.scicrop.biblia.model.CanonBook;
import com.scicrop.biblia.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<CanonBook> listBooks(String canonCode) {
        if (!bookRepository.canonExists(canonCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown canon: " + canonCode);
        }
        return bookRepository.findByCanon(canonCode);
    }
}

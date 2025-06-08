package com.readcycle.server.controller;



import com.readcycle.server.entity.Book;
import com.readcycle.server.repository.BookRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public List<Book> getBooks(@RequestParam(required = false) String genre) {
        if (genre != null && !genre.isEmpty()) {
            return bookRepository.findByGenreIgnoreCase(genre);
        } else {
            return bookRepository.findAll();
        }
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookRepository.findById(id)
                .map(ResponseEntity::ok)//
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/books/search")
    public List<Book> searchBooks(@RequestParam("q") String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query);
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().invalidate(); // Invalidate the session
        response.sendRedirect("http://localhost:3000"); // Redirect to homepage
    }

}

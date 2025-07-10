package com.readcycle.server.repository;

import com.readcycle.server.dto.BookSummaryDto;
import com.readcycle.server.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT new com.readcycle.server.dto.BookSummaryDto(b.id, b.title, b.sellPrice, b.price, b.imageUrl) " +
            "FROM Book b JOIN b.genres g WHERE LOWER(g) = LOWER(:genre)")
    List<BookSummaryDto> findByGenreIgnoreCaseSummary(@Param("genre") String genre);

    @Query("SELECT new com.readcycle.server.dto.BookSummaryDto(b.id, b.title, b.sellPrice, b.price, b.imageUrl) " +
            "FROM Book b")
    List<BookSummaryDto> findAllBookSummaries();

    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);
}

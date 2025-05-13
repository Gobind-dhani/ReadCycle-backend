package com.readcycle.server.repository;



import com.readcycle.server.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {


    @Query("SELECT b FROM Book b JOIN b.genres g WHERE LOWER(g) = LOWER(:genre)")
    List<Book> findByGenreIgnoreCase(@Param("genre") String genre);
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);

}

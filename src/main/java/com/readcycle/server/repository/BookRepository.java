package com.readcycle.server.repository;



import com.readcycle.server.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByGenreIgnoreCase(String genre);
}

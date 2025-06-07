package com.readcycle.server.controller;

import com.readcycle.server.entity.Book;
import com.readcycle.server.entity.Review;
import com.readcycle.server.entity.User;
import com.readcycle.server.repository.BookRepository;
import com.readcycle.server.repository.ReviewRepository;
import com.readcycle.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewRepository reviewRepository, BookRepository bookRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{bookId}")
    public List<Review> getReviewsForBook(@PathVariable Long bookId) {
        return reviewRepository.findByBookId(bookId);
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<?> addReview(@PathVariable Long bookId,
                                       @RequestParam Long userId,
                                       @RequestBody Review review) {
        Book book = bookRepository.findById(bookId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (book == null || user == null) {
            return ResponseEntity.badRequest().body("Invalid book or user.");
        }

        review.setBook(book);
        review.setUser(user);
        reviewRepository.save(review);

        // Update average rating
        List<Review> allReviews = reviewRepository.findByBookId(bookId);
        double avg = allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        book.setAverageRating((float) avg);
        bookRepository.save(book);

        return ResponseEntity.ok(review);
    }
}

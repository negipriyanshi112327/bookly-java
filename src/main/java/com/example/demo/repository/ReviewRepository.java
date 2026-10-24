package com.example.demo.repository;

import com.example.demo.model.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    /** Rows of [bookId, average rating, review count] for every reviewed book. */
    @Query("select r.book.id, avg(r.rating), count(r) from Review r group by r.book.id")
    List<Object[]> ratingSummaries();

    @Query("select avg(r.rating), count(r) from Review r where r.book.id = :bookId")
    List<Object[]> ratingSummaryForBook(@Param("bookId") Long bookId);

    @Modifying
    @Query("delete from Review r where r.book.id = :bookId")
    int deleteByBookId(@Param("bookId") Long bookId);
}

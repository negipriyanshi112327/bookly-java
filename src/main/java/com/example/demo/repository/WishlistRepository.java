package com.example.demo.repository;

import com.example.demo.model.WishlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = "book")
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    Optional<WishlistItem> findByUserIdAndBookId(Long userId, Long bookId);

    @Query("select w.book.id from WishlistItem w where w.user.id = :userId")
    List<Long> findBookIdsByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("delete from WishlistItem w where w.book.id = :bookId")
    int deleteByBookId(@Param("bookId") Long bookId);
}

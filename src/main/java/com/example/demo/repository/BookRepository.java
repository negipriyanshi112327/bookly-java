package com.example.demo.repository;

import com.example.demo.model.Book;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    /** Original search kept for compatibility (title OR author, case-insensitive). */
    List<Book> findByNameContainingIgnoreCaseOrAuthorContainingIgnoreCase(String name, String author);

    /** Store catalogue: active books only, optional text search and category filter. */
    @Query("""
            select b from Book b
            where b.active = true
              and (:q is null
                   or lower(b.name) like lower(concat('%', :q, '%'))
                   or lower(b.author) like lower(concat('%', :q, '%')))
              and (:category is null or b.category = :category)
            """)
    List<Book> searchCatalogue(@Param("q") String q, @Param("category") String category, Sort sort);

    /** Admin list: includes archived books. */
    @Query("""
            select b from Book b
            where (:q is null
                   or lower(b.name) like lower(concat('%', :q, '%'))
                   or lower(b.author) like lower(concat('%', :q, '%')))
              and (:category is null or b.category = :category)
            """)
    List<Book> adminSearch(@Param("q") String q, @Param("category") String category, Sort sort);

    @Query("select distinct b.category from Book b where b.active = true and b.category is not null order by b.category")
    List<String> findActiveCategories();

    @Query("select distinct b.category from Book b where b.category is not null order by b.category")
    List<String> findAllCategories();

    List<Book> findTop4ByFeaturedTrueAndActiveTrueOrderByIdDesc();

    List<Book> findTop4ByCategoryAndActiveTrueAndIdNotOrderByIdDesc(String category, Long id);

    List<Book> findByActiveTrue();

    List<Book> findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(int limit);

    Optional<Book> findByIdAndActiveTrue(Long id);

    Optional<Book> findFirstByNameIgnoreCase(String name);

    long countByActiveTrue();

    long countByActiveTrueAndStockQuantityLessThanEqual(int limit);

    @Query("select coalesce(sum(b.stockQuantity), 0) from Book b where b.active = true")
    long totalUnitsInStock();

    /**
     * Atomic stock reservation used at checkout. The WHERE clause makes the database
     * refuse the update when there is not enough stock, so two simultaneous orders
     * can never push stock below zero. Returns 1 on success, 0 if not enough stock.
     */
    @Modifying
    @Query("""
            update Book b set b.stockQuantity = b.stockQuantity - :qty
            where b.id = :id and b.active = true and b.stockQuantity >= :qty
            """)
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /** Puts stock back when an order is cancelled. */
    @Modifying
    @Query("update Book b set b.stockQuantity = b.stockQuantity + :qty where b.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Admin stock change with optimistic check: only applies if the stock is still what
     * the admin saw on screen. Returns 0 if an order changed it in the meantime.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Book b set b.stockQuantity = :newQty
            where b.id = :id and b.stockQuantity = :expected
            """)
    int updateStockIfUnchanged(@Param("id") Long id, @Param("expected") int expected, @Param("newQty") int newQty);

    @Query("select b.stockQuantity from Book b where b.id = :id")
    Optional<Integer> findStockById(@Param("id") Long id);
}

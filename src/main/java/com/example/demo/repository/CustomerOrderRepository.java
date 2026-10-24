package com.example.demo.repository;

import com.example.demo.model.CustomerOrder;
import com.example.demo.model.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    /** A customer's own orders, newest first, with their items. */
    @EntityGraph(attributePaths = "items")
    List<CustomerOrder> findByUserIdOrderByOrderDateDesc(Long userId);

    /** One order with items and customer loaded (pages render after the transaction ends). */
    @Query("select distinct o from CustomerOrder o left join fetch o.items left join fetch o.user where o.id = :id")
    Optional<CustomerOrder> findWithItemsById(@Param("id") Long id);

    @EntityGraph(attributePaths = "user")
    Optional<CustomerOrder> findByCheckoutToken(String checkoutToken);

    @Query("""
            select o from CustomerOrder o left join fetch o.user u
            where (:status is null or o.status = :status)
              and (:orderId is null or o.id = :orderId)
              and (:text is null
                   or lower(o.customerName) like lower(concat('%', :text, '%'))
                   or lower(u.email) like lower(concat('%', :text, '%'))
                   or lower(u.name) like lower(concat('%', :text, '%')))
            order by o.orderDate desc
            """)
    List<CustomerOrder> adminSearch(@Param("status") OrderStatus status,
                                    @Param("orderId") Long orderId,
                                    @Param("text") String text);

    @EntityGraph(attributePaths = "user")
    List<CustomerOrder> findTop5ByOrderByOrderDateDesc();

    long countByStatus(OrderStatus status);

    long countByUserId(Long userId);

    @Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status = :status")
    BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);

    /** Number of order lines that point at a book (a book with orders is archived, not deleted). */
    @Query("select count(oi) from OrderItem oi where oi.book.id = :bookId")
    long countItemsForBook(@Param("bookId") Long bookId);

    /** Verified purchase check for reviews: delivered orders by this customer containing this book. */
    @Query("""
            select count(oi) from OrderItem oi
            where oi.book.id = :bookId and oi.order.user.id = :userId and oi.order.status = :status
            """)
    long countPurchases(@Param("userId") Long userId,
                        @Param("bookId") Long bookId,
                        @Param("status") OrderStatus status);

    @Query("select o.user.id, count(o) from CustomerOrder o where o.user is not null group by o.user.id")
    List<Object[]> countOrdersPerUser();
}

package com.example.demo;

import com.example.demo.dto.BookForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import com.example.demo.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookServiceTest {

    @Autowired BookService bookService;
    @Autowired BookRepository books;

    Book sapiens, nineteen, farm;

    @BeforeEach
    void setUp() {
        sapiens = TestData.book(books, "Sapiens", "Yuval Noah Harari", "History", "499.00", 10);
        nineteen = TestData.book(books, "1984", "George Orwell", "Classics", "249.00", 5);
        farm = TestData.book(books, "Animal Farm", "George Orwell", "Classics", "199.00", 0);
    }

    private List<String> names(List<Book> list) {
        return list.stream().map(Book::getName).toList();
    }

    @Test
    void searchByTitleIgnoresCase() {
        assertThat(names(bookService.catalogue("sAPIens", null, null))).containsExactly("Sapiens");
    }

    @Test
    void searchByAuthorIgnoresCase() {
        assertThat(names(bookService.catalogue("ORWELL", null, null)))
                .containsExactlyInAnyOrder("1984", "Animal Farm");
    }

    @Test
    void filterByCategoryAndSortByPrice() {
        assertThat(names(bookService.catalogue(null, "Classics", "price_asc"))).containsExactly("Animal Farm", "1984");
        assertThat(names(bookService.catalogue(null, "Classics", "price_desc"))).containsExactly("1984", "Animal Farm");
    }

    @Test
    void archivedBooksAreHiddenFromTheStore() {
        nineteen.setActive(false);
        books.saveAndFlush(nineteen);
        assertThat(names(bookService.catalogue("orwell", null, null))).containsExactly("Animal Farm");
    }

    @Test
    void adminCanCreateAndEditABook() {
        BookForm form = new BookForm();
        form.setName("Clean Code");
        form.setAuthor("Robert C. Martin");
        form.setCategory("Technology");
        form.setPrice(new BigDecimal("699.5"));
        form.setStockQuantity(7);
        Book created = bookService.create(form);
        assertThat(created.getPrice()).isEqualByComparingTo("699.50");

        BookForm edit = BookForm.from(created);
        edit.setPrice(new BigDecimal("650.00"));
        edit.setStockQuantity(9);
        bookService.update(created.getId(), edit);

        Book reloaded = books.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getPrice()).isEqualByComparingTo("650.00");
        assertThat(books.findStockById(created.getId())).contains(9);
    }

    @Test
    void stockChangeIsRefusedWhenStockChangedMeanwhile() {
        // admin opened the form when stock was 10, then an order took 2
        books.decrementStock(sapiens.getId(), 2);
        assertThatThrownBy(() -> bookService.changeStock(sapiens.getId(), 10, 50))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8");
    }

    @Test
    void negativeStockIsRejected() {
        assertThatThrownBy(() -> bookService.changeStock(sapiens.getId(), 10, -1))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void neverOrderedBookIsDeleted() {
        assertThat(bookService.deleteOrArchive(farm.getId())).isEqualTo(BookService.RemovalResult.DELETED);
        assertThat(books.findById(farm.getId())).isEmpty();
    }
}

package com.example.demo.service;

import com.example.demo.dto.BookForm;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CustomerOrderRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.WishlistRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BookService {

    /** Sort options offered on the home page. Unknown values fall back to title A-Z. */
    public enum SortOption {
        TITLE("title", "Title (A–Z)", Sort.by(Sort.Order.asc("name").ignoreCase())),
        PRICE_ASC("price_asc", "Price: low to high", Sort.by("price").ascending().and(Sort.by("name"))),
        PRICE_DESC("price_desc", "Price: high to low", Sort.by("price").descending().and(Sort.by("name"))),
        NEWEST("newest", "Newest first", Sort.by("id").descending());

        private final String key;
        private final String label;
        private final Sort sort;

        SortOption(String key, String label, Sort sort) {
            this.key = key;
            this.label = label;
            this.sort = sort;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public Sort getSort() { return sort; }

        public static SortOption from(String key) {
            for (SortOption option : values()) {
                if (option.key.equalsIgnoreCase(key == null ? "" : key)) return option;
            }
            return TITLE;
        }
    }

    public enum RemovalResult { DELETED, ARCHIVED }

    private final BookRepository books;
    private final CustomerOrderRepository orders;
    private final WishlistRepository wishlists;
    private final ReviewRepository reviews;

    public BookService(BookRepository books, CustomerOrderRepository orders,
                       WishlistRepository wishlists, ReviewRepository reviews) {
        this.books = books;
        this.orders = orders;
        this.wishlists = wishlists;
        this.reviews = reviews;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // ---------- store front ----------

    @Transactional(readOnly = true)
    public List<Book> catalogue(String query, String category, String sortKey) {
        return books.searchCatalogue(blankToNull(query), blankToNull(category), SortOption.from(sortKey).getSort());
    }

    @Transactional(readOnly = true)
    public List<Book> featured() {
        return books.findTop4ByFeaturedTrueAndActiveTrueOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return books.findActiveCategories();
    }

    @Transactional(readOnly = true)
    public List<String> allCategories() {
        return books.findAllCategories();
    }

    /** A book visible in the store (archived books are "not found" for customers). */
    @Transactional(readOnly = true)
    public Book getActiveBook(Long id) {
        return books.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("That book is not available."));
    }

    @Transactional(readOnly = true)
    public List<Book> related(Book book) {
        if (book.getCategory() == null) return List.of();
        return books.findTop4ByCategoryAndActiveTrueAndIdNotOrderByIdDesc(book.getCategory(), book.getId());
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<Book> adminSearch(String query, String category) {
        return books.adminSearch(blankToNull(query), blankToNull(category),
                Sort.by(Sort.Order.desc("active"), Sort.Order.asc("name").ignoreCase()));
    }

    @Transactional(readOnly = true)
    public Book getAnyBook(Long id) {
        return books.findById(id).orElseThrow(() -> new NotFoundException("Book not found."));
    }

    @Transactional
    public Book create(BookForm form) {
        Book book = new Book();
        copyDetails(form, book);
        book.setStockQuantity(form.getStockQuantity());
        book.setActive(true);
        return books.save(book);
    }

    /**
     * Updates the details. Stock is changed with a separate guarded update so an order
     * placed while the admin was editing is never overwritten.
     */
    @Transactional
    public Book update(Long id, BookForm form) {
        Book book = getAnyBook(id);
        copyDetails(form, book);
        books.flush();

        int newStock = form.getStockQuantity();
        Integer expected = form.getOriginalStock();
        if (expected != null && newStock != expected) {
            changeStock(id, expected, newStock);
        }
        return book;
    }

    /**
     * Books that appear in past orders are archived (hidden from the store) so the order
     * history stays intact. Books that were never ordered are deleted together with their
     * wishlist entries and reviews.
     */
    @Transactional
    public RemovalResult deleteOrArchive(Long id) {
        Book book = getAnyBook(id);
        if (orders.countItemsForBook(id) > 0) {
            book.setActive(false);
            book.setFeatured(false);
            return RemovalResult.ARCHIVED;
        }
        wishlists.deleteByBookId(id);
        reviews.deleteByBookId(id);
        books.delete(book);
        return RemovalResult.DELETED;
    }

    @Transactional
    public void restore(Long id) {
        getAnyBook(id).setActive(true);
    }

    /** Sets stock to newQty only if it is still `expected` (what the admin saw). */
    @Transactional
    public void changeStock(Long id, int expected, int newQty) {
        if (newQty < 0) {
            throw new BusinessException("Stock cannot be negative.");
        }
        if (newQty > 100000) {
            throw new BusinessException("Stock must be at most 100000.");
        }
        int updated = books.updateStockIfUnchanged(id, expected, newQty);
        if (updated == 0) {
            int current = books.findStockById(id)
                    .orElseThrow(() -> new NotFoundException("Book not found."));
            throw new BusinessException("Stock changed to " + current
                    + " while you were editing (probably a new order). Please check and try again.");
        }
    }

    @Transactional(readOnly = true)
    public List<Book> inventory(boolean lowOnly) {
        if (lowOnly) {
            return books.findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(Book.LOW_STOCK_LIMIT);
        }
        return books.adminSearch(null, null, Sort.by(Sort.Order.desc("active"), Sort.Order.asc("stockQuantity"),
                Sort.Order.asc("name")));
    }

    private void copyDetails(BookForm form, Book book) {
        book.setName(form.getName().trim());
        book.setAuthor(form.getAuthor().trim());
        book.setDescription(blankToNull(form.getDescription()));
        book.setCategory(form.getCategory().trim());
        book.setPrice(form.getPrice().setScale(2, RoundingMode.HALF_UP));
        book.setImage(blankToNull(form.getImage()));
        book.setFeatured(form.isFeatured());
    }

    /** Rounds to paise (2 decimals); null counts as zero. */
    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

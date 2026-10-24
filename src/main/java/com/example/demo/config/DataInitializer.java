package com.example.demo.config;

import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sample catalogue (the same 18 books as database/bookly_db.sql).
 *
 * - A book is inserted only if no book with that title exists, so restarting never
 *   creates duplicates and never overwrites prices or stock an admin has changed.
 * - The 4 original books from the first version had no category/description/stock;
 *   those missing fields are filled in once.
 * - Set BOOKLY_SEED=false to switch seeding off (e.g. after deleting sample books).
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String COVER = "https://covers.openlibrary.org/b/isbn/%s-L.jpg";

    record SeedBook(String name, String author, String category, int price, int stock,
                    boolean featured, String isbn, String description) {}

    static final List<SeedBook> SAMPLE_BOOKS = List.of(
            // ---- the 4 original books ----
            new SeedBook("Atomic Habits", "James Clear", "Self-Help", 399, 25, true, "9780735211292",
                    "A practical guide to building good habits and breaking bad ones through tiny, consistent changes that compound over time."),
            new SeedBook("The Alchemist", "Paulo Coelho", "Fiction", 299, 30, true, "9780062315007",
                    "A young shepherd travels from Spain to the Egyptian desert in search of treasure and learns to listen to his heart along the way."),
            new SeedBook("Ikigai", "Héctor García", "Self-Help", 249, 20, false, "9780143130727",
                    "Explores the Japanese idea of a reason for being, drawing on the daily habits of some of the longest-living people in the world."),
            new SeedBook("Deep Work", "Cal Newport", "Productivity", 349, 18, false, "9781455586691",
                    "Argues that focused, distraction-free work is rare and valuable, and gives rules for training your attention."),
            // ---- 14 new books ----
            new SeedBook("Sapiens", "Yuval Noah Harari", "History", 499, 15, true, "9780062316097",
                    "A sweeping history of humankind, from the first humans in Africa to the scientific and economic revolutions of today."),
            new SeedBook("The Psychology of Money", "Morgan Housel", "Finance", 349, 22, true, "9780857197689",
                    "Short stories about how people actually think about money, and why behaviour matters more than intelligence when investing."),
            new SeedBook("Rich Dad Poor Dad", "Robert T. Kiyosaki", "Finance", 299, 12, false, "9781612680194",
                    "Contrasts the money lessons of two father figures and makes the case for financial literacy, assets and passive income."),
            new SeedBook("Thinking, Fast and Slow", "Daniel Kahneman", "Psychology", 599, 8, false, "9780374533557",
                    "A Nobel-winning psychologist explains the two systems that drive how we think, and the biases that lead us astray."),
            new SeedBook("To Kill a Mockingbird", "Harper Lee", "Classics", 399, 10, false, "9780061120084",
                    "Seen through the eyes of young Scout Finch, a story of justice, prejudice and courage in a small town in the American South."),
            new SeedBook("1984", "George Orwell", "Classics", 249, 16, false, "9780451524935",
                    "A chilling portrait of a surveillance state where Big Brother watches everyone and even thought can be a crime."),
            new SeedBook("Pride and Prejudice", "Jane Austen", "Classics", 199, 14, false, "9780141439518",
                    "Elizabeth Bennet and Mr Darcy clash over first impressions in Austen's witty novel of manners, family and marriage."),
            new SeedBook("The Great Gatsby", "F. Scott Fitzgerald", "Classics", 229, 3, false, "9780743273565",
                    "The mysterious Jay Gatsby throws lavish parties on Long Island while chasing a dream from his past in the Jazz Age."),
            new SeedBook("The Hobbit", "J.R.R. Tolkien", "Fantasy", 449, 11, false, "9780547928227",
                    "Bilbo Baggins leaves his comfortable home for an adventure with dwarves, a wizard and a dragon guarding a mountain of gold."),
            new SeedBook("The Midnight Library", "Matt Haig", "Fiction", 399, 9, false, "9780525559474",
                    "Between life and death, Nora finds a library whose books let her try the other lives she might have lived."),
            new SeedBook("Clean Code", "Robert C. Martin", "Technology", 699, 7, false, "9780132350884",
                    "A handbook of practices for writing readable, maintainable software, with many before-and-after code examples."),
            new SeedBook("The Pragmatic Programmer", "Andrew Hunt & David Thomas", "Technology", 749, 4, false, "9780135957059",
                    "Timeless advice on the craft of programming, from avoiding duplication to taking responsibility for your career."),
            new SeedBook("Zero to One", "Peter Thiel", "Business", 399, 13, false, "9780804139298",
                    "A startup founder's notes on building companies that create something truly new instead of copying what exists."),
            new SeedBook("Wings of Fire", "A.P.J. Abdul Kalam", "Biography", 299, 0, false, "9788173711466",
                    "The autobiography of India's Missile Man, from his childhood in Rameswaram to leading the country's space and defence programmes.")
    );

    @Bean
    CommandLineRunner seedBooks(BookRepository books, TransactionTemplate tx,
                                @Value("${bookly.seed.enabled:true}") boolean enabled) {
        return args -> {
            if (!enabled) {
                log.info("Sample book seeding is disabled (BOOKLY_SEED=false).");
                return;
            }
            tx.executeWithoutResult(status -> {
                int inserted = 0;
                for (SeedBook seed : SAMPLE_BOOKS) {
                    Book book = books.findFirstByNameIgnoreCase(seed.name()).orElse(null);
                    if (book == null) {
                        book = new Book(seed.name(), seed.author(), BigDecimal.valueOf(seed.price()).setScale(2),
                                COVER.formatted(seed.isbn()));
                        book.setCategory(seed.category());
                        book.setDescription(seed.description());
                        book.setStockQuantity(seed.stock());
                        book.setFeatured(seed.featured());
                        books.save(book);
                        inserted++;
                    } else if (book.getCategory() == null) {
                        // book from the first version of the project: fill only what is missing
                        book.setCategory(seed.category());
                        if (book.getDescription() == null) book.setDescription(seed.description());
                        if (book.getImage() == null || book.getImage().isBlank()) book.setImage(COVER.formatted(seed.isbn()));
                        if (book.getStockQuantity() <= 0) book.setStockQuantity(seed.stock());
                        book.setFeatured(seed.featured());
                    }
                }
                if (inserted > 0) log.info("Inserted {} sample books.", inserted);
            });
        };
    }
}

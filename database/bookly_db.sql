-- =====================================================================
-- BOOKLY Online Bookstore - MySQL 8 setup script
-- Database: bookly_db
--
-- How to use (MySQL Workbench): File > Open SQL Script > this file > Execute (lightning icon).
-- Safe to run more than once: tables use IF NOT EXISTS and books are inserted only if missing.
--
-- You do NOT have to run this file: with spring.jpa.hibernate.ddl-auto=update the
-- application creates/updates the same tables and inserts the same books at start-up.
-- Use it when you want to create the schema yourself or show it in the project report.
--
-- The administrator account is NOT created here (no passwords in SQL). It is created
-- by the app from BOOKLY_ADMIN_EMAIL / BOOKLY_ADMIN_PASSWORD with a BCrypt hash.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS bookly_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookly_db;

-- ---------------------------------------------------------------------
-- 1. Users (customers and administrators)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt hash, never plaintext',
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER' COMMENT 'CUSTOMER or ADMIN',
    enabled     BIT(1)       NOT NULL DEFAULT b'1',
    created_at  DATETIME(6)  NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_users_email (email),
    KEY idx_app_users_role (role),
    CONSTRAINT chk_app_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. Books (table name "products" kept from the first version)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    name            VARCHAR(200)   NOT NULL,
    author          VARCHAR(150)   NULL,
    description     VARCHAR(2000)  NULL,
    category        VARCHAR(60)    NULL,
    price           DECIMAL(10,2)  NOT NULL,
    image           VARCHAR(500)   NULL,
    stock_quantity  INT            NOT NULL DEFAULT 0,
    featured        BIT(1)         NOT NULL DEFAULT b'0',
    active          BIT(1)         NOT NULL DEFAULT b'1' COMMENT '0 = archived (kept for old orders)',
    created_at      DATETIME(6)    NULL,
    updated_at      DATETIME(6)    NULL,
    PRIMARY KEY (id),
    KEY idx_products_name (name),
    KEY idx_products_author (author),
    KEY idx_products_category (category),
    CONSTRAINT chk_products_price CHECK (price > 0),
    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. Orders (Cash on Delivery)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_orders (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    user_id         BIGINT         NULL,
    customer_name   VARCHAR(100)   NULL,
    phone           VARCHAR(20)    NULL,
    address         VARCHAR(255)   NULL,
    total_amount    DECIMAL(12,2)  NOT NULL,
    status          VARCHAR(20)    NULL COMMENT 'PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED',
    payment_method  VARCHAR(20)    NULL COMMENT 'COD',
    payment_status  VARCHAR(20)    NULL COMMENT 'PENDING, PAID, CANCELLED',
    order_date      DATETIME(6)    NULL,
    updated_at      DATETIME(6)    NULL,
    checkout_token  VARCHAR(64)    NULL COMMENT 'stops duplicate orders from a double submit',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_checkout_token (checkout_token),
    KEY idx_orders_user (user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_date (order_date),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. Order items (book name and unit price are snapshots at purchase time)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    book_id    BIGINT         NULL,
    book_name  VARCHAR(200)   NULL,
    quantity   INT            NOT NULL,
    price      DECIMAL(10,2)  NOT NULL COMMENT 'unit price when ordered',
    subtotal   DECIMAL(12,2)  NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_book (book_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES customer_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_book  FOREIGN KEY (book_id)  REFERENCES products (id),
    CONSTRAINT chk_order_items_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. Wishlists (one row per customer per book)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlists (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    book_id     BIGINT      NOT NULL,
    created_at  DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_user_book (user_id, book_id),
    KEY idx_wishlists_user (user_id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlists_book FOREIGN KEY (book_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. Reviews (one per customer per book, rating 1-5)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    book_id      BIGINT        NOT NULL,
    rating       INT           NOT NULL,
    review_text  VARCHAR(1000) NOT NULL,
    created_at   DATETIME(6)   NULL,
    updated_at   DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_user_book (user_id, book_id),
    KEY idx_reviews_book (book_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Sample catalogue: 4 original + 14 new books (inserted only if missing)
-- Covers: Open Library cover service by ISBN
-- ---------------------------------------------------------------------
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Atomic Habits', 'James Clear', 'A practical guide to building good habits and breaking bad ones through tiny, consistent changes that compound over time.', 'Self-Help', 399.00, 'https://covers.openlibrary.org/b/isbn/9780735211292-L.jpg', 25, 1, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Atomic Habits'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'The Alchemist', 'Paulo Coelho', 'A young shepherd travels from Spain to the Egyptian desert in search of treasure and learns to listen to his heart along the way.', 'Fiction', 299.00, 'https://covers.openlibrary.org/b/isbn/9780062315007-L.jpg', 30, 1, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('The Alchemist'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Ikigai', 'Héctor García', 'Explores the Japanese idea of a reason for being, drawing on the daily habits of some of the longest-living people in the world.', 'Self-Help', 249.00, 'https://covers.openlibrary.org/b/isbn/9780143130727-L.jpg', 20, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Ikigai'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Deep Work', 'Cal Newport', 'Argues that focused, distraction-free work is rare and valuable, and gives rules for training your attention.', 'Productivity', 349.00, 'https://covers.openlibrary.org/b/isbn/9781455586691-L.jpg', 18, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Deep Work'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Sapiens', 'Yuval Noah Harari', 'A sweeping history of humankind, from the first humans in Africa to the scientific and economic revolutions of today.', 'History', 499.00, 'https://covers.openlibrary.org/b/isbn/9780062316097-L.jpg', 15, 1, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Sapiens'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'The Psychology of Money', 'Morgan Housel', 'Short stories about how people actually think about money, and why behaviour matters more than intelligence when investing.', 'Finance', 349.00, 'https://covers.openlibrary.org/b/isbn/9780857197689-L.jpg', 22, 1, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('The Psychology of Money'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Rich Dad Poor Dad', 'Robert T. Kiyosaki', 'Contrasts the money lessons of two father figures and makes the case for financial literacy, assets and passive income.', 'Finance', 299.00, 'https://covers.openlibrary.org/b/isbn/9781612680194-L.jpg', 12, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Rich Dad Poor Dad'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Thinking, Fast and Slow', 'Daniel Kahneman', 'A Nobel-winning psychologist explains the two systems that drive how we think, and the biases that lead us astray.', 'Psychology', 599.00, 'https://covers.openlibrary.org/b/isbn/9780374533557-L.jpg', 8, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Thinking, Fast and Slow'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'To Kill a Mockingbird', 'Harper Lee', 'Seen through the eyes of young Scout Finch, a story of justice, prejudice and courage in a small town in the American South.', 'Classics', 399.00, 'https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg', 10, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('To Kill a Mockingbird'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT '1984', 'George Orwell', 'A chilling portrait of a surveillance state where Big Brother watches everyone and even thought can be a crime.', 'Classics', 249.00, 'https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg', 16, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('1984'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Pride and Prejudice', 'Jane Austen', 'Elizabeth Bennet and Mr Darcy clash over first impressions in Austen''s witty novel of manners, family and marriage.', 'Classics', 199.00, 'https://covers.openlibrary.org/b/isbn/9780141439518-L.jpg', 14, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Pride and Prejudice'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'The Great Gatsby', 'F. Scott Fitzgerald', 'The mysterious Jay Gatsby throws lavish parties on Long Island while chasing a dream from his past in the Jazz Age.', 'Classics', 229.00, 'https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg', 3, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('The Great Gatsby'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'The Hobbit', 'J.R.R. Tolkien', 'Bilbo Baggins leaves his comfortable home for an adventure with dwarves, a wizard and a dragon guarding a mountain of gold.', 'Fantasy', 449.00, 'https://covers.openlibrary.org/b/isbn/9780547928227-L.jpg', 11, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('The Hobbit'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'The Midnight Library', 'Matt Haig', 'Between life and death, Nora finds a library whose books let her try the other lives she might have lived.', 'Fiction', 399.00, 'https://covers.openlibrary.org/b/isbn/9780525559474-L.jpg', 9, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('The Midnight Library'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Clean Code', 'Robert C. Martin', 'A handbook of practices for writing readable, maintainable software, with many before-and-after code examples.', 'Technology', 699.00, 'https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg', 7, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Clean Code'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'The Pragmatic Programmer', 'Andrew Hunt & David Thomas', 'Timeless advice on the craft of programming, from avoiding duplication to taking responsibility for your career.', 'Technology', 749.00, 'https://covers.openlibrary.org/b/isbn/9780135957059-L.jpg', 4, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('The Pragmatic Programmer'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Zero to One', 'Peter Thiel', 'A startup founder''s notes on building companies that create something truly new instead of copying what exists.', 'Business', 399.00, 'https://covers.openlibrary.org/b/isbn/9780804139298-L.jpg', 13, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Zero to One'));
INSERT INTO products (name, author, description, category, price, image, stock_quantity, featured, active, created_at, updated_at)
SELECT 'Wings of Fire', 'A.P.J. Abdul Kalam', 'The autobiography of India''s Missile Man, from his childhood in Rameswaram to leading the country''s space and defence programmes.', 'Biography', 299.00, 'https://covers.openlibrary.org/b/isbn/9788173711466-L.jpg', 0, 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM products WHERE LOWER(name) = LOWER('Wings of Fire'));

-- Fill in details for the 4 books created by the first version (they had no category/stock)
UPDATE products SET category = 'Self-Help',    stock_quantity = IF(stock_quantity = 0, 25, stock_quantity), featured = 1 WHERE name = 'Atomic Habits' AND category IS NULL;
UPDATE products SET category = 'Fiction',      stock_quantity = IF(stock_quantity = 0, 30, stock_quantity), featured = 1 WHERE name = 'The Alchemist' AND category IS NULL;
UPDATE products SET category = 'Self-Help',    stock_quantity = IF(stock_quantity = 0, 20, stock_quantity)               WHERE name = 'Ikigai'        AND category IS NULL;
UPDATE products SET category = 'Productivity', stock_quantity = IF(stock_quantity = 0, 18, stock_quantity)               WHERE name = 'Deep Work'     AND category IS NULL;

-- ---------------------------------------------------------------------
-- OPTIONAL: upgrading a database created by the FIRST version of the project.
-- Hibernate "update" adds new columns but never changes existing column types.
-- These lines convert old DOUBLE money columns to exact DECIMAL. Run once if you like.
-- ---------------------------------------------------------------------
-- ALTER TABLE products        MODIFY price        DECIMAL(10,2) NOT NULL;
-- ALTER TABLE customer_orders MODIFY total_amount DECIMAL(12,2) NOT NULL;
-- ALTER TABLE order_items     MODIFY price        DECIMAL(10,2) NOT NULL;

-- ---------------------------------------------------------------------
-- Useful queries for the demo / viva
-- ---------------------------------------------------------------------
-- SELECT id, name, author, category, price, stock_quantity, active FROM products ORDER BY id;
-- SELECT id, name, email, role, enabled FROM app_users;              -- password hashes not selected
-- SELECT o.id, o.customer_name, o.total_amount, o.status, o.payment_method, o.order_date FROM customer_orders o ORDER BY o.id DESC;
-- SELECT * FROM order_items WHERE order_id = 1;

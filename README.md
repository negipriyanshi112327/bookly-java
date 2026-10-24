# BOOKLY – Online Bookstore (Java / Spring Boot)

BOOKLY is a database-connected online bookstore built for a college mini-project.
Customers browse and search books, save a wishlist, order with **Cash on Delivery**, track
orders and review books they received. Administrators manage books, stock, orders and customers.

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.4.5, Spring MVC |
| Frontend | Thymeleaf, HTML5, CSS3, vanilla JavaScript |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8 (`bookly_db`) |
| Security | Spring Security (form login, roles, CSRF), BCrypt |
| Validation | Jakarta Bean Validation |
| Tests | JUnit 5, Spring Boot Test, MockMvc, H2 (in-memory, tests only) |
| Build | Maven |

---

## 1. Prerequisites

- **JDK 21**
- **Maven 3.9+** (or VS Code with the *Extension Pack for Java* and *Spring Boot Extension Pack*, which can run Maven for you)
- **MySQL Server 8** and **MySQL Workbench**

## 2. Create the database (MySQL Workbench)

1. Open MySQL Workbench and connect to your local server (usually `root@localhost:3306`).
2. **File → Open SQL Script…** and choose `database/bookly_db.sql`.
3. Click the lightning icon (**Execute**). This creates `bookly_db`, all 6 tables and the 18 sample books.
   It is safe to run again – nothing is duplicated.

> Running the script is optional. The app uses `spring.jpa.hibernate.ddl-auto=update`, so on start-up it
> creates any missing tables/columns itself and inserts the sample books. It never drops your data.

Upgrading a database from the first version of the project? Start the app once – Hibernate adds the new
columns and the 4 old books get their category/stock filled in. Optionally run the three `ALTER TABLE`
lines at the bottom of `bookly_db.sql` to convert old `DOUBLE` money columns to `DECIMAL`.

## 3. Configure environment variables

No passwords are stored in the source code. Copy the example file and fill it in:

```bash
cp .env.example .env          # Windows: copy .env.example .env
```

| Variable | Meaning | Example |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/bookly_db?...` |
| `DB_USERNAME` | MySQL user | `root` |
| `DB_PASSWORD` | MySQL password | *(your password)* |
| `BOOKLY_ADMIN_EMAIL` | Administrator login email | `admin@bookly.local` |
| `BOOKLY_ADMIN_PASSWORD` | Administrator password (min. 8 chars) | *(choose one)* |
| `BOOKLY_ADMIN_NAME` | Name shown in the admin area | `Bookly Admin` |
| `BOOKLY_SEED` | Insert missing sample books on start-up | `true` |

`.env` is read automatically (`spring.config.import=optional:file:.env[.properties]`) when you run the app
from the project folder, and it is listed in `.gitignore`. You can also set the same names as real
environment variables (PowerShell: `$env:DB_PASSWORD="..."`).

> **Important:** the first version of `application.properties` contained a real MySQL password.
> It has been removed, but if this project was ever shared or pushed to GitHub, change that MySQL password.

## 4. Build and run

```bash
mvn spring-boot:run
```

Open **http://localhost:8080**. Admin area: **http://localhost:8080/admin/login**.

Other commands:

```bash
mvn test            # run the automated tests (uses in-memory H2, does not touch MySQL)
mvn clean package   # build target/bookly-0.0.1-SNAPSHOT.jar
java -jar target/bookly-0.0.1-SNAPSHOT.jar
```

## 5. Administrator setup

On every start-up `AdminInitializer` reads `BOOKLY_ADMIN_EMAIL` / `BOOKLY_ADMIN_PASSWORD`:

- if no account exists for that email, it creates an **ADMIN** account (password stored as a BCrypt hash);
- if the admin exists and the password changed, it updates the hash;
- if the variables are missing, no admin is created and a warning is logged.

Admins log in only at `/admin/login`; customers only at `/login`. An admin cannot use customer features
(cart, checkout, wishlist, reviews) and a customer cannot open any `/admin/**` page – this is enforced by
Spring Security on the server, not just by hiding buttons.

## 6. Demo walkthrough (for the viva)

1. Register a customer at `/register`, log in, search “orwell”, filter by *Classics*, sort by price.
2. Open a book, add 2 copies to the cart, change quantity with − / +, save another book to the wishlist.
3. Try **Book Compass** (`/book-compass`): category *Finance*, interest “money” → see the reasons.
4. Checkout → name, phone `9876543210`, address → **Place order** → confirmation page with order ID.
5. Log in as admin → **Orders** → move the order Pending → Confirmed → Shipped → Delivered.
   (Try Pending → Delivered: it is refused.) Check **Inventory**: stock went down.
6. As the customer, open the book → **Reviews** → post a 1–5 star review (only allowed after delivery).
7. Place another order and cancel it from **My Orders** → stock is restored.
8. Admin → **Books** → add a book with a cover URL, edit it, remove it (books that were ordered are archived).

## 7. Routes

| Method | Route | Who | Purpose |
|---|---|---|---|
| GET | `/` | everyone | Home, featured books, catalogue, search `q`, `category`, `sort` |
| GET | `/books/{id}` | everyone | Book detail |
| GET/POST | `/register` | guests | Registration |
| GET/POST | `/login` | guests | Customer login (POST handled by Spring Security) |
| POST | `/logout` | logged in | Logout, session invalidated |
| GET | `/cart` | guests, customers | Cart with server-side totals |
| POST | `/cart/add/{id}`, `/cart/update/{id}`, `/cart/remove/{id}` | guests, customers | Cart changes |
| GET/POST | `/checkout` | customers | Delivery details, place COD order |
| GET | `/orders/{id}/confirmation` | owner | Order confirmation |
| GET | `/orders`, `/orders/{id}` | owner | Order history and tracking |
| POST | `/orders/{id}/cancel` | owner | Cancel (Pending/Confirmed only) |
| GET | `/wishlist` | customers | Wishlist |
| POST | `/wishlist/add/{id}`, `/wishlist/remove/{id}`, `/wishlist/move-to-cart/{id}` | customers | Wishlist changes |
| GET | `/book-compass` | everyone | Rule-based recommendations |
| GET | `/books/{id}/reviews` | everyone | Reviews and average rating |
| POST | `/books/{id}/reviews`, `/books/{id}/reviews/delete` | verified purchasers | Write/edit/delete own review |
| GET/POST | `/admin/login`, POST `/admin/logout` | admin | Admin login/logout |
| GET | `/admin/dashboard` | admin | Statistics, recent orders, low stock |
| GET/POST | `/admin/books`, `/admin/books/new`, `/admin/books/{id}/edit`, `/admin/books/{id}`, `/admin/books/{id}/delete`, `/admin/books/{id}/restore` | admin | Book management |
| GET/POST | `/admin/customers`, `/admin/customers/{id}/status` | admin | Customer list, activate/deactivate |
| GET/POST | `/admin/orders`, `/admin/orders/{id}`, `/admin/orders/{id}/status` | admin | Order management |
| GET/POST | `/admin/inventory`, `/admin/inventory/{id}` | admin | Stock management |

All state-changing requests are POST forms with a CSRF token and use redirect-after-post.

## 8. Features and business rules

**Catalogue** – books come from MySQL; search by title or author ignores case; category filter; sort by
title or price; detail page with description, stock and rating; missing covers show a styled fallback.

**Cart** – stored in the session as `bookId → quantity` only. Prices and totals are always recalculated
from the database with `BigDecimal`. Quantity must be 1 … min(stock, 10); out-of-stock, archived or deleted
books cannot be added and are flagged/removed from the cart.

**Checkout (COD)** – one database transaction: re-reads prices, reserves stock with an atomic
`UPDATE … SET stock = stock - qty WHERE stock >= qty` (so two customers can never oversell the last copy),
saves the order and items with name/price snapshots, then clears the cart. Any failure rolls back everything
and keeps the cart. A one-time checkout token (unique column) stops double-submitted forms from creating two
orders. No online payment gateway is integrated.

**Order status** – `PENDING → CONFIRMED → SHIPPED → DELIVERED`; `PENDING/CONFIRMED → CANCELLED`.
Every other change is rejected. Cancelling returns stock; delivery marks the COD payment as PAID.
Customers can only see and cancel their own orders (someone else's order ID returns 404).

**Wishlist** – stored in MySQL (`wishlists`), unique per customer and book.

**Reviews** – 1–5 stars plus 10–1000 characters; only customers with a **delivered** order containing the book
can review; one review per customer per book (posting again edits it); customers can delete only their own.

**Book Compass** – transparent scoring, no AI: author match +4, category match +3, each interest keyword in
title/description/category +2, rated ≥ 4★ +1; skips out-of-stock books and books over budget; shows the
reasons; falls back to popular in-stock books when nothing matches.

**Admin** – dashboard statistics; add/edit/remove/restore books (books with past orders are archived instead
of deleted); search and filter; customer list without passwords, activate/deactivate accounts; order search
by ID/name/email, status updates; inventory with low-stock filter. Stock edits use an optimistic check, so an
admin cannot accidentally overwrite stock that an order just changed.

## 9. Database design

| Table | Key columns | Constraints |
|---|---|---|
| `app_users` | id, name, email, password (BCrypt), role, enabled, created_at, updated_at | email UNIQUE |
| `products` | id, name, author, description, category, price DECIMAL(10,2), image, stock_quantity, featured, active, timestamps | indexes on name, author, category |
| `customer_orders` | id, user_id → app_users, customer_name, phone, address, total_amount, status, payment_method, payment_status, order_date, checkout_token | checkout_token UNIQUE |
| `order_items` | id, order_id → customer_orders, book_id → products, book_name, quantity, price (unit), subtotal | |
| `wishlists` | id, user_id, book_id, created_at | UNIQUE (user_id, book_id) |
| `reviews` | id, user_id, book_id, rating, review_text, timestamps | UNIQUE (user_id, book_id) |

A separate `categories` table was not needed; category is a column on `products`.

## 10. Project structure

```text
bookly-java/
├── pom.xml
├── .env.example                 environment variables (copy to .env)
├── database/bookly_db.sql       MySQL schema + 18 sample books
└── src/
    ├── main/java/com/example/demo/
    │   ├── config/        AdminInitializer, AppConfig (BCrypt), DataInitializer (sample books)
    │   ├── controller/    Home, Auth, Cart, Order, Wishlist, Review, BookCompass, Admin, AdminBook, AdminOrder
    │   ├── dto/           form objects (registration, checkout, book, review) and view objects
    │   ├── exception/     business exceptions + GlobalExceptionHandler
    │   ├── model/         AppUser, Book, CustomerOrder, OrderItem, WishlistItem, Review + enums
    │   ├── repository/    Spring Data JPA repositories
    │   ├── security/      SecurityConfig, BooklyUserDetails, BooklyUserDetailsService
    │   └── service/       Auth, Book, Cart, Order, Wishlist, Review, BookCompass, Admin
    ├── main/resources/
    │   ├── application.properties
    │   ├── static/css/style.css, static/js/app.js
    │   └── templates/     22 pages + fragments (head, navbar, footer, book-card)
    └── test/              JUnit 5 + MockMvc tests, H2 test configuration
```

## 11. Automated tests

| # | Requirement | Test |
|---|---|---|
| 1 | Successful registration | `AuthServiceTest`, `AuthControllerTest.successfulRegistrationRedirectsToLogin` |
| 2 | Duplicate email rejected | `AuthServiceTest.duplicateEmailIsRejectedIgnoringCase`, `AuthControllerTest.duplicateEmailShowsFieldError` |
| 3 | Login valid / invalid | `AuthControllerTest` (customer, wrong password, deactivated, admin vs customer forms) |
| 4 | Password hash verification | `AuthServiceTest.registrationCreatesCustomerWithBcryptHash` |
| 5 | Search by title and author | `BookServiceTest` |
| 6 | Cart add/update/remove/totals | `CartServiceTest` |
| 7 | Checkout with valid details | `OrderServiceTest`, `WebFlowTest.checkoutFlowCreatesOrderAndClearsCart` |
| 8 | Insufficient stock rejected | `OrderServiceTest.insufficientStockRejectsCheckoutAndRollsBackEverything` |
| 9 | Order and item persistence | `OrderServiceTest.checkoutSavesOrderItemsAndReducesStock`, `priceSnapshotSurvivesCatalogueChanges` |
| 10 | Inventory after an order / cancel | `OrderServiceTest` |
| 11 | Other customers' orders blocked | `OrderServiceTest`, `WebFlowTest.customersCannotOpenOtherCustomersOrders` |
| 12 | Admin-only management | `WebFlowTest` (customer → 403, admin CRUD, admin blocked from customer features) |
| 13 | Wishlist duplicates | `WishlistAndReviewTest.wishlistPreventsDuplicates` |
| 14 | Review validation | `WishlistAndReviewTest` |
| 15 | Invalid status transitions | `OrderServiceTest.invalidStatusTransitionsAreRejected`, `WebFlowTest.adminStatusUpdateFollowsTransitionRules` |

`WebFlowTest` also renders every page through Thymeleaf, so template errors fail the build.

## 12. Troubleshooting

- **`Access denied for user 'root'@'localhost'`** – check `DB_USERNAME` / `DB_PASSWORD` in `.env`.
- **Admin login fails** – make sure `BOOKLY_ADMIN_EMAIL` and `BOOKLY_ADMIN_PASSWORD` are set and restart; the log
  says “Administrator account created/updated”. The email must not belong to a customer account.
- **Deleted sample books come back after restart** – set `BOOKLY_SEED=false` once the catalogue is set up.
- **Port 8080 in use** – add `server.port=8081` to `.env`.
- **Covers not showing** – covers load from Open Library; without internet the title fallback is shown.

## 13. Known limitations

- Payment is Cash on Delivery only; there is no online payment gateway.
- No email verification or password-reset email.
- A deactivated customer who is already logged in keeps their session until it ends, but cannot place orders,
  wishlist or review (the services re-check the account).
- `ddl-auto=update` is a development setting; a production deployment should use migrations (e.g. Flyway).

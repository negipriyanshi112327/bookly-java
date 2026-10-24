# BOOKLY – project status

## Verification status (read this first)

| Check | Result |
|---|---|
| Java syntax of all 67 source files (`javac` parse) | ✅ passed |
| Calls between project classes (getters, services, DTOs) cross-checked with `javac` | ✅ no errors found |
| Thymeleaf templates: tags and `${…}` expressions balanced | ✅ passed |
| `mvn compile` / `mvn test` against Spring Boot | ⚠️ **not run yet** – Maven Central was unreachable in the environment where this was written. Run `mvn test` first; it compiles everything and renders every page. |
| Running against MySQL `bookly_db` | ⚠️ **not run yet** – do the steps in README sections 2–4. |

## Feature checklist

| Feature | Implemented | Automated test |
|---|---|---|
| Home page, featured books, catalogue from MySQL | ✅ | WebFlowTest.publicPagesRender |
| Case-insensitive search by title/author, category filter, sort | ✅ | BookServiceTest |
| Book detail page (description, category, stock, rating, related) | ✅ | WebFlowTest |
| Loading / empty / error states, responsive layout | ✅ | – (manual) |
| Registration with confirm password, unique email, validation | ✅ | AuthServiceTest, AuthControllerTest |
| Login / logout, BCrypt, roles CUSTOMER / ADMIN, CSRF | ✅ | AuthControllerTest, WebFlowTest |
| Deactivated accounts cannot log in | ✅ | AuthControllerTest.deactivatedCustomerCannotLogin |
| Cart add / + / − / remove, server totals with BigDecimal, stock limits | ✅ | CartServiceTest |
| Wishlist in MySQL, no duplicates, move to cart, private per user | ✅ | WishlistAndReviewTest |
| Checkout (COD), validation, transaction, atomic stock reservation, rollback | ✅ | OrderServiceTest, WebFlowTest |
| Duplicate-submit protection (checkout token) | ✅ | OrderServiceTest, WebFlowTest |
| Order confirmation, history, detail, tracker, customer cancel | ✅ | OrderServiceTest, WebFlowTest |
| Order status transition rules, stock restore on cancel | ✅ | OrderServiceTest, WebFlowTest |
| Book Compass (rule-based, reasons, fallback) | ✅ | WebFlowTest.publicPagesRender (smoke) |
| Reviews 1–5★, verified purchase, one per user, edit/delete own | ✅ | WishlistAndReviewTest |
| Admin dashboard statistics | ✅ | WebFlowTest.adminPagesRender |
| Admin add / edit / remove (archive) / restore books, search, filter | ✅ | WebFlowTest, BookServiceTest |
| Admin customers: search, activate/deactivate, no passwords shown | ✅ | WebFlowTest.adminPagesRender (smoke) |
| Admin orders: search by ID/name/email, detail, status update | ✅ | WebFlowTest |
| Admin inventory, low-stock filter, no negative stock, concurrent-safe edits | ✅ | BookServiceTest |
| 18 sample books (4 original + 14 new) with covers, prices, authors | ✅ | – (DataInitializer + SQL script) |
| Admin credentials and DB password from environment / `.env` | ✅ | AuthControllerTest.adminLoginUsesHashedConfiguredPassword |

## Remaining / not included

- Online payment gateway (COD only, by design).
- Email verification and password reset by email.
- Database migrations tool (Flyway) – `ddl-auto=update` is used for development.

## Files modified (from the original project)

- `README.md`
- `pom.xml`
- `src/main/java/com/example/demo/DemoApplication.java`
- `src/main/java/com/example/demo/config/AdminInitializer.java`
- `src/main/java/com/example/demo/config/DataInitializer.java`
- `src/main/java/com/example/demo/controller/AdminController.java`
- `src/main/java/com/example/demo/controller/AuthController.java`
- `src/main/java/com/example/demo/controller/CartController.java`
- `src/main/java/com/example/demo/controller/HomeController.java`
- `src/main/java/com/example/demo/model/AppUser.java`
- `src/main/java/com/example/demo/model/Book.java`
- `src/main/java/com/example/demo/model/CustomerOrder.java`
- `src/main/java/com/example/demo/model/OrderItem.java`
- `src/main/java/com/example/demo/repository/BookRepository.java`
- `src/main/java/com/example/demo/repository/CustomerOrderRepository.java`
- `src/main/java/com/example/demo/repository/UserRepository.java`
- `src/main/resources/application.properties`
- `src/main/resources/static/css/style.css`
- `src/main/resources/templates/admin-dashboard.html`
- `src/main/resources/templates/admin-login.html`
- `src/main/resources/templates/cart.html`
- `src/main/resources/templates/index.html`
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/register.html`

Unchanged: `src/main/java/com/example/demo/config/AppConfig.java`.  
The original `target/` build-output folder was not kept (Maven recreates it).

## Files created

- `.env.example`
- `.gitignore`
- `database/bookly_db.sql`
- `src/main/java/com/example/demo/controller/AdminBookController.java`
- `src/main/java/com/example/demo/controller/AdminOrderController.java`
- `src/main/java/com/example/demo/controller/BookCompassController.java`
- `src/main/java/com/example/demo/controller/GlobalModelAttributes.java`
- `src/main/java/com/example/demo/controller/OrderController.java`
- `src/main/java/com/example/demo/controller/ReviewController.java`
- `src/main/java/com/example/demo/controller/SafeRedirect.java`
- `src/main/java/com/example/demo/controller/WishlistController.java`
- `src/main/java/com/example/demo/dto/BookForm.java`
- `src/main/java/com/example/demo/dto/CartLine.java`
- `src/main/java/com/example/demo/dto/CartView.java`
- `src/main/java/com/example/demo/dto/CheckoutForm.java`
- `src/main/java/com/example/demo/dto/CompassResult.java`
- `src/main/java/com/example/demo/dto/CustomerSummary.java`
- `src/main/java/com/example/demo/dto/DashboardStats.java`
- `src/main/java/com/example/demo/dto/RatingSummary.java`
- `src/main/java/com/example/demo/dto/Recommendation.java`
- `src/main/java/com/example/demo/dto/RegistrationForm.java`
- `src/main/java/com/example/demo/dto/ReviewForm.java`
- `src/main/java/com/example/demo/exception/BusinessException.java`
- `src/main/java/com/example/demo/exception/DuplicateEmailException.java`
- `src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`
- `src/main/java/com/example/demo/exception/InsufficientStockException.java`
- `src/main/java/com/example/demo/exception/InvalidStatusTransitionException.java`
- `src/main/java/com/example/demo/exception/NotFoundException.java`
- `src/main/java/com/example/demo/model/OrderStatus.java`
- `src/main/java/com/example/demo/model/PaymentMethod.java`
- `src/main/java/com/example/demo/model/PaymentStatus.java`
- `src/main/java/com/example/demo/model/Review.java`
- `src/main/java/com/example/demo/model/Role.java`
- `src/main/java/com/example/demo/model/WishlistItem.java`
- `src/main/java/com/example/demo/repository/ReviewRepository.java`
- `src/main/java/com/example/demo/repository/WishlistRepository.java`
- `src/main/java/com/example/demo/security/BooklyUserDetails.java`
- `src/main/java/com/example/demo/security/BooklyUserDetailsService.java`
- `src/main/java/com/example/demo/security/SecurityConfig.java`
- `src/main/java/com/example/demo/service/AdminService.java`
- `src/main/java/com/example/demo/service/AuthService.java`
- `src/main/java/com/example/demo/service/BookCompassService.java`
- `src/main/java/com/example/demo/service/BookService.java`
- `src/main/java/com/example/demo/service/CartService.java`
- `src/main/java/com/example/demo/service/OrderService.java`
- `src/main/java/com/example/demo/service/ReviewService.java`
- `src/main/java/com/example/demo/service/WishlistService.java`
- `src/main/resources/static/js/app.js`
- `src/main/resources/templates/access-denied.html`
- `src/main/resources/templates/admin-book-form.html`
- `src/main/resources/templates/admin-books.html`
- `src/main/resources/templates/admin-customers.html`
- `src/main/resources/templates/admin-inventory.html`
- `src/main/resources/templates/admin-order-detail.html`
- `src/main/resources/templates/admin-orders.html`
- `src/main/resources/templates/book-compass.html`
- `src/main/resources/templates/book-detail.html`
- `src/main/resources/templates/checkout.html`
- `src/main/resources/templates/error.html`
- `src/main/resources/templates/fragments/book-card.html`
- `src/main/resources/templates/fragments/footer.html`
- `src/main/resources/templates/fragments/head.html`
- `src/main/resources/templates/fragments/navbar.html`
- `src/main/resources/templates/order-confirmation.html`
- `src/main/resources/templates/order-detail.html`
- `src/main/resources/templates/order-history.html`
- `src/main/resources/templates/reviews.html`
- `src/main/resources/templates/wishlist.html`
- `src/test/java/com/example/demo/AuthControllerTest.java`
- `src/test/java/com/example/demo/AuthServiceTest.java`
- `src/test/java/com/example/demo/BookServiceTest.java`
- `src/test/java/com/example/demo/CartServiceTest.java`
- `src/test/java/com/example/demo/OrderServiceTest.java`
- `src/test/java/com/example/demo/TestData.java`
- `src/test/java/com/example/demo/WebFlowTest.java`
- `src/test/java/com/example/demo/WishlistAndReviewTest.java`
- `src/test/resources/application.properties`
- `docs/PROJECT_STATUS.md`

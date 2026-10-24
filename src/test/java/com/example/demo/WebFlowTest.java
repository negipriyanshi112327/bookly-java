package com.example.demo;

import com.example.demo.model.AppUser;
import com.example.demo.model.Book;
import com.example.demo.model.CustomerOrder;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CustomerOrderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.BooklyUserDetails;
import com.example.demo.service.CartService;
import com.example.demo.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end checks through Spring MVC + Spring Security + Thymeleaf:
 * access rules, page rendering, admin management and the checkout flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BookRepository books;
    @Autowired CustomerOrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired PasswordEncoder encoder;

    AppUser customer, otherCustomer;
    BooklyUserDetails customerLogin, otherLogin, adminLogin;
    Book book;

    @BeforeEach
    void setUp() {
        customer = TestData.customer(users, encoder);
        otherCustomer = TestData.customer(users, encoder);
        customerLogin = TestData.principal(customer);
        otherLogin = TestData.principal(otherCustomer);
        adminLogin = TestData.admin(users);
        book = TestData.book(books, "Sapiens", "Yuval Noah Harari", "History", "499.00", 10);
    }

    private CustomerOrder orderFor(AppUser owner) {
        return orderService.placeOrder(owner.getId(), TestData.checkoutForm(),
                Map.of(book.getId(), 1), UUID.randomUUID().toString());
    }

    // ---------- public pages render ----------

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
        mvc.perform(get("/").param("q", "sapiens").param("category", "History").param("sort", "price_desc"))
                .andExpect(status().isOk());
        mvc.perform(get("/books/{id}", book.getId())).andExpect(status().isOk()).andExpect(view().name("book-detail"));
        mvc.perform(get("/books/{id}/reviews", book.getId())).andExpect(status().isOk());
        mvc.perform(get("/book-compass").param("go", "1").param("category", "History"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("result"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
        mvc.perform(get("/cart")).andExpect(status().isOk());
    }

    @Test
    void unknownBookShowsNotFoundPage() throws Exception {
        mvc.perform(get("/books/{id}", 987654)).andExpect(status().isNotFound()).andExpect(view().name("error"));
    }

    // ---------- access restrictions ----------

    @Test
    void protectedCustomerPagesRequireLogin() throws Exception {
        mvc.perform(get("/orders")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?required"));
        mvc.perform(get("/checkout")).andExpect(redirectedUrl("/login?required"));
        mvc.perform(get("/wishlist")).andExpect(redirectedUrl("/login?required"));
        mvc.perform(post("/wishlist/add/{id}", book.getId()).with(csrf())).andExpect(redirectedUrl("/login?required"));
    }

    @Test
    void customerPagesRenderForCustomers() throws Exception {
        mvc.perform(get("/orders").with(user(customerLogin))).andExpect(status().isOk());
        mvc.perform(get("/wishlist").with(user(customerLogin))).andExpect(status().isOk());
        mvc.perform(get("/").with(user(customerLogin))).andExpect(status().isOk());
    }

    @Test
    void customersCannotOpenOtherCustomersOrders() throws Exception {
        CustomerOrder order = orderFor(customer);
        mvc.perform(get("/orders/{id}", order.getId()).with(user(customerLogin))).andExpect(status().isOk());
        mvc.perform(get("/orders/{id}", order.getId()).with(user(otherLogin))).andExpect(status().isNotFound());
        mvc.perform(post("/orders/{id}/cancel", order.getId()).with(user(otherLogin)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void customersCannotUseAdminPages() throws Exception {
        mvc.perform(get("/admin/dashboard").with(user(customerLogin))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/books").with(user(customerLogin)).with(csrf())
                        .param("name", "Hack").param("author", "X").param("category", "Y")
                        .param("price", "10").param("stockQuantity", "1"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/books")).andExpect(redirectedUrl("/admin/login?required"));
        assertThat(books.findFirstByNameIgnoreCase("Hack")).isEmpty();
    }

    @Test
    void adminsCannotUseCustomerFeatures() throws Exception {
        mvc.perform(post("/cart/add/{id}", book.getId()).with(user(adminLogin)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/checkout").with(user(adminLogin))).andExpect(status().isForbidden());
        mvc.perform(get("/orders").with(user(adminLogin))).andExpect(status().isForbidden());
        mvc.perform(get("/wishlist").with(user(adminLogin))).andExpect(status().isForbidden());
    }

    // ---------- admin management ----------

    @Test
    void adminPagesRender() throws Exception {
        CustomerOrder order = orderFor(customer);
        mvc.perform(get("/admin/dashboard").with(user(adminLogin))).andExpect(status().isOk());
        mvc.perform(get("/admin/books").with(user(adminLogin)).param("q", "sap")).andExpect(status().isOk());
        mvc.perform(get("/admin/books/new").with(user(adminLogin))).andExpect(status().isOk());
        mvc.perform(get("/admin/books/{id}/edit", book.getId()).with(user(adminLogin))).andExpect(status().isOk());
        mvc.perform(get("/admin/customers").with(user(adminLogin))).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").with(user(adminLogin)).param("q", String.valueOf(order.getId())))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/orders/{id}", order.getId()).with(user(adminLogin))).andExpect(status().isOk());
        mvc.perform(get("/admin/inventory").with(user(adminLogin))).andExpect(status().isOk());
        mvc.perform(get("/admin/inventory").param("low", "true").with(user(adminLogin))).andExpect(status().isOk());
    }

    @Test
    void adminCanAddAndRemoveBooks() throws Exception {
        mvc.perform(post("/admin/books").with(user(adminLogin)).with(csrf())
                        .param("name", "Zero to One").param("author", "Peter Thiel").param("category", "Business")
                        .param("price", "399").param("stockQuantity", "13")
                        .param("image", "https://covers.openlibrary.org/b/isbn/9780804139298-L.jpg"))
                .andExpect(redirectedUrl("/admin/books"));
        Book created = books.findFirstByNameIgnoreCase("Zero to One").orElseThrow();
        assertThat(created.getStockQuantity()).isEqualTo(13);

        mvc.perform(post("/admin/books/{id}/delete", created.getId()).with(user(adminLogin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/books"));
        assertThat(books.findById(created.getId())).isEmpty();
    }

    @Test
    void invalidBookFormIsRejected() throws Exception {
        mvc.perform(post("/admin/books").with(user(adminLogin)).with(csrf())
                        .param("name", "").param("author", "A").param("category", "C")
                        .param("price", "-5").param("stockQuantity", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-book-form"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "price", "stockQuantity"));
    }

    @Test
    void orderedBookIsArchivedNotDeleted() throws Exception {
        orderFor(customer);
        mvc.perform(post("/admin/books/{id}/delete", book.getId()).with(user(adminLogin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/books"));
        Book archived = books.findById(book.getId()).orElseThrow();
        assertThat(archived.isActive()).isFalse();
        mvc.perform(get("/books/{id}", book.getId())).andExpect(status().isNotFound());
    }

    @Test
    void adminStatusUpdateFollowsTransitionRules() throws Exception {
        CustomerOrder order = orderFor(customer);

        mvc.perform(post("/admin/orders/{id}/status", order.getId()).param("status", "DELIVERED")
                        .with(user(adminLogin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/orders/" + order.getId()))
                .andExpect(flash().attributeExists("error"));
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);

        mvc.perform(post("/admin/orders/{id}/status", order.getId()).param("status", "CONFIRMED")
                        .with(user(adminLogin)).with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ---------- full checkout through the web layer ----------

    @Test
    void checkoutFlowCreatesOrderAndClearsCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add/{id}", book.getId()).param("quantity", "2")
                        .session(session).with(user(customerLogin)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        MvcResult page = mvc.perform(get("/checkout").session(session).with(user(customerLogin)))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andReturn();
        String token = (String) page.getModelAndView().getModel().get("checkoutToken");

        MvcResult placed = mvc.perform(post("/checkout").session(session).with(user(customerLogin)).with(csrf())
                        .param("checkoutToken", token)
                        .param("customerName", "Test Reader")
                        .param("phone", "9876543210")
                        .param("address", "12 Test Street, Goregaon East, Mumbai 400063")
                        .param("paymentMethod", "COD"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = placed.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/orders/").endsWith("/confirmation");
        assertThat(CartService.itemCount(session)).isZero();
        assertThat(books.findStockById(book.getId())).contains(8);

        mvc.perform(get(location).session(session).with(user(customerLogin)))
                .andExpect(status().isOk()).andExpect(view().name("order-confirmation"));

        // submitting the same form again does not create a second order
        mvc.perform(post("/checkout").session(session).with(user(customerLogin)).with(csrf())
                        .param("checkoutToken", token)
                        .param("customerName", "Test Reader")
                        .param("phone", "9876543210")
                        .param("address", "12 Test Street, Goregaon East, Mumbai 400063")
                        .param("paymentMethod", "COD"))
                .andExpect(redirectedUrl(location));
        assertThat(books.findStockById(book.getId())).contains(8);
    }

    @Test
    void checkoutValidatesDeliveryDetails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(book.getId(), 1);
        session.setAttribute(CartService.SESSION_KEY, cart);
        session.setAttribute("checkoutToken", "token-123");

        mvc.perform(post("/checkout").session(session).with(user(customerLogin)).with(csrf())
                        .param("checkoutToken", "token-123")
                        .param("customerName", "")
                        .param("phone", "12345")
                        .param("address", "short")
                        .param("paymentMethod", "COD"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeHasFieldErrors("form", "customerName", "phone", "address"));
        assertThat(books.findStockById(book.getId())).contains(10);
    }
}

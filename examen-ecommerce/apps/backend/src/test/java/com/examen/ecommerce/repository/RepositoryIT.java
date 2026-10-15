package com.examen.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.entity.OrderItem;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.support.MySqlContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @DataJpaTest + Testcontainers mysql:5.7.44. Si el contexto arranca, ddl-auto=validate pasó
 * contra el esquema real (INT/DECIMAL/TINYINT UNSIGNED, CHAR(3)).
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryIT extends MySqlContainerSupport {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void schemaValidationPassesAndSeedIsReadable() {
        assertThat(categoryRepository.count()).isEqualTo(4);
        assertThat(productRepository.count()).isEqualTo(12);
        Coupon inactive = couponRepository.findByCode("INACTIVE10").orElseThrow();
        assertThat(inactive.isActive()).isFalse();
        assertThat(couponRepository.findByCode("WELCOME2026").orElseThrow().isActive()).isTrue();
        assertThat(categoryRepository.findById(1).orElseThrow().getName()).isEqualTo("Tecnología");
    }

    @Test
    void readsTheFourSeedOrdersWithItemsWithoutNPlusOne() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        statistics.clear();

        List<Order> orders = orderRepository.findAllWithItems();
        long itemCount = orders.stream().mapToLong(order -> order.getItems().stream()
                .map(OrderItem::getProductName).count()).sum();

        assertThat(orders).extracting(Order::getOrderNumber)
                .containsExactly("ORD-2026-000001", "ORD-2026-000002", "ORD-2026-000003", "ORD-2026-000004");
        assertThat(itemCount).isEqualTo(9);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        Order capped = orders.get(3);
        assertThat(capped.isCapApplied()).isTrue();
        assertThat(capped.getCapAdjustmentAmount()).isEqualByComparingTo("13.16");
        assertThat(capped.getCurrency()).isEqualTo("USD");
    }

    @Test
    void loadsProductsWithCategoryInOneQuery() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        statistics.clear();

        List<Product> products = productRepository.findAllWithCategoryByIdIn(List.of(4, 12, 10));
        products.forEach(product -> product.getCategory().getName());

        assertThat(products).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void atomicStockUpdate() {
        assertThat(productRepository.decrementStock(7, 3)).isZero();
        assertThat(productRepository.decrementStock(7, 2)).isEqualTo(1);
        assertThat(productRepository.decrementStock(5, 1)).isZero();
        entityManager.clear();
        assertThat(productRepository.findStockById(7)).contains(0);
    }

    @Test
    void filtersProductsAndFindsLastOrderNumber() {
        assertThat(productRepository.findAll(ProductSpecifications.withFilters(1, "mouse"), PageRequest.of(0, 10))
                .getContent()).extracting(Product::getName).containsExactly("Mouse inalámbrico");
        assertThat(productRepository.findAll(ProductSpecifications.withFilters(1, null), PageRequest.of(0, 20))
                .getTotalElements()).isEqualTo(6);
        assertThat(orderRepository.findLastOrderNumber("ORD-2026-")).contains("ORD-2026-000004");
        assertThat(orderRepository.findLastOrderNumber("ORD-2027-")).isEmpty();
        assertThat(orderRepository.findAll(OrderSpecifications.withFilters(
                LocalDateTime.of(2026, 10, 4, 0, 0), null, "TESTCAP30"), PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(orderRepository.existsByCouponId(1)).isTrue();
        assertThat(orderItemRepository.existsByProductId(2)).isTrue();
        assertThat(productRepository.existsByCategoryId(4)).isTrue();
    }

    @Test
    void uniqueConstraintsAreEnforced() {
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category("Hogar", BigDecimal.ZERO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueCouponCodeIsEnforced() {
        Coupon duplicated = new Coupon("WELCOME2026", BigDecimal.TEN, LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0), true);
        assertThatThrownBy(() -> couponRepository.saveAndFlush(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeysRestrictDeletes() {
        Category tecnologia = categoryRepository.findById(1).orElseThrow();
        assertThatThrownBy(() -> {
            categoryRepository.delete(tecnologia);
            categoryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}

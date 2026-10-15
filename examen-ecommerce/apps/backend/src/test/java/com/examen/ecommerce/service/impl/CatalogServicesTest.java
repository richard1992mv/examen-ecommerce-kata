package com.examen.ecommerce.service.impl;

import static com.examen.ecommerce.support.TestData.money;
import static com.examen.ecommerce.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.dto.request.CategoryRequest;
import com.examen.ecommerce.dto.request.CouponRequest;
import com.examen.ecommerce.dto.request.ProductRequest;
import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.ResourceConflictException;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.CategoryMapper;
import com.examen.ecommerce.mapper.CouponMapper;
import com.examen.ecommerce.mapper.ProductMapper;
import com.examen.ecommerce.repository.CategoryRepository;
import com.examen.ecommerce.repository.CouponRepository;
import com.examen.ecommerce.repository.OrderItemRepository;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.support.TestData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class CatalogServicesTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CouponRepository couponRepository = mock(CouponRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final Pageable page = PageRequest.of(0, 20);

    @Nested
    class Categories {

        private final CategoryServiceImpl service = new CategoryServiceImpl(categoryRepository, productRepository,
                new CategoryMapper());

        @Test
        void listsAndFinds() {
            when(categoryRepository.findAll(page)).thenReturn(new PageImpl<>(List.of(TestData.TECNOLOGIA), page, 1));
            when(categoryRepository.findById(1)).thenReturn(Optional.of(TestData.TECNOLOGIA));

            assertThat(service.findAll(page).content()).singleElement()
                    .satisfies(category -> assertThat(category.name()).isEqualTo("Tecnología"));
            assertThat(service.findById(1).discountPct()).isEqualByComparingTo("10.00");
        }

        @Test
        void missingCategoryIs404() {
            when(categoryRepository.findById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(99)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void createsAndRejectsDuplicates() {
            CategoryRequest request = new CategoryRequest(" Oficina ", money("5"));
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(service.create(request).name()).isEqualTo("Oficina");

            when(categoryRepository.existsByNameIgnoreCase("Hogar")).thenReturn(true);
            assertThatThrownBy(() -> service.create(new CategoryRequest("Hogar", money("0"))))
                    .isInstanceOf(ResourceConflictException.class);
        }

        @Test
        void updatesAndRejectsDuplicateName() {
            Category hogar = TestData.category(2, "Hogar", "0.00");
            when(categoryRepository.findById(2)).thenReturn(Optional.of(hogar));
            assertThat(service.update(2, new CategoryRequest("Casa", money("2.50"))).name()).isEqualTo("Casa");

            when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Libros", 2)).thenReturn(true);
            assertThatThrownBy(() -> service.update(2, new CategoryRequest("Libros", money("0"))))
                    .isInstanceOf(ResourceConflictException.class);
        }

        @Test
        void deleteIs409WhenItHasProducts() {
            Category hogar = TestData.category(2, "Hogar", "0.00");
            when(categoryRepository.findById(2)).thenReturn(Optional.of(hogar));
            when(productRepository.existsByCategoryId(2)).thenReturn(true);

            assertThatThrownBy(() -> service.delete(2)).isInstanceOf(ResourceConflictException.class);
            verify(categoryRepository, never()).delete(any());

            when(productRepository.existsByCategoryId(2)).thenReturn(false);
            service.delete(2);
            verify(categoryRepository).delete(hogar);
        }
    }

    @Nested
    class Products {

        private final ProductServiceImpl service = new ProductServiceImpl(productRepository, categoryRepository,
                orderItemRepository, new ProductMapper());

        @Test
        @SuppressWarnings("unchecked")
        void listsWithFilters() {
            when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(product(3)), page, 1));

            assertThat(service.findAll(1, "teclado", page).content()).singleElement()
                    .satisfies(found -> assertThat(found.categoryName()).isEqualTo("Tecnología"));
        }

        @Test
        void createsUpdatesAndFinds() {
            ProductRequest request = new ProductRequest("Webcam", money("35.00"), 1, 5);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(TestData.TECNOLOGIA));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(service.create(request).unitPrice()).isEqualByComparingTo("35.00");

            Product teclado = product(3);
            when(productRepository.findWithCategoryById(3)).thenReturn(Optional.of(teclado));
            assertThat(service.update(3, request).stock()).isEqualTo(5);
            assertThat(service.findById(3).name()).isEqualTo("Webcam");
        }

        @Test
        void unknownCategoryOrProductIs404() {
            when(categoryRepository.findById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.create(new ProductRequest("X", money("1"), 99, 1)))
                    .isInstanceOf(ResourceNotFoundException.class);
            when(productRepository.findWithCategoryById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(99)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void deleteIs409WhenItHasSales() {
            Product mouse = product(2);
            when(productRepository.findWithCategoryById(2)).thenReturn(Optional.of(mouse));
            when(orderItemRepository.existsByProductId(2)).thenReturn(true);
            assertThatThrownBy(() -> service.delete(2)).isInstanceOf(ResourceConflictException.class);

            when(orderItemRepository.existsByProductId(2)).thenReturn(false);
            service.delete(2);
            verify(productRepository).delete(mouse);
        }
    }

    @Nested
    class Coupons {

        private final CouponServiceImpl service = new CouponServiceImpl(couponRepository, orderRepository,
                new CouponMapper());
        private final CouponRequest request = new CouponRequest("BLACKFRIDAY", money("20"),
                LocalDateTime.of(2026, 11, 27, 0, 0), LocalDateTime.of(2026, 11, 30, 23, 59, 59), true);

        @Test
        void listsAndFindsByCode() {
            when(couponRepository.findAll(page)).thenReturn(new PageImpl<>(List.of(TestData.welcome2026()), page, 1));
            when(couponRepository.findByCode("WELCOME2026")).thenReturn(Optional.of(TestData.welcome2026()));
            when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

            assertThat(service.findAll(page).totalElements()).isEqualTo(1);
            assertThat(service.findByCode("WELCOME2026").active()).isTrue();
            assertThatThrownBy(() -> service.findByCode("NOPE")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void createsAndRejectsDuplicates() {
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(service.create(request).code()).isEqualTo("BLACKFRIDAY");

            when(couponRepository.existsByCode("BLACKFRIDAY")).thenReturn(true);
            assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResourceConflictException.class);
        }

        @Test
        void updatesAndRejectsDuplicateCode() {
            when(couponRepository.findById(3)).thenReturn(Optional.of(TestData.inactive10()));
            assertThat(service.update(3, request).discountPct()).isEqualByComparingTo("20.00");

            when(couponRepository.existsByCodeAndIdNot("BLACKFRIDAY", 3)).thenReturn(true);
            assertThatThrownBy(() -> service.update(3, request)).isInstanceOf(ResourceConflictException.class);
            when(couponRepository.findById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(99, request)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void deleteIs409WhenUsed() {
            Coupon welcome = TestData.welcome2026();
            when(couponRepository.findById(1)).thenReturn(Optional.of(welcome));
            when(orderRepository.existsByCouponId(1)).thenReturn(true);
            assertThatThrownBy(() -> service.delete(1)).isInstanceOf(ResourceConflictException.class);

            when(orderRepository.existsByCouponId(1)).thenReturn(false);
            service.delete(1);
            verify(couponRepository).delete(welcome);
        }
    }
}

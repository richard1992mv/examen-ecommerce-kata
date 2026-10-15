package com.examen.ecommerce.service.impl;

import static com.examen.ecommerce.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.InsufficientStockException;
import com.examen.ecommerce.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class StockServiceImplTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final StockServiceImpl service = new StockServiceImpl(repository);

    @Test
    void productFiveWithoutStockIsRejected() {
        assertThatThrownBy(() -> service.assertAvailable(List.of(new CartItemRequest(5, 1)), Map.of(5, product(5))))
                .isInstanceOfSatisfying(InsufficientStockException.class, ex -> {
                    assertThat(ex.getProductId()).isEqualTo(5);
                    assertThat(ex.getAvailable()).isZero();
                });
    }

    @Test
    void productSevenAcceptsTwoButNotThree() {
        Map<Integer, Product> seven = Map.of(7, product(7));
        assertThatThrownBy(() -> service.assertAvailable(List.of(new CartItemRequest(7, 3)), seven))
                .isInstanceOfSatisfying(InsufficientStockException.class, ex -> {
                    assertThat(ex.getRequested()).isEqualTo(3);
                    assertThat(ex.getAvailable()).isEqualTo(2);
                });
        assertThatCode(() -> service.assertAvailable(List.of(new CartItemRequest(7, 2)), seven))
                .doesNotThrowAnyException();
    }

    @Test
    void decrementUsesAtomicUpdateInProductIdOrder() {
        when(repository.decrementStock(1, 2)).thenReturn(1);
        when(repository.decrementStock(9, 1)).thenReturn(1);

        service.decrement(List.of(new CartItemRequest(9, 1), new CartItemRequest(1, 2)));

        InOrder order = inOrder(repository);
        order.verify(repository).decrementStock(1, 2);
        order.verify(repository).decrementStock(9, 1);
    }

    @Test
    void decrementWithoutAffectedRowsThrows409() {
        when(repository.decrementStock(7, 3)).thenReturn(0);
        when(repository.findStockById(7)).thenReturn(Optional.of(2));

        assertThatThrownBy(() -> service.decrement(List.of(new CartItemRequest(7, 3))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Stock insuficiente para el producto 7: solicitada 3, disponible 2");
    }
}

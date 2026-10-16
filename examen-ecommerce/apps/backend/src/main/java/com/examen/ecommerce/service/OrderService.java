package com.examen.ecommerce.service;

import com.examen.ecommerce.config.OrderNumberProperties;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.exception.OrderNumberGenerationException;
import com.examen.ecommerce.mapper.CheckoutMapper;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.util.OrderNumberGenerator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * HU3: checkout. Cada intento corre en UNA transacción: mismo cálculo que /cart/quote → UPDATE atómico
 * de stock → order_number → persistir. Si dos checkouts generan el mismo order_number, el UNIQUE
 * uk_orders_order_number lo rechaza y se reintenta (acotado) en una transacción nueva.
 */
@Service
public class OrderService {

    static final String ORDER_NUMBER_CONSTRAINT = "uk_orders_order_number";
    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final PricingService pricingService;
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final OrderNumberGenerator numberGenerator;
    private final CheckoutMapper mapper;
    private final TransactionTemplate transaction;
    private final int maxAttempts;
    private final Clock clock;

    public OrderService(PricingService pricingService, StockService stockService, OrderRepository orderRepository,
                        OrderNumberGenerator numberGenerator, CheckoutMapper mapper,
                        PlatformTransactionManager transactionManager, OrderNumberProperties properties,
                        Clock clock) {
        this.pricingService = pricingService;
        this.stockService = stockService;
        this.orderRepository = orderRepository;
        this.numberGenerator = numberGenerator;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(transactionManager);
        this.maxAttempts = properties.maxAttempts();
        this.clock = clock;
    }

    public OrderResponse checkout(CartRequest request) {
        DataIntegrityViolationException lastCollision = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return transaction.execute(status -> placeOrder(request));
            } catch (DataIntegrityViolationException ex) {
                if (!isOrderNumberCollision(ex)) {
                    throw ex;
                }
                lastCollision = ex;
                LOG.warn("Colisión de order_number en el intento {}/{}; reintentando", attempt, maxAttempts);
            }
        }
        throw new OrderNumberGenerationException(maxAttempts, lastCollision);
    }

    private OrderResponse placeOrder(CartRequest request) {
        PricedCart priced = pricingService.price(request);
        stockService.decrement(request.items());
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = mapper.toOrder(priced, nextOrderNumber(now.getYear()), now);
        return mapper.toOrderResponse(orderRepository.saveAndFlush(order));
    }

    private String nextOrderNumber(int year) {
        return numberGenerator.next(year, orderRepository.findLastOrderNumber(numberGenerator.yearPrefix(year)));
    }

    private static boolean isOrderNumberCollision(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(ORDER_NUMBER_CONSTRAINT);
    }
}

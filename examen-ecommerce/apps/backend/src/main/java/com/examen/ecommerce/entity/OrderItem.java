package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Tabla order_items con snapshots (nombre, categoría y precio al momento de la compra). */
@Entity
@Table(name = "order_items")
public class OrderItem extends IdentifiedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false, columnDefinition = UNSIGNED_INT)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false, columnDefinition = UNSIGNED_INT)
    private Product product;

    @Column(name = "product_name", nullable = false, length = 150, updatable = false)
    private String productName;

    @Column(name = "category_name", nullable = false, length = 100, updatable = false)
    private String categoryName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false, updatable = false, columnDefinition = UNSIGNED_INT)
    private Integer quantity;

    @Column(name = "line_subtotal", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal lineSubtotal;

    @Column(name = "category_discount_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal categoryDiscountAmount;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    /**
     * @param snapshot valores históricos copiados del producto y del cálculo
     */
    public OrderItem(Product product, ItemSnapshot snapshot) {
        this.product = product;
        this.productName = snapshot.productName();
        this.categoryName = snapshot.categoryName();
        this.unitPrice = snapshot.unitPrice();
        this.quantity = snapshot.quantity();
        this.lineSubtotal = snapshot.lineSubtotal();
        this.categoryDiscountAmount = snapshot.categoryDiscountAmount();
        this.lineTotal = snapshot.lineTotal();
    }

    void assignTo(Order owner) {
        this.order = owner;
    }

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public BigDecimal getCategoryDiscountAmount() {
        return categoryDiscountAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    @Override
    public String toString() {
        return "OrderItem{id=" + getId() + ", productName='" + productName + "', quantity=" + quantity + '}';
    }

    /** Valores inmutables de la línea al confirmar la compra. */
    public record ItemSnapshot(
            String productName,
            String categoryName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal lineSubtotal,
            BigDecimal categoryDiscountAmount,
            BigDecimal lineTotal) {
    }
}

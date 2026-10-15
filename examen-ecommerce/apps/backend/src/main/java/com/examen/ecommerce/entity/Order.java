package com.examen.ecommerce.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Tabla orders (inmutable: no hay setters públicos ni status en el esquema).
 * Nombre de entidad "PurchaseOrder" porque ORDER es palabra reservada en JPQL.
 */
@Entity(name = "PurchaseOrder")
@Table(name = "orders")
public class Order extends IdentifiedEntity {

    @Column(name = "order_number", nullable = false, length = 30, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "coupon_id", updatable = false, columnDefinition = UNSIGNED_INT)
    private Coupon coupon;

    @Column(name = "coupon_code", length = 50, updatable = false)
    private String couponCode;

    @Column(name = "subtotal_original", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal subtotalOriginal;

    @Column(name = "category_discount_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal categoryDiscountAmount;

    @Column(name = "volume_discount_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal volumeDiscountAmount;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal couponDiscountAmount;

    @Column(name = "cap_adjustment_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal capAdjustmentAmount;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal discountTotal;

    @Column(name = "effective_discount_pct", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal effectiveDiscountPct;

    @Column(name = "discount_cap_pct", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal discountCapPct;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "cap_applied", nullable = false, updatable = false)
    private boolean capApplied;

    @Column(name = "total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal total;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    /** Se fija desde el Clock inyectado (determinista en pruebas). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    private Order(Builder builder) {
        this.orderNumber = builder.orderNumber;
        this.coupon = builder.coupon;
        this.couponCode = builder.couponCode;
        this.subtotalOriginal = builder.subtotalOriginal;
        this.categoryDiscountAmount = builder.categoryDiscountAmount;
        this.volumeDiscountAmount = builder.volumeDiscountAmount;
        this.couponDiscountAmount = builder.couponDiscountAmount;
        this.capAdjustmentAmount = builder.capAdjustmentAmount;
        this.discountTotal = builder.discountTotal;
        this.effectiveDiscountPct = builder.effectiveDiscountPct;
        this.discountCapPct = builder.discountCapPct;
        this.capApplied = builder.capApplied;
        this.total = builder.total;
        this.currency = builder.currency;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Sincroniza ambos lados de la relación. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.assignTo(this);
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public BigDecimal getSubtotalOriginal() {
        return subtotalOriginal;
    }

    public BigDecimal getCategoryDiscountAmount() {
        return categoryDiscountAmount;
    }

    public BigDecimal getVolumeDiscountAmount() {
        return volumeDiscountAmount;
    }

    public BigDecimal getCouponDiscountAmount() {
        return couponDiscountAmount;
    }

    public BigDecimal getCapAdjustmentAmount() {
        return capAdjustmentAmount;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getEffectiveDiscountPct() {
        return effectiveDiscountPct;
    }

    public BigDecimal getDiscountCapPct() {
        return discountCapPct;
    }

    public boolean isCapApplied() {
        return capApplied;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public String toString() {
        return "Order{id=" + getId() + ", orderNumber='" + orderNumber + "', total=" + total + '}';
    }

    /** Construcción explícita de la cabecera (evita constructores con 15 parámetros). */
    public static final class Builder {

        private String orderNumber;
        private Coupon coupon;
        private String couponCode;
        private BigDecimal subtotalOriginal;
        private BigDecimal categoryDiscountAmount;
        private BigDecimal volumeDiscountAmount;
        private BigDecimal couponDiscountAmount;
        private BigDecimal capAdjustmentAmount;
        private BigDecimal discountTotal;
        private BigDecimal effectiveDiscountPct;
        private BigDecimal discountCapPct;
        private boolean capApplied;
        private BigDecimal total;
        private String currency;
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder orderNumber(String value) {
            this.orderNumber = value;
            return this;
        }

        public Builder coupon(Coupon value, String code) {
            this.coupon = value;
            this.couponCode = code;
            return this;
        }

        public Builder subtotalOriginal(BigDecimal value) {
            this.subtotalOriginal = value;
            return this;
        }

        public Builder categoryDiscountAmount(BigDecimal value) {
            this.categoryDiscountAmount = value;
            return this;
        }

        public Builder volumeDiscountAmount(BigDecimal value) {
            this.volumeDiscountAmount = value;
            return this;
        }

        public Builder couponDiscountAmount(BigDecimal value) {
            this.couponDiscountAmount = value;
            return this;
        }

        public Builder capAdjustmentAmount(BigDecimal value) {
            this.capAdjustmentAmount = value;
            return this;
        }

        public Builder discountTotal(BigDecimal value) {
            this.discountTotal = value;
            return this;
        }

        public Builder effectiveDiscountPct(BigDecimal value) {
            this.effectiveDiscountPct = value;
            return this;
        }

        public Builder discountCapPct(BigDecimal value) {
            this.discountCapPct = value;
            return this;
        }

        public Builder capApplied(boolean value) {
            this.capApplied = value;
            return this;
        }

        public Builder total(BigDecimal value) {
            this.total = value;
            return this;
        }

        public Builder currency(String value) {
            this.currency = value;
            return this;
        }

        public Builder createdAt(LocalDateTime value) {
            this.createdAt = value;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }
}

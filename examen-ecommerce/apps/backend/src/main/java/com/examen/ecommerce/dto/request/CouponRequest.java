package com.examen.ecommerce.dto.request;

import com.examen.ecommerce.dto.validation.DateRange;
import com.examen.ecommerce.dto.validation.Percentage;
import com.examen.ecommerce.dto.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entrada de cupones: valid_from ≤ valid_until y discount_pct entre 0 y 100. */
@ValidDateRange
public record CouponRequest(
        @Schema(example = "BLACKFRIDAY") @NotBlank @Size(max = 50) String code,
        @Schema(example = "20.00") @NotNull @Percentage BigDecimal discountPct,
        @Schema(example = "2026-11-27T00:00:00") @NotNull LocalDateTime validFrom,
        @Schema(example = "2026-11-30T23:59:59") @NotNull LocalDateTime validUntil,
        @Schema(example = "true") @NotNull Boolean active) implements DateRange {
}

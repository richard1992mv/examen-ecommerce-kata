package com.examen.ecommerce.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/** Envoltorio estable de paginación (no se serializa PageImpl directamente). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}

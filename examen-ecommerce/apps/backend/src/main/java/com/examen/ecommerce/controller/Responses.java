package com.examen.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Construcción común de 201 Created + Location y 204 No Content (DRY). */
final class Responses {

    private static final String ID_PATH = "/{id}";

    private Responses() {
    }

    static <T> ResponseEntity<T> created(Integer id, T body) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path(ID_PATH).buildAndExpand(id).toUri()).body(body);
    }

    static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}

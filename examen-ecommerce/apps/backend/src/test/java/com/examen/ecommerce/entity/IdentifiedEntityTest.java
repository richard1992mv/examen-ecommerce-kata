package com.examen.ecommerce.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Igualdad de entidades JPA por id (patrón recomendado para Hibernate): dos entidades sin persistir nunca son
 * iguales y el hashCode es estable antes y después de asignar el id. Prueba unitaria pura.
 */
@DisplayName("IdentifiedEntity · equals/hashCode por id")
class IdentifiedEntityTest {

    /** Entidades de prueba: subclases concretas, como Product o Coupon. */
    static class Item extends IdentifiedEntity {
    }

    static class OtherItem extends IdentifiedEntity {
    }

    private static <T extends IdentifiedEntity> T withId(T entity, Integer id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Test
    @DisplayName("Misma clase y mismo id → iguales y con el mismo hashCode")
    void mismoId() {
        Item a = withId(new Item(), 1);
        Item b = withId(new Item(), 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a);
    }

    @Test
    @DisplayName("Distinto id, distinta clase, null u otro tipo → distintos")
    void distintos() {
        Item a = withId(new Item(), 1);
        assertNotEquals(a, withId(new Item(), 2));
        assertNotEquals(a, withId(new OtherItem(), 1));
        assertNotEquals(null, a);
        assertNotEquals(a, "1");
    }

    @Test
    @DisplayName("Dos entidades sin id (no persistidas) nunca son iguales")
    void sinId() {
        assertNotEquals(new Item(), new Item());
    }

    @Test
    @DisplayName("El hashCode no cambia al asignar el id (seguro en HashSet antes de persistir)")
    void hashCodeEstable() {
        Item item = new Item();
        int before = item.hashCode();
        withId(item, 10);
        assertEquals(before, item.hashCode());
    }
}

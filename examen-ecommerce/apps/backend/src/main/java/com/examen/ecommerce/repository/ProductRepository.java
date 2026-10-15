package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    /** HU1: catálogo completo con su categoría en una sola consulta (sin N+1). */
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByIdAsc();

    /** Quote/checkout: productos del carrito + categoría en una sola consulta. */
    @Query("select p from Product p join fetch p.category where p.id in :ids")
    List<Product> findAllWithCategoryByIdIn(@Param("ids") Collection<Integer> ids);

    /** UPDATE atómico: stock >= :quantity evita sobreventa. Devuelve filas afectadas (0 = sin stock). */
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int decrementStock(@Param("id") Integer id, @Param("quantity") int quantity);

    @Query("select p.stock from Product p where p.id = :id")
    Optional<Integer> findStockById(@Param("id") Integer id);
}

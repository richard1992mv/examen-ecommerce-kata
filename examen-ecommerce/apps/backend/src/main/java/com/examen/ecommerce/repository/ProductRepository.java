package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    /** Listado paginado con la categoría en la misma consulta (sin N+1). */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Integer id);

    /** Checkout/quote: productos + categoría en UNA sola consulta. */
    @Query("select p from Product p join fetch p.category where p.id in :ids")
    List<Product> findAllWithCategoryByIdIn(@Param("ids") Collection<Integer> ids);

    boolean existsByCategoryId(Integer categoryId);

    /**
     * UPDATE atómico: la condición stock >= :quantity evita sobreventa bajo concurrencia.
     * Devuelve las filas afectadas (0 = stock insuficiente).
     */
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int decrementStock(@Param("id") Integer id, @Param("quantity") int quantity);

    @Query("select p.stock from Product p where p.id = :id")
    Optional<Integer> findStockById(@Param("id") Integer id);
}

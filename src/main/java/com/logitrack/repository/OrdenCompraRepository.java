package com.logitrack.repository;

import com.logitrack.model.OrdenCompra;
import com.logitrack.model.EstadoOrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    List<OrdenCompra> findByEstado(EstadoOrdenCompra estado);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM OrdenCompra o WHERE o.estado = :estado")
    BigDecimal sumTotalByEstado(@Param("estado") EstadoOrdenCompra estado);

    @Query("SELECT COUNT(o) FROM OrdenCompra o WHERE o.estado = :estado")
    Long countByEstado(@Param("estado") EstadoOrdenCompra estado);

    @Query("SELECT COUNT(o) FROM OrdenCompra o WHERE o.producto.id = :productoId AND o.estado IN :estados")
    Long countByProductoIdAndEstados(@Param("productoId") Long productoId, @Param("estados") List<EstadoOrdenCompra> estados);
}
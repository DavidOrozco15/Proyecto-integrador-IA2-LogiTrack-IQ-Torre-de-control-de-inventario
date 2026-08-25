package com.logitrack.repository;

import com.logitrack.model.MovimientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MovimientoDetalleRepository extends JpaRepository<MovimientoDetalle, Long> {

    @Query("SELECT COALESCE(SUM(md.cantidad), 0) FROM MovimientoDetalle md " +
           "JOIN md.movimiento m " +
           "WHERE md.producto.id = :productoId " +
           "AND m.tipoMovimiento = 'SALIDA' " +
           "AND m.fecha BETWEEN :inicio AND :fin")
    Integer sumSalidasPorProductoEnRango(@Param("productoId") Long productoId,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin);
}
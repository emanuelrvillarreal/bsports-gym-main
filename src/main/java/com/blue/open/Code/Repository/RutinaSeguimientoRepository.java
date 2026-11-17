package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.RutinaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de RutinaSeguimiento. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface RutinaSeguimientoRepository extends JpaRepository<RutinaSeguimiento, Long> {
}

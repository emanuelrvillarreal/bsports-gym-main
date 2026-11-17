package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.RutinaAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de RutinaAsignacion. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface RutinaAsignacionRepository extends JpaRepository<RutinaAsignacion, Long> {
}

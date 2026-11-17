package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.RutinaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de RutinaHistorial. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface RutinaHistorialRepository extends JpaRepository<RutinaHistorial, Long> {
}

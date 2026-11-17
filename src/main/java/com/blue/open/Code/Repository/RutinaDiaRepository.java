package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.RutinaDia;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de RutinaDia. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface RutinaDiaRepository extends JpaRepository<RutinaDia, Long> {
}

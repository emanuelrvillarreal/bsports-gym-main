package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Rutina;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de Rutina. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface RutinaRepository extends JpaRepository<Rutina, Long> {
}

package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.RutinaItem;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de RutinaItem. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface RutinaItemRepository extends JpaRepository<RutinaItem, Long> {
}

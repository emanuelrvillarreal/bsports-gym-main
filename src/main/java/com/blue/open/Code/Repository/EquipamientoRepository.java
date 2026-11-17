package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Equipamiento;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de Equipamiento. Las consultas por club viven en EquipamientoService. */
public interface EquipamientoRepository extends JpaRepository<Equipamiento, Long> {
}

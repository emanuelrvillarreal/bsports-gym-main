package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.EquipamientoMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de EquipamientoMantenimiento. Las consultas por club viven en EquipamientoService. */
public interface EquipamientoMantenimientoRepository extends JpaRepository<EquipamientoMantenimiento, Long> {
}

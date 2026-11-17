package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Ejercicio;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de Ejercicio. Las consultas con filtros viven en EntrenamientoService (siempre por club). */
public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {
}

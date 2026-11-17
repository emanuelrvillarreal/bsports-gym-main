package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.EvaluacionFisica;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de evaluaciones fisicas. Las consultas por club viven en EvaluacionFisicaService. */
public interface EvaluacionFisicaRepository extends JpaRepository<EvaluacionFisica, Long> {
}

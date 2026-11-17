package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de Torneo. Las consultas por club viven en CompetenciasService. */
public interface TorneoRepository extends JpaRepository<Torneo, Long> {
}

package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.TorneoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de TorneoParticipante. Las consultas por club viven en CompetenciasService. */
public interface TorneoParticipanteRepository extends JpaRepository<TorneoParticipante, Long> {
}

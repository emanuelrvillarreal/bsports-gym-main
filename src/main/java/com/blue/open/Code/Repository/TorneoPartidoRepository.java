package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.TorneoPartido;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de TorneoPartido. Las consultas por club viven en CompetenciasService. */
public interface TorneoPartidoRepository extends JpaRepository<TorneoPartido, Long> {
}

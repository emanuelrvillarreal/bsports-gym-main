package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.PartidoEstadistica;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de PartidoEstadistica. Las consultas por club viven en CompetenciasService. */
public interface PartidoEstadisticaRepository extends JpaRepository<PartidoEstadistica, Long> {
}

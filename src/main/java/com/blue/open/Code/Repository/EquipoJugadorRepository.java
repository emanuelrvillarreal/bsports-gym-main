package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.EquipoJugador;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de EquipoJugador. Las consultas por club viven en CompetenciasService. */
public interface EquipoJugadorRepository extends JpaRepository<EquipoJugador, Long> {
}

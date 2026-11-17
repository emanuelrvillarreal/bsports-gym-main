package com.blue.open.Code.Service.Competencias;

import com.blue.open.Code.Entity.DTO.CompetenciasDTO;
import com.blue.open.Code.Entity.Enums.PartidoEstadoEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tabla de posiciones: puntos configurables, solo partidos jugados, desempate por diferencia y a favor. */
class TablaPosicionesTest {

    private static final CompetenciasDTO.Participante A = new CompetenciasDTO.Participante("a", "Club A", "eq", true);
    private static final CompetenciasDTO.Participante B = new CompetenciasDTO.Participante("b", "Rival B", null, false);
    private static final CompetenciasDTO.Participante C = new CompetenciasDTO.Participante("c", "Rival C", null, false);

    private static CompetenciasDTO.Partido partido(CompetenciasDTO.Participante local, CompetenciasDTO.Participante visitante,
                                                   Integer tl, Integer tv, PartidoEstadoEnum estado) {
        CompetenciasDTO.Partido p = new CompetenciasDTO.Partido();
        p.setLocal(local);
        p.setVisitante(visitante);
        p.setTantosLocal(tl);
        p.setTantosVisitante(tv);
        p.setEstado(estado);
        return p;
    }

    @Test
    void sumaPuntosYOrdena() {
        List<CompetenciasDTO.Posicion> tabla = CompetenciasService.tabla(List.of(A, B, C), List.of(
                partido(A, B, 2, 0, PartidoEstadoEnum.JUGADO),
                partido(B, C, 1, 1, PartidoEstadoEnum.JUGADO),
                partido(C, A, 3, 1, PartidoEstadoEnum.JUGADO),
                partido(A, C, null, null, PartidoEstadoEnum.PROGRAMADO)), 3, 1, 0);

        // * A: 3 pts dif 0, C: 4 pts, B: 1 pt
        assertEquals("Rival C", tabla.get(0).getNombre());
        assertEquals(4, tabla.get(0).getPuntos());
        assertEquals("Club A", tabla.get(1).getNombre());
        assertEquals(2, tabla.get(1).getJugados());
        assertEquals(1, tabla.get(1).getGanados());
        assertEquals(1, tabla.get(1).getPerdidos());
        assertEquals(3, tabla.get(1).getFavor());
        assertEquals(3, tabla.get(1).getContra());
        assertEquals(1, tabla.get(2).getPuntos());
    }

    @Test
    void respetaLosPuntosDelTorneo() {
        List<CompetenciasDTO.Posicion> tabla = CompetenciasService.tabla(List.of(A, B), List.of(
                partido(A, B, 70, 65, PartidoEstadoEnum.JUGADO)), 2, 1, 1);
        assertEquals(2, tabla.get(0).getPuntos());
        assertEquals(1, tabla.get(1).getPuntos());
        assertEquals(5, tabla.get(0).getDiferencia());
    }

    @Test
    void ignoraPartidosSuspendidos() {
        List<CompetenciasDTO.Posicion> tabla = CompetenciasService.tabla(List.of(A, B), List.of(
                partido(A, B, 1, 0, PartidoEstadoEnum.SUSPENDIDO)), 3, 1, 0);
        assertEquals(0, tabla.get(0).getJugados());
    }
}

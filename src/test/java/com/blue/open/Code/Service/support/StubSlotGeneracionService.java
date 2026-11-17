package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Service.Actividades.SlotGeneracionService;

import java.time.LocalDate;

public class StubSlotGeneracionService extends SlotGeneracionService {

    private int regeneraciones;
    private Actividad ultimaActividadRegenerada;

    @Override
    public int generarVentanaCompleta(Actividad actividad, LocalDate desde) {
        return 0;
    }

    @Override
    public int regenerarTrasCambioHorarioODias(Actividad actividad) {
        regeneraciones++;
        ultimaActividadRegenerada = actividad;
        return 0;
    }

    @Override
    public boolean generarDiaExtension(Actividad actividad, LocalDate diaObjetivo) {
        return false;
    }

    public int getRegeneraciones() {
        return regeneraciones;
    }

    public Actividad getUltimaActividadRegenerada() {
        return ultimaActividadRegenerada;
    }
}

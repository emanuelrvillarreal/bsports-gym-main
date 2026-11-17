package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtros del reporte de movimientos economicos. Todos son opcionales y se combinan (AND).
 * Sin fechas se consulta todo el historial del club. La exportacion a Excel usa exactamente
 * estos mismos filtros.
 */
@Getter
@Setter
public class ReporteEconomicoRequestDTO {

    private LocalDate desde;
    private LocalDate hasta;

    /** INGRESO o EGRESO. Null = ambos. */
    private MovimientoTipoEnum tipo;

    /**
     * Procedencia: CUOTA_SOCIAL, CUOTA_ACTIVIDAD, AMENITY, EVENTO o MANUAL (otros ingresos y gastos
     * cargados a mano). Vacio = todas.
     */
    @Size(max = 5)
    private List<String> categorias;

    @Size(max = 36)
    private String actividadUuid;

    /** Socio: coincide como persona o como titular que pago. */
    @Size(max = 36)
    private String memberUuid;

    /** Busqueda por nombre, apellido o DNI del socio (persona o titular). */
    @Size(max = 100)
    private String socio;

    /** Categoria deportiva del socio. */
    @Size(max = 36)
    private String categoriaSocioUuid;

    @Size(max = 4)
    private List<MedioPagoEnum> mediosPago;

    /** Por defecto solo ACTIVO: un movimiento anulado no es dinero del club. */
    @Size(max = 2)
    private List<MovimientoEstadoEnum> estados;

    @Size(max = 36)
    private String registradoPorUuid;

    /** Texto contenido en el concepto. */
    @Size(max = 100)
    private String concepto;

    @DecimalMin("0")
    private BigDecimal importeMinimo;

    @DecimalMin("0")
    private BigDecimal importeMaximo;

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;
}

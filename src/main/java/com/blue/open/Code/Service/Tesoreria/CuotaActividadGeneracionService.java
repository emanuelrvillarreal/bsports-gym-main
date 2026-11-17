package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.CuotaActividadAltaResultDTO;
import com.blue.open.Code.Entity.DTO.CuotaActividadGeneracionResultDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.InscripcionActividadDescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;
import com.blue.open.Code.Service.Membresias.MembresiaCoberturaActividades;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera las cuotas de actividad según el puntero {@link InscripcionActividad#getProximaGeneracion()}.
 * <p>
 * La regla es una sola: mientras la próxima generación de la inscripción sea menor o igual a la
 * fecha pedida, se genera la cuota de esa fecha y el puntero avanza un período según la periodicidad
 * de la actividad. Cuota y puntero se guardan en la misma transacción, así que volver a correr no duplica.
 */
@Service
@Slf4j
public class CuotaActividadGeneracionService {

    private static final int STATUS_ACTIVE = 0;

    private static final List<MemberEstadoEnum> ESTADOS_ELEGIBLES = List.of(
            MemberEstadoEnum.ACTIVO,
            MemberEstadoEnum.MOROSO,
            MemberEstadoEnum.SUSPENDIDO
    );

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private InscripcionActividadDescuentoRepository inscripcionActividadDescuentoRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MembresiaCoberturaActividades coberturaPases;

    /** Genera, para todos los clubes activos, las cuotas pendientes hasta {@code hasta}. Lo usa el job diario. */
    public CuotaActividadGeneracionResultDTO generarHasta(LocalDate hasta) {
        List<Company> companies = companyRepository.findByStatus(STATUS_ACTIVE);

        CuotaActividadGeneracionResultDTO result = nuevoResultado(hasta);
        result.setCompaniesProcessed(companies.size());

        for (Company company : companies) {
            try {
                acumular(result, procesarCompanyEnTransaccion(company.getId(), hasta));
            } catch (Exception ex) {
                // Un club con datos inconsistentes no puede dejar sin facturar al resto.
                log.error("Falló la generación de cuotas de actividad club={} hasta={}",
                        company.getId(), hasta, ex);
            }
        }

        return result;
    }

    /** Genera las cuotas pendientes de un club hasta {@code hasta}. Lo usa la generación manual. */
    public CuotaActividadGeneracionResultDTO generarHastaParaCompany(Long companyId, LocalDate hasta) {
        if (hasta == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Fecha hasta requerida",
                    "CuotaActividadGeneracionService | hasta null");
        }

        CuotaActividadGeneracionResultDTO result = nuevoResultado(hasta);
        result.setCompaniesProcessed(1);
        acumular(result, procesarCompanyEnTransaccion(companyId, hasta));
        return result;
    }

    /**
     * Cuota de la inscripción recién creada, con fecha de generación igual a la de inscripción.
     * El alta deja el puntero un ciclo adelante, así que el job no vuelve a generar esta fecha.
     */
    @Transactional
    public CuotaActividadAltaResultDTO generarPorInscripcion(
            Long memberId, Long actividadId, Long inscripcionActividadId, LocalDate fechaInscripcion) {

        LocalDate fecha = fechaInscripcion != null ? fechaInscripcion : LocalDate.now();
        CuotaActividadAltaResultDTO result = new CuotaActividadAltaResultDTO();

        if (memberId == null || actividadId == null || inscripcionActividadId == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Member, actividad e inscripción requeridos",
                    "CuotaActividadGeneracionService | memberId, actividadId o inscripcionActividadId null");
        }

        Member persona = memberRepository.findByIdWithTitular(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "CuotaActividadGeneracionService | memberId: " + memberId));

        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "CuotaActividadGeneracionService | actividadId: " + actividadId));

        Company company = persona.getCompany();

        if (coberturaPases.cubre(persona.getId(), actividad.getId(), fecha)) {
            result.setGenerada(false);
            result.setCubiertaPorPase(true);
            return result;
        }

        // Reinscripción en el mismo período (se dio de baja y se volvió a anotar): ya tiene la cuota de esa
        // fecha, así que no se genera otra (chocaría con uk_cuotas_actividad_fecha_gen); se informa la existente.
        Cuota existente = cuotaRepository.findFirstByPersona_IdAndActividad_IdAndFechaGeneracionAndEstadoNot(
                persona.getId(), actividad.getId(), fecha, CuotaEstadoEnum.ANULADA).orElse(null);
        if (existente != null) {
            result.setGenerada(false);
            result.setYaExistia(true);
            result.setCuotaUuid(existente.getUuid());
            result.setVencimiento(existente.getVencimiento());
            result.setEstado(existente.getEstado());
            return result;
        }

        // Sin transacción aislada a propósito: la cuota debe ser atómica con el alta de la
        // inscripción. Si acá hubiera un conflicto de unicidad, la inscripción debe caerse
        // con ella en lugar de quedar sin cuota.
        Cuota cuota = Cuota.deActividad(company, persona, actividad, fecha,
                resolverDescuentosVigentes(inscripcionActividadId, fecha));
        Cuota saved = cuotaRepository.save(cuota);

        result.setGenerada(true);
        result.setCuotaUuid(saved.getUuid());
        result.setVencimiento(saved.getVencimiento());
        result.setEstado(saved.getEstado());
        return result;
    }

    private CuotaActividadGeneracionResultDTO nuevoResultado(LocalDate hasta) {
        CuotaActividadGeneracionResultDTO result = new CuotaActividadGeneracionResultDTO();
        result.setHasta(hasta);
        return result;
    }

    private void acumular(CuotaActividadGeneracionResultDTO result, CompanyStats stats) {
        if (stats != null) {
            result.setCuotasGeneradas(result.getCuotasGeneradas() + stats.generadas());
            result.setCuotasOmitidas(result.getCuotasOmitidas() + stats.omitidas());
        }
    }

    private CompanyStats procesarCompanyEnTransaccion(Long companyId, LocalDate hasta) {
        return transactionTemplate.execute(status -> procesarCompany(companyId, hasta));
    }

    private CompanyStats procesarCompany(Long companyId, LocalDate hasta) {
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return CompanyStats.VACIO;
        }

        List<InscripcionActividad> pendientes = inscripcionActividadRepository.findConGeneracionPendiente(
                companyId, ESTADOS_ELEGIBLES, hasta);
        if (pendientes.isEmpty()) {
            return CompanyStats.VACIO;
        }

        List<Cuota> cuotas = new ArrayList<>();
        int omitidas = 0;
        for (InscripcionActividad inscripcion : pendientes) {
            Member persona = inscripcion.getMember();
            Actividad actividad = inscripcion.getActividad();

            try {
                // Una cuota por cada ciclo vencido hasta la fecha pedida. El puntero se avanza en la
                // misma transacción que la cuota: si el commit falla (por ejemplo, por @Version), no
                // queda ni la cuota ni el puntero movido.
                while (!inscripcion.getProximaGeneracion().isAfter(hasta)) {
                    LocalDate fecha = inscripcion.getProximaGeneracion();
                    // Ciclo cubierto por un pase que incluye la actividad: no se cobra, pero el puntero avanza.
                    if (coberturaPases.cubre(persona.getId(), actividad.getId(), fecha)) {
                        inscripcion.avanzarProximaGeneracion();
                        continue;
                    }
                    cuotas.add(Cuota.deActividad(company, persona, actividad, fecha,
                            resolverDescuentosVigentes(inscripcion.getId(), fecha)));
                    inscripcion.avanzarProximaGeneracion();
                }
            } catch (CustomException ex) {
                // Actividad sin importe, socio sin titular: se omite esa inscripción, no el club.
                // El puntero queda en la cuota que falló, así que se reintenta en la próxima corrida.
                omitidas++;
                log.warn("No se pudo construir la cuota de actividad club={} persona={} actividad={}: {}",
                        companyId, persona.getId(), actividad.getId(), ex.getMessage());
            }
        }

        if (cuotas.isEmpty()) {
            return new CompanyStats(0, omitidas);
        }

        cuotaRepository.saveAll(cuotas);
        return new CompanyStats(cuotas.size(), omitidas);
    }

    /**
     * Descuentos recurrentes vigentes de una inscripción en {@code fecha}. La query ya
     * prefiltra por fecha y status, pero la vigencia real la decide
     * {@link InscripcionActividadDescuento#estaVigente} — no se confía ciegamente en el
     * filtro de la consulta.
     */
    private List<Descuento> resolverDescuentosVigentes(Long inscripcionActividadId, LocalDate fecha) {
        return inscripcionActividadDescuentoRepository
                .findVigentesByInscripcionActividadId(inscripcionActividadId, fecha).stream()
                .filter(asignacion -> asignacion.estaVigente(fecha))
                .map(InscripcionActividadDescuento::getDescuento)
                .toList();
    }

    private record CompanyStats(int generadas, int omitidas) {
        private static final CompanyStats VACIO = new CompanyStats(0, 0);
    }
}

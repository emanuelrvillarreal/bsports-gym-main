package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.CuotaSocialAltaResultDTO;
import com.blue.open.Code.Entity.DTO.CuotaSocialGeneracionResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera las cuotas sociales según el puntero {@link Member#getProximaGeneracionCuota()}.
 * <p>
 * La regla es una sola: mientras la próxima generación del socio sea menor o igual a la fecha
 * pedida, se genera la cuota de esa fecha y el puntero avanza un período según la periodicidad del club.
 * Cuota y puntero se guardan en la misma transacción, así que volver a correr no duplica.
 */
@Service
@Slf4j
public class CuotaSocialGeneracionService {

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
    private CuotaRepository cuotaRepository;

    @Autowired
    private MemberDescuentoRepository memberDescuentoRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /** Genera, para todos los clubes con cuota social, las cuotas pendientes hasta {@code hasta}. Lo usa el job diario. */
    public CuotaSocialGeneracionResultDTO generarHasta(LocalDate hasta) {
        List<Company> companies = companyRepository.findByCuotaSocialHabilitadaTrueAndStatus(STATUS_ACTIVE);

        CuotaSocialGeneracionResultDTO result = nuevoResultado(hasta);
        result.setCompaniesProcessed(companies.size());

        for (Company company : companies) {
            try {
                acumular(result, procesarCompanyEnTransaccion(company.getId(), hasta));
            } catch (Exception ex) {
                // Un club con datos inconsistentes no puede dejar sin facturar al resto.
                log.error("Falló la generación de cuotas sociales club={} hasta={}",
                        company.getId(), hasta, ex);
            }
        }

        return result;
    }

    /** Genera las cuotas sociales pendientes de un club hasta {@code hasta}. Lo usa la generación manual. */
    public CuotaSocialGeneracionResultDTO generarHastaParaCompany(Long companyId, LocalDate hasta) {
        if (hasta == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Fecha hasta requerida",
                    "CuotaSocialGeneracionService | hasta null");
        }

        CuotaSocialGeneracionResultDTO result = nuevoResultado(hasta);
        result.setCompaniesProcessed(1);
        acumular(result, procesarCompanyEnTransaccion(companyId, hasta));
        return result;
    }

    /**
     * Cuota del socio recién dado de alta, con fecha de generación igual a la de alta. Si se
     * genera, el puntero del socio avanza un ciclo, así que el job no vuelve a generar esta fecha.
     * <p>
     * A diferencia del alta de actividad (donde una actividad sin importe es un caso raro
     * bloqueado por validación previa), un club sin cuota social configurada es un estado
     * normal y frecuente. Por eso acá sí se pre-chequea antes de tocar el factory: así el
     * alta de un socio en un club que no usa cuota social nunca dispara una excepción que
     * tumbaría el alta completa.
     */
    @Transactional
    public CuotaSocialAltaResultDTO generarPorAlta(Long memberId, LocalDate fechaAlta) {
        LocalDate fecha = fechaAlta != null ? fechaAlta : LocalDate.now();
        CuotaSocialAltaResultDTO result = new CuotaSocialAltaResultDTO();

        if (memberId == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Member requerido",
                    "CuotaSocialGeneracionService | memberId null");
        }

        Member persona = memberRepository.findByIdWithTitular(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "CuotaSocialGeneracionService | memberId: " + memberId));

        Company company = persona.getCompany();

        if (!company.tieneCuotaSocialConfigurada()) {
            result.setOmitida(true);
            result.setMotivoOmitida("El club no tiene cuota social habilitada");
            return result;
        }

        // Sin transacción aislada a propósito: la cuota debe ser atómica con el alta del
        // socio. Si acá hubiera un conflicto de unicidad, el alta debe caerse con ella en
        // lugar de quedar sin cuota.
        Cuota cuota = Cuota.deSocial(company, persona, fecha, resolverDescuentosVigentes(memberId, fecha));
        Cuota saved = cuotaRepository.save(cuota);
        persona.avanzarProximaGeneracionCuota();

        result.setGenerada(true);
        result.setCuotaUuid(saved.getUuid());
        result.setVencimiento(saved.getVencimiento());
        result.setEstado(saved.getEstado());
        return result;
    }

    private CuotaSocialGeneracionResultDTO nuevoResultado(LocalDate hasta) {
        CuotaSocialGeneracionResultDTO result = new CuotaSocialGeneracionResultDTO();
        result.setHasta(hasta);
        return result;
    }

    private void acumular(CuotaSocialGeneracionResultDTO result, CompanyStats stats) {
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
        // tieneCuotaSocialConfigurada() es el único gate en el camino de
        // generarHastaParaCompany; el importe depende del rango etario de cada socio
        // y se valida por socio dentro de Cuota.deSocial().
        if (company == null || !company.tieneCuotaSocialConfigurada()) {
            return CompanyStats.VACIO;
        }

        List<Member> pendientes = memberRepository.findConGeneracionCuotaPendiente(
                companyId, ESTADOS_ELEGIBLES, hasta);
        if (pendientes.isEmpty()) {
            return CompanyStats.VACIO;
        }

        List<Cuota> cuotas = new ArrayList<>();
        int omitidas = 0;
        for (Member persona : pendientes) {
            try {
                // Una cuota por cada ciclo vencido hasta la fecha pedida. El puntero se avanza en la
                // misma transacción que la cuota, así que si el commit falla no queda ninguno de los dos.
                while (!persona.getProximaGeneracionCuota().isAfter(hasta)) {
                    LocalDate fecha = persona.getProximaGeneracionCuota();
                    cuotas.add(Cuota.deSocial(company, persona, fecha,
                            resolverDescuentosVigentes(persona.getId(), fecha)));
                    persona.avanzarProximaGeneracionCuota();
                }
            } catch (CustomException ex) {
                // Socio sin titular o sin rango: se omite ese socio, no el club. El puntero queda en la
                // cuota que falló, así que se reintenta en la próxima corrida.
                omitidas++;
                log.warn("No se pudo construir la cuota social club={} persona={}: {}",
                        companyId, persona.getId(), ex.getMessage());
            }
        }

        if (cuotas.isEmpty()) {
            return new CompanyStats(0, omitidas);
        }

        cuotaRepository.saveAll(cuotas);
        return new CompanyStats(cuotas.size(), omitidas);
    }

    /**
     * Descuentos recurrentes vigentes de un socio en {@code fecha}. La query ya prefiltra por
     * fecha y status, pero la vigencia real la decide {@link MemberDescuento#estaVigente} —
     * no se confía ciegamente en el filtro de la consulta.
     */
    private List<Descuento> resolverDescuentosVigentes(Long memberId, LocalDate fecha) {
        return memberDescuentoRepository.findVigentesByMemberId(memberId, fecha).stream()
                .filter(asignacion -> asignacion.estaVigente(fecha))
                .map(MemberDescuento::getDescuento)
                .toList();
    }

    private record CompanyStats(int generadas, int omitidas) {
        private static final CompanyStats VACIO = new CompanyStats(0, 0);
    }
}

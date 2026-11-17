package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.DTO.AsistenciaLoteCommand;
import com.blue.open.Code.Entity.DTO.AsistenciaLoteItemCommand;
import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AsistenciaActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaLoteSaveServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ProfesorAuthService profesorAuthService;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private AsistenciaActividadRepository asistenciaActividadRepository;

    private AsistenciaLoteSaveService service;
    private StubAuthService stubAuthService;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        AsistenciaAutorizacionService autorizacionService = new AsistenciaAutorizacionService();
        ReflectionTestUtils.setField(autorizacionService, "authService", stubAuthService);
        ReflectionTestUtils.setField(autorizacionService, "slotRepository", slotRepository);
        ReflectionTestUtils.setField(autorizacionService, "actividadProfesorRepository", actividadProfesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(autorizacionService, "profesorAuthService", profesorAuthService);

        AsistenciaPlanillaQueryService planillaQueryService = new AsistenciaPlanillaQueryService();
        ReflectionTestUtils.setField(planillaQueryService, "authService", stubAuthService);
        ReflectionTestUtils.setField(planillaQueryService, "autorizacionService", autorizacionService);
        ReflectionTestUtils.setField(planillaQueryService, "inscripcionActividadRepository", inscripcionActividadRepository);
        ReflectionTestUtils.setField(planillaQueryService, "asistenciaActividadRepository", asistenciaActividadRepository);
        ReflectionTestUtils.setField(planillaQueryService, "asistenciaActividadMapper", new AsistenciaActividadMapper());

        service = new AsistenciaLoteSaveService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "autorizacionService", autorizacionService);
        ReflectionTestUtils.setField(service, "planillaQueryService", planillaQueryService);
        ReflectionTestUtils.setField(service, "asistenciaActividadRepository", asistenciaActividadRepository);
    }

    // CA-03: lote parcial válido persiste idempotente
    @Test
    void guardarLote_loteParcialValido_persisteYDevuelvePlanilla() {
        Slot slot = slotBase();
        configurarProfesorAsignado();
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(10L, 1L))
                .thenReturn(List.of(inscripcion("member-1", 1L)));
        when(asistenciaActividadRepository.findBySlotIdAndMemberIdIn(100L, List.of(1L)))
                .thenReturn(List.of());
        when(asistenciaActividadRepository.findBySlotIdWithMember(100L)).thenReturn(List.of());

        AsistenciaLoteCommand command = new AsistenciaLoteCommand();
        command.setItems(List.of(item("member-1", AsistenciaActividadEstadoEnum.PRESENTE)));

        service.guardarLote("slot-uuid", command);

        ArgumentCaptor<List<AsistenciaActividad>> captor = ArgumentCaptor.forClass(List.class);
        verify(asistenciaActividadRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(AsistenciaActividadEstadoEnum.PRESENTE, captor.getValue().get(0).getEstado());
    }

    // CA-03: member fuera de planilla rechaza toda la operación
    @Test
    void guardarLote_memberFueraDePlanilla_rechazaTodo() {
        Slot slot = slotBase();
        configurarProfesorAsignado();
        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(10L, 1L))
                .thenReturn(List.of(inscripcion("member-1", 1L)));

        AsistenciaLoteCommand command = new AsistenciaLoteCommand();
        command.setItems(List.of(item("member-otro", AsistenciaActividadEstadoEnum.AUSENTE)));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.guardarLote("slot-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(asistenciaActividadRepository, never()).saveAll(anyList());
    }

    // CA-03: member repetido rechaza toda la operación
    @Test
    void guardarLote_memberRepetido_rechazaTodo() {
        AsistenciaLoteCommand command = new AsistenciaLoteCommand();
        command.setItems(List.of(
                item("member-1", AsistenciaActividadEstadoEnum.PRESENTE),
                item("member-1", AsistenciaActividadEstadoEnum.AUSENTE)));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.guardarLote("slot-uuid", command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(asistenciaActividadRepository, never()).saveAll(anyList());
    }

    // CA-03/CA-04: corrección PRESENTE↔AUSENTE actualiza existente
    @Test
    void guardarLote_corrigeRegistroExistente() {
        Slot slot = slotBase();
        configurarProfesorAsignado();
        AsistenciaActividad existente = new AsistenciaActividad();
        existente.setMember(member("member-1", 1L));
        existente.setEstado(AsistenciaActividadEstadoEnum.PRESENTE);

        when(slotRepository.findByUuidAndCompanyIdWithActividad("slot-uuid", 1L)).thenReturn(Optional.of(slot));
        when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(10L, 1L))
                .thenReturn(List.of(inscripcion("member-1", 1L)));
        when(asistenciaActividadRepository.findBySlotIdAndMemberIdIn(100L, List.of(1L)))
                .thenReturn(List.of(existente));
        when(asistenciaActividadRepository.findBySlotIdWithMember(100L)).thenReturn(List.of(existente));

        AsistenciaLoteCommand command = new AsistenciaLoteCommand();
        command.setItems(List.of(item("member-1", AsistenciaActividadEstadoEnum.AUSENTE)));

        service.guardarLote("slot-uuid", command);

        ArgumentCaptor<List<AsistenciaActividad>> captor = ArgumentCaptor.forClass(List.class);
        verify(asistenciaActividadRepository).saveAll(captor.capture());
        assertEquals(AsistenciaActividadEstadoEnum.AUSENTE, captor.getValue().get(0).getEstado());
    }

    private void configurarProfesorAsignado() {
        Profesor profesor = new Profesor();
        profesor.setId(5L);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        when(profesorAuthService.getCurrentProfesorOrThrow(1L)).thenReturn(profesor);
        when(profesorAuthService.requirePortalEligible(profesor)).thenReturn(profesor);
        when(actividadProfesorRepository.existsByActividadIdAndProfesorIdAndStatus(10L, 5L, 0)).thenReturn(true);
    }

    private Slot slotBase() {
        Company company = new Company();
        company.setId(1L);

        Actividad actividad = new Actividad();
        actividad.setId(10L);
        actividad.setStatus(0);

        Slot slot = new Slot();
        slot.setId(100L);
        slot.setUuid("slot-uuid");
        slot.setCompany(company);
        slot.setActividad(actividad);
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slot.setInicio(LocalDateTime.now());
        slot.setFin(LocalDateTime.now().plusHours(1));
        return slot;
    }

    private InscripcionActividad inscripcion(String memberUuid, Long memberId) {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setMember(member(memberUuid, memberId));
        inscripcion.setFechaInscripcion(LocalDate.now().minusMonths(1));
        return inscripcion;
    }

    private Member member(String uuid, Long id) {
        Member member = new Member();
        member.setUuid(uuid);
        member.setId(id);
        return member;
    }

    private AsistenciaLoteItemCommand item(String memberUuid, AsistenciaActividadEstadoEnum estado) {
        AsistenciaLoteItemCommand item = new AsistenciaLoteItemCommand();
        item.setMemberUuid(memberUuid);
        item.setEstado(estado);
        return item;
    }
}

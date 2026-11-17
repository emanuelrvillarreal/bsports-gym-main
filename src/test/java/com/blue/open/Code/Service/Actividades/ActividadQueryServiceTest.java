package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.DTO.ActividadHorarioDTO;
import com.blue.open.Code.Entity.DTO.ActividadPaginateDTO;
import com.blue.open.Code.Entity.DTO.ActividadPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.ActividadMapper;
import com.blue.open.Code.Mapper.CategoriaMapper;
import com.blue.open.Code.Mapper.ProfesorMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadQueryServiceTest {

  private static final Long COMPANY_ID = 10L;

  @Mock
  private ActividadRepository actividadRepository;

  @Mock
  private ActividadProfesorRepository actividadProfesorRepository;

  @Mock
  private ProfesorRepository profesorRepository;

  @Mock
  private InscripcionActividadRepository inscripcionActividadRepository;

  @Mock
  private ActividadHorariosService actividadHorariosService;

  private ActividadQueryService service;

  @BeforeEach
  void setUp() {
    service = new ActividadQueryService();
    ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
    ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
    ReflectionTestUtils.setField(service, "profesorRepository", profesorRepository);
    ReflectionTestUtils.setField(service, "inscripcionActividadRepository", inscripcionActividadRepository);
    ReflectionTestUtils.setField(service, "actividadHorariosService", actividadHorariosService);
    ActividadMapper actividadMapper = new ActividadMapper();
    ReflectionTestUtils.setField(actividadMapper, "profesorMapper", new ProfesorMapper());
    ReflectionTestUtils.setField(actividadMapper, "categoriaMapper", new CategoriaMapper());
    ReflectionTestUtils.setField(actividadMapper, "actividadHorariosService", actividadHorariosService);
    ReflectionTestUtils.setField(service, "actividadMapper", actividadMapper);
  }

  @SuppressWarnings("unchecked")
  @Test
  void paginar_devuelveDtoLivianoConCategoriaYCantidadInscriptos() {
    Categoria categoria = new Categoria();
    categoria.setUuid("cat-uuid");
    categoria.setNombre("Infantil");

    Actividad actividad = new Actividad();
    actividad.setId(1L);
    actividad.setUuid("act-uuid");
    actividad.setNombre("Futbol");
    actividad.setUbicacion("Cancha 1");
    actividad.setCupoMaximo(20);
    actividad.setDiasSemana("LUNES");
    actividad.setCompleta(false);
    actividad.getCategorias().add(categoria);

    when(actividadRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(actividad)));
    List<Object[]> conteos = new java.util.ArrayList<>();
    conteos.add(new Object[] {1L, 4L});
    when(inscripcionActividadRepository.countActivasByActividadIdIn(List.of(1L)))
        .thenReturn(conteos);
    ActividadHorarioDTO horarioDto = new ActividadHorarioDTO();
    horarioDto.setUuid("hor-lunes");
    when(actividadHorariosService.obtenerPorActividad(List.of(1L)))
        .thenReturn(Map.of(1L, List.of(horarioDto)));

    Page<ActividadPaginateDTO> result = service.paginar(new ActividadPaginateRequestDTO(), COMPANY_ID);

    assertEquals(1, result.getTotalElements());
    ActividadPaginateDTO dto = result.getContent().get(0);
    assertEquals("Futbol", dto.getNombre());
    assertEquals(List.of("Infantil"), dto.getCategoriaNombres());
    assertEquals(4L, dto.getCantidadInscriptos());
    assertEquals(List.of(horarioDto), dto.getHorarios());
  }

  @Test
  void findByUuid_devuelveActividadConProfesoresEInscriptos() {
    Actividad actividad = new Actividad();
    actividad.setId(1L);
    actividad.setUuid("act-uuid");
    actividad.setNombre("Futbol");

    Profesor profesor = new Profesor();
    profesor.setId(3L);
    profesor.setUuid("prof-uuid");
    User profesorUser = new User();
    profesorUser.setNombre("Juan");
    profesorUser.setApellido("Perez");
    profesorUser.setImageUrl("https://cdn.example/profesor.jpg");
    profesor.setUser(profesorUser);

    ActividadProfesor link = new ActividadProfesor();
    link.setActividadId(1L);
    link.setProfesorId(3L);

    Member member = new Member();
    member.setId(5L);
    member.setUuid("member-uuid");
    User memberUser = new User();
    memberUser.setNombre("Ana");
    memberUser.setApellido("Gomez");
    memberUser.setEmail("ana@test.com");
    memberUser.setDni("40123456");
    memberUser.setUuid("user-uuid");
    memberUser.setFechaNacimiento(LocalDate.of(2014, 5, 17));
    member.setUser(memberUser);

    Member titular = new Member();
    titular.setUuid("titular-member-uuid");
    member.setTitular(titular);

    ActividadHorario horario = new ActividadHorario();
    horario.setId(7L);
    horario.setUuid("hor-lunes");

    InscripcionActividad inscripcion = new InscripcionActividad();
    inscripcion.setMember(member);
    inscripcion.getHorarios().add(horario);

    when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("act-uuid", COMPANY_ID))
        .thenReturn(Optional.of(actividad));
    when(actividadProfesorRepository.findActivasByActividadIdIn(List.of(1L))).thenReturn(List.of(link));
    when(profesorRepository.findByIdInWithUser(List.of(3L))).thenReturn(List.of(profesor));
    when(inscripcionActividadRepository.findActivasByActividadIdAndCompanyIdWithMember(1L, COMPANY_ID))
        .thenReturn(List.of(inscripcion));

    ActividadDTO dto = service.findByUuid("act-uuid", COMPANY_ID);

    assertEquals("act-uuid", dto.getActividadUuid());
    assertEquals(1, dto.getProfesores().size());
    assertEquals("Juan", dto.getProfesores().get(0).getNombre());
    assertEquals("https://cdn.example/profesor.jpg", dto.getProfesores().get(0).getImageUrl());
    assertEquals(1, dto.getInscriptos().size());
    assertEquals("Ana", dto.getInscriptos().get(0).getNombre());
    assertEquals("ana@test.com", dto.getInscriptos().get(0).getEmail());
    assertEquals("user-uuid", dto.getInscriptos().get(0).getUserUuid());
    assertEquals("titular-member-uuid", dto.getInscriptos().get(0).getTitularMemberUuid());
    assertEquals(LocalDate.of(2014, 5, 17), dto.getInscriptos().get(0).getFechaNacimiento());
    assertEquals(List.of("hor-lunes"), dto.getInscriptos().get(0).getHorarioUuids());
  }

  @Test
  void findByUuid_lanzaNotFoundSiNoExiste() {
    when(actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria("otro-uuid", COMPANY_ID))
        .thenReturn(Optional.empty());

    CustomException exception = assertThrows(CustomException.class,
        () -> service.findByUuid("otro-uuid", COMPANY_ID));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }
}

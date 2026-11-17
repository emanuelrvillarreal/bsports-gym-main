package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.MovimientoManualCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Mapper.MovimientoMapper;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoManualServiceTest {

    @Mock
    private MovimientoRepository movimientoRepository;

    private MovimientoManualService service;
    private final StubAuthService stubAuthService = new StubAuthService();

    @BeforeEach
    void setUp() {
        service = new MovimientoManualService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "movimientoRepository", movimientoRepository);
        ReflectionTestUtils.setField(service, "movimientoMapper", new MovimientoMapper());
    }

    @Test
    void registrar_ingreso_requiereCollect() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
        when(movimientoRepository.save(any())).thenAnswer(inv -> {
            Movimiento m = inv.getArgument(0);
            m.setUuid("mov-1");
            return m;
        });

        MovimientoManualCommand cmd = new MovimientoManualCommand();
        cmd.setTipo(MovimientoTipoEnum.INGRESO);
        cmd.setMonto(new BigDecimal("50"));
        cmd.setConcepto("Donación");
        cmd.setMedioPago(MedioPagoEnum.EFECTIVO);

        service.registrar(cmd);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(MovimientoEstadoEnum.ACTIVO, captor.getValue().getEstado());
        assertEquals(MovimientoTipoEnum.INGRESO, captor.getValue().getTipo());
    }

    @Test
    void registrar_ingreso_sinMedioPago_falla() {
        Company company = new Company();
        stubAuthService.setCompany(company);

        MovimientoManualCommand cmd = new MovimientoManualCommand();
        cmd.setTipo(MovimientoTipoEnum.INGRESO);
        cmd.setMonto(new BigDecimal("50"));
        cmd.setConcepto("Donación");

        CustomException ex = assertThrows(CustomException.class, () -> service.registrar(cmd));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void registrar_egreso_usaExpense() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MovimientoManualCommand cmd = new MovimientoManualCommand();
        cmd.setTipo(MovimientoTipoEnum.EGRESO);
        cmd.setMonto(new BigDecimal("100"));
        cmd.setConcepto("Gastos varios");

        service.registrar(cmd);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(MovimientoTipoEnum.EGRESO, captor.getValue().getTipo());
    }

    private static class StubAuthService extends AuthService {

        private Company company = new Company();

        void setCompany(Company company) {
            this.company = company;
        }

        @Override
        public User valid() {
            return new User();
        }

        @Override
        public User valid(List<Enum<?>> required) {
            return valid();
        }

        @Override
        public Company getCurrentCompanyOrThrow() {
            return company;
        }
    }
}

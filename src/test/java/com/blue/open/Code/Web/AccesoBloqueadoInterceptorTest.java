package com.blue.open.Code.Web;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CompanyAccesoService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dobles como subclases reales en vez de mocks de Mockito, por el mismo motivo que el resto de los
 * tests del proyecto: bytebuddy no puede instrumentar estas clases concretas bajo JDK 25.
 */
class AccesoBloqueadoInterceptorTest {

    private AccesoBloqueadoInterceptor interceptor;

    private boolean sysAdmin;
    private Company companyActual;
    private CustomException errorAlResolverCompany;
    private boolean bloqueada;
    private boolean consultoElBloqueo;

    private final AuthService authService = new AuthService() {
        @Override
        public boolean isSysAdmin() {
            return sysAdmin;
        }

        @Override
        public Company getCurrentCompanyOrThrow() {
            if (errorAlResolverCompany != null) {
                throw errorAlResolverCompany;
            }
            return companyActual;
        }
    };

    private final CompanyAccesoService companyAccesoService = new CompanyAccesoService() {
        @Override
        public boolean estaBloqueada(Company company) {
            consultoElBloqueo = true;
            return bloqueada;
        }
    };

    @BeforeEach
    void setUp() {
        interceptor = new AccesoBloqueadoInterceptor();
        ReflectionTestUtils.setField(interceptor, "authService", authService);
        ReflectionTestUtils.setField(interceptor, "companyAccesoService", companyAccesoService);

        sysAdmin = false;
        errorAlResolverCompany = null;
        bloqueada = false;
        consultoElBloqueo = false;

        companyActual = new Company();
        companyActual.setId(10L);
        companyActual.setUuid("club-uuid");
    }

    // Dado un sysadmin, cuando preHandle, entonces pasa sin siquiera resolver la company: es quien
    // administra los clubes desde el panel interno, incluido el de suscribir a uno bloqueado.
    @Test
    void preHandle_sysadmin_pasaSinMirarElBloqueo() {
        sysAdmin = true;
        bloqueada = true;

        assertTrue(interceptor.preHandle(null, null, null));
        assertFalse(consultoElBloqueo);
    }

    // Dado un club bloqueado y un usuario que no es sysadmin, cuando preHandle, entonces corta con
    // 403.
    @Test
    void preHandle_clubBloqueado_cortaCon403() {
        bloqueada = true;

        CustomException ex = assertThrows(CustomException.class,
                () -> interceptor.preHandle(null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // Dado un club sin bloquear, cuando preHandle, entonces deja pasar.
    @Test
    void preHandle_clubSinBloquear_pasa() {
        assertTrue(interceptor.preHandle(null, null, null));
        assertTrue(consultoElBloqueo);
    }

    // Dado un request que no resuelve company (sin JWT, sin header, sin acceso), cuando preHandle,
    // entonces deja pasar: de eso se encarga despues el propio controller.
    @Test
    void preHandle_sinCompanyResoluble_pasa() {
        errorAlResolverCompany = new CustomException(HttpStatus.BAD_REQUEST, "sin company", "test");

        assertTrue(interceptor.preHandle(null, null, null));
        assertFalse(consultoElBloqueo);
    }
}

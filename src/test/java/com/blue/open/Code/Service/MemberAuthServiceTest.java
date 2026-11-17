package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberAuthService service;
    private StubAuthService stubAuthService;

    @BeforeEach
    void setUp() {
        service = new MemberAuthService();
        stubAuthService = new StubAuthService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
    }

    @Test
    void requireTitular_rechazaFamiliar() {
        Member familiar = new Member();
        familiar.setId(2L);
        familiar.setTitular(new Member());

        CustomException ex = assertThrows(CustomException.class, () -> service.requireTitular(familiar));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void resolveTitularByUuid_rechazaSiNoEsTitular() {
        Member familiar = new Member();
        familiar.setId(2L);
        familiar.setTitular(new Member());

        when(memberRepository.findByUuidAndCompanyIdWithDetails("fam-uuid", 10L))
                .thenReturn(Optional.of(familiar));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.resolveTitularByUuid("fam-uuid", 10L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void getCurrentMemberOrThrow_resuelvePorUsuarioYCompany() {
        User user = new User();
        user.setId(99L);
        Member member = titularMember(1L);
        stubAuthService.setUser(user);

        when(memberRepository.findByUserIdAndCompanyIdWithDetails(99L, 10L))
                .thenReturn(Optional.of(member));

        Member result = service.getCurrentMemberOrThrow(10L);

        assertEquals(1L, result.getId());
    }

    @Test
    void requirePortalEligible_rechazaInactivo() {
        Member member = titularMember(1L);
        member.setEstado(MemberEstadoEnum.INACTIVO);

        CustomException ex = assertThrows(CustomException.class, () -> service.requirePortalEligible(member));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private Member titularMember(Long id) {
        Member member = new Member();
        member.setId(id);
        member.setCompany(new Company());
        member.setUser(new User());
        return member;
    }

    private static class StubAuthService extends AuthService {
        private User user;

        void setUser(User user) {
            this.user = user;
        }

        @Override
        public User getActualOrThrow() {
            if (user == null) {
                throw new CustomException(HttpStatus.FORBIDDEN, "Usuario no identificado", "stub");
            }
            return user;
        }
    }
}

package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recalcula qué {@link CuotaSocialRango} le corresponde a un socio según su edad actual.
 * <p>
 * Se dispara en dos escenarios:
 * <ul>
 *   <li>Crear/editar un rango ({@link com.blue.open.Code.Event.CuotaSocialRangoCambiadoEvent}):
 *       recorre todos los socios activos de la company, ya que insertar o mover un tramo puede
 *       cambiar cuál es el tramo más ajustado para socios existentes. Solo <b>asigna</b> cuando
 *       encuentra un tramo que cubre la edad actual del socio; si no encuentra ninguno, no toca
 *       la asignación existente.</li>
 *   <li>Dar de baja un rango ({@link com.blue.open.Code.Event.CuotaSocialRangoBajaEvent}):
 *       recorre solo los socios que tenían asignado el rango borrado, les limpia esa referencia
 *       y les busca el tramo que corresponda a su edad actual (si no hay ninguno, quedan sin
 *       rango).</li>
 * </ul>
 */
@Service
@Slf4j
public class MemberCuotaSocialRangoRecalculoService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    @Transactional
    public void recalcularPorCompany(Long companyId) {
        List<Member> miembros = memberRepository.findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(companyId);
        List<Member> modificados = new ArrayList<>();

        for (Member member : miembros) {
            Optional<CuotaSocialRango> correcto = resolverRangoActual(companyId, member);
            if (correcto.isEmpty()) {
                continue;
            }
            CuotaSocialRango actual = member.getCuotaSocialRango();
            if (actual == null || !actual.getId().equals(correcto.get().getId())) {
                member.setCuotaSocialRango(correcto.get());
                modificados.add(member);
            }
        }

        if (!modificados.isEmpty()) {
            memberRepository.saveAll(modificados);
        }
        log.info("Recalculo de rangos de cuota social: {} socio(s) actualizados company={}",
                modificados.size(), companyId);
    }

    @Transactional
    public void recalcularPorRangoBorrado(Long companyId, Long cuotaSocialRangoIdBorrado) {
        List<Member> miembros = memberRepository
                .findParaRecalculoCuotaSocialRangoByCuotaSocialRangoIdAndCompanyIdWithUser(cuotaSocialRangoIdBorrado, companyId);
        if (miembros.isEmpty()) {
            return;
        }

        for (Member member : miembros) {
            member.setCuotaSocialRango(null);
            resolverRangoActual(companyId, member).ifPresent(member::setCuotaSocialRango);
        }

        memberRepository.saveAll(miembros);
        log.info("Recalculo de rangos de cuota social tras baja de rango {}: {} socio(s) reasignados company={}",
                cuotaSocialRangoIdBorrado, miembros.size(), companyId);
    }

    private Optional<CuotaSocialRango> resolverRangoActual(Long companyId, Member member) {
        User user = member.getUser();
        if (user == null || user.getFechaNacimiento() == null) {
            return Optional.empty();
        }
        int edad = LocalDate.now().getYear() - user.getFechaNacimiento().getYear();
        return cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(companyId, edad);
    }
}

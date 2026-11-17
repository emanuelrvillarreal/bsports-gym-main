package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CategoriaBajaDesasignacionService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    @Transactional
    public void desasignarPorBaja(Long companyId, Long categoriaId, String categoriaUuid) {
        int socios = memberRepository.clearCategoriaByCategoriaIdAndCompanyId(categoriaId, companyId);
        int actividades = actividadRepository.clearCategoriaByCategoriaIdAndCompanyId(categoriaId, companyId);

        log.info(
                "Categoría dada de baja: desasignados {} socio(s) y {} actividad(es) | companyId={} categoriaId={} uuid={}",
                socios, actividades, companyId, categoriaId, categoriaUuid);
    }
}

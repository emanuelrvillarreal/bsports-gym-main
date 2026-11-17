package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberCategoriaInscripcionService {

    @Autowired
    private MemberRepository memberRepository;

    public void asignar(Member member, Categoria categoria) {
        if (member == null || categoria == null) {
            return;
        }
        member.setCategoria(categoria);
        memberRepository.save(member);
    }

    public void desasignarSiCorresponde(Member member, Actividad actividad) {
        if (member == null || actividad == null || actividad.getCategorias().isEmpty()) {
            return;
        }
        Categoria categoriaMember = member.getCategoria();
        if (categoriaMember == null) {
            return;
        }
        boolean categoriaDeLaActividad = actividad.getCategorias().stream()
                .anyMatch(categoria -> categoria.getId().equals(categoriaMember.getId()));
        if (!categoriaDeLaActividad) {
            return;
        }
        member.setCategoria(null);
        memberRepository.save(member);
    }

    public void limpiar(Member member) {
        if (member == null || member.getCategoria() == null) {
            return;
        }
        member.setCategoria(null);
        memberRepository.save(member);
    }
}

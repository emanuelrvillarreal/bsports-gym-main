package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Event.CategoriaBajaEvent;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaBajaService {

    private static final int STATUS_DELETED = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void bajaLogica(String categoriaUuid) {
        authService.valid(List.of(PermissionEnum.CATEGORIA_DELETE));
        Company company = authService.getCurrentCompanyOrThrow();

        Categoria categoria = categoriaRepository.findByUuidAndCompanyIdAndStatusActive(categoriaUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Categoría no encontrada",
                        "CategoriaBajaService | uuid: " + categoriaUuid));

        if (categoria.getStatus() != null && categoria.getStatus() == STATUS_DELETED) {
            return;
        }

        categoria.setStatus(STATUS_DELETED);
        categoriaRepository.save(categoria);

        eventPublisher.publishEvent(new CategoriaBajaEvent(
                company.getId(),
                categoria.getId(),
                categoria.getUuid()));
    }
}

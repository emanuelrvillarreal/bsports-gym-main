package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CategoriaAltaCommand;
import com.blue.open.Code.Entity.DTO.CategoriaDTO;
import com.blue.open.Code.Entity.DTO.CategoriaEdicionCommand;
import com.blue.open.Code.Entity.DTO.CategoriaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Categorias.CategoriaAltaService;
import com.blue.open.Code.Service.Categorias.CategoriaBajaService;
import com.blue.open.Code.Service.Categorias.CategoriaEdicionService;
import com.blue.open.Code.Service.Categorias.CategoriaQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("categoria")
public class CategoriaController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoriaAltaService categoriaAltaService;

    @Autowired
    private CategoriaEdicionService categoriaEdicionService;

    @Autowired
    private CategoriaBajaService categoriaBajaService;

    @Autowired
    private CategoriaQueryService categoriaQueryService;

    @PostMapping
    public ResponseEntity<CategoriaDTO> alta(@Valid @RequestBody CategoriaAltaCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaAltaService.alta(command));
    }

    @PutMapping("/{categoriaUuid}")
    public ResponseEntity<CategoriaDTO> editar(
            @PathVariable String categoriaUuid,
            @Valid @RequestBody CategoriaEdicionCommand command) {
        return ResponseEntity.ok(categoriaEdicionService.editar(categoriaUuid, command));
    }

    @DeleteMapping("/{categoriaUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String categoriaUuid) {
        categoriaBajaService.bajaLogica(categoriaUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<CategoriaDTO>> paginate(
            @RequestBody(required = false) CategoriaPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.CATEGORIA_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(categoriaQueryService.paginar(request, company.getId()));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listar() {
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(categoriaQueryService.listarActivas(company.getId()));
    }
}

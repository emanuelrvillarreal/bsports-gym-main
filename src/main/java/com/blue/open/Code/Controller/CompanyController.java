package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.ClubProfileUpdateRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.DTO.CompanyPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyPublicDTO;
import com.blue.open.Code.Entity.DTO.MenuSolapasDTO;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.CompanyService;
import com.blue.open.Code.Service.MenuSolapasService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Controller.BaseCompanyController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;



@RestController

@RequestMapping("companies")

public class CompanyController extends BaseCompanyController<CompanyDTO, CompanyService> {
    @Autowired
    private MenuSolapasService menuSolapasService;

    @PostMapping("/paginate-club")
    @Operation(summary = "Paginar clubes", description = "Busca y pagina clubes por CUIT, razón social o nombre comercial. Solo SYSADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<CompanyDTO>> paginate(@RequestBody(required = false) CompanyPaginateRequestDTO request) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(service.paginar(request));
    }

    @PutMapping("/profile")
    @Operation(summary = "Actualizar perfil del club", description = "Actualiza los datos institucionales del club actual. Solo ADMIN o SYSADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CompanyDTO> updateProfile(@Valid @RequestBody ClubProfileUpdateRequestDTO request) {
        authService.valid(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
        return new ResponseEntity<>(service.updateClubProfile(request), HttpStatus.OK);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir logo del club", description = "Reemplaza el logo institucional del club actual. Solo ADMIN o SYSADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CompanyDTO> uploadLogo(@RequestPart("file") MultipartFile file) {
        authService.valid(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
        return new ResponseEntity<>(service.uploadLogo(file), HttpStatus.OK);
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Eliminar logo del club", description = "Elimina el logo institucional del club actual. Solo ADMIN o SYSADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CompanyDTO> deleteLogo() {
        authService.valid(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
        return new ResponseEntity<>(service.deleteLogo(), HttpStatus.OK);
    }

    @GetMapping("/menu-solapas")
    @Operation(summary = "Obtener solapas ocultas del menú", description = "Devuelve las solapas del menú administrativo que el club actual ocultó. ADMIN, ADM_RECEPCION o SYSADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MenuSolapasDTO> getMenuSolapas() {
        authService.valid(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN, RoleEnum.ADM_RECEPCION));
        return ResponseEntity.ok(menuSolapasService.obtener());
    }

    @PutMapping("/menu-solapas")
    @Operation(summary = "Guardar solapas ocultas del menú", description = "Reemplaza las solapas ocultas del menú administrativo del club actual. Aplica al ADMIN y a recepción. Solo ADMIN o SYSADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MenuSolapasDTO> updateMenuSolapas(@Valid @RequestBody MenuSolapasDTO request) {
        authService.valid(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(menuSolapasService.guardar(request));
    }

    @GetMapping("/public/by-path/{path}")
    @Operation(summary = "Obtener club por path", description = "Busca un club por su identificador de path. Endpoint público. Devuelve solo datos públicos del club.")
    public ResponseEntity<CompanyPublicDTO> findByPath(@PathVariable String path) {
        return ResponseEntity.ok(service.findByPath(path));
    }

    @GetMapping("/current")
    @Operation(summary = "Obtener club actual", description = "Obtiene los datos del club actual (header X-Company-Uuid). ADMIN o SYSADMIN reciben los datos completos; SOCIO_TITULAR, FAMILIAR o PROFESOR solo reciben colores y logo.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CompanyDTO> getCurrentCompany() {
        User user = authService.valid();
        return ResponseEntity.ok(service.getCurrentCompany(user));
    }
}



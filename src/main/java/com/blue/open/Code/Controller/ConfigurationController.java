package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Code.Service.ConfiguracionService;
import com.blue.open.Code.Entity.DTO.ConfigurationDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Las configuraciones permiten guardar variables del sistema
// Son utiles para cambiar parametros sin compilar, o que varian dependiendo del entorno
// No hay un límite de variables a crear
// Además son dinamicas, por ende siempre se tiene que comprobar que existan o usar un valor predetermiado

// Existen 2 tipos de variables, las publicas y las privadas
// Las publicas pueden ser obtenidas por cualquier usuario, incluso sin estar conectado
// Las privadas solo las pueden ver los admins, o se pueden obtener desde el backend
// Las variables solo pueden ser editadas por los admins

@RestController
@CrossOrigin
@RequestMapping(path = "/configuracion")
public class ConfigurationController {
    @Autowired
    ConfiguracionService service;
    @Autowired
    AuthService authService;

    /**
     * Obtiene todas las configuraciones públicas
     *
     * @return InfraccionDTO
     * @throws List<ConfigurationDTO>
     */
    @GetMapping("/all")
    public ResponseEntity<List<ConfigurationDTO>> getAll(
            @RequestParam(required = false) String companyUuid) throws Exception {
        if (this.authService.validBoolean(List.of(PermissionEnum.CONFIGURATION_VIEW))) {
            return new ResponseEntity<>(this.service.getAll(companyUuid), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(this.service.getPublic(), HttpStatus.OK);
        }
    }

    /**
     * Obtiene una sola configuracion buscando por key
     *
     * @return InfraccionDTO
     * @throws List<ConfigurationDTO>
     */
    @GetMapping("/get/{key}")
    public ResponseEntity<ConfigurationDTO> getOne(
            @PathVariable("key") String key,
            @RequestParam(required = false) String companyUuid) throws Exception {
        ConfigurationDTO configuration = this.service.getByKeyDto(key, null, companyUuid);
        if (configuration.isGlobal() || this.authService.validBoolean(List.of(PermissionEnum.CONFIGURATION_VIEW))) {
            return new ResponseEntity<>(configuration, HttpStatus.OK);
        } else {
            throw new CustomException("La variable es privada");
        }
    }

    /**
     * Obtiene una sola configuracion buscando por key
     *
     * @return InfraccionDTO
     * @throws List<ConfigurationDTO>
     */
    @PostMapping("/get/list")
    public ResponseEntity<List<ConfigurationDTO>> getList(
            @RequestBody List<String> keys,
            @RequestParam(required = false) String companyUuid) throws Exception {
        List<ConfigurationDTO> configuraciones = this.service.getByKeyList(keys, companyUuid);
        if (!this.authService.validBoolean(List.of(BaseRoleEnum.ADMIN))) {
            String privadas = "";
            for (ConfigurationDTO dto : configuraciones) {
                if (!dto.isGlobal())
                    privadas += dto.getName() + ", ";
            }
            if (!privadas.isEmpty())
                throw new CustomException("Las variables " + privadas + " son privadas");
        }

        return new ResponseEntity<>(configuraciones, HttpStatus.OK);
    }

    /**
     * Edita o crea una lista de configuraciones
     *
     * @return InfraccionDTO
     * @throws List<ConfigurationDTO>
     */
    @PostMapping("/save")
    public ResponseEntity<List<ConfigurationDTO>> saveOrCreate(
            @RequestBody List<ConfigurationDTO> configuracions,
            @RequestParam(required = false) String companyUuid)
            throws Exception {
        this.authService.valid(List.of(PermissionEnum.CONFIGURATION_CREATE));
        return new ResponseEntity<>(this.service.saveOrCreate(configuracions, companyUuid), HttpStatus.OK);
    }
}

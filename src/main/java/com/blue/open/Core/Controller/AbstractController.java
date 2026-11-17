package com.blue.open.Core.Controller;

import com.blue.open.Core.Entity.DTO.AbstractEntityDTO;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.AbstractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public abstract class AbstractController<T extends AbstractEntity, DTO extends AbstractEntityDTO, PAGINATEDTO extends AbstractEntityDTO, S extends AbstractService<T, DTO, PAGINATEDTO>> {

        @Autowired
        protected S service;

        @Autowired
        protected AuthService authService;

        // Hooks
        protected void hookUuidAuth(String uuid) {
        }

        protected void hookPaginateAuth(PaginateList paginateList) {
        }

        protected PaginateList hookPaginate(PaginateList paginateList) {
                return paginateList;
        }

        protected void hookSaveAuth(DTO model) {
        }

        protected void hookDeleteAuth(String uuid) {
        }

        /**
         * Obtiene un solo resultado buscando por uuid
         */
        @GetMapping("/uuid/{uuid}")
        @Operation(summary = "Obtener por UUID", description = "Busca un registro a partir de su UUID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Registro encontrado"),
                        @ApiResponse(responseCode = "400", description = "UUID invalido", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 11:12:33\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"No existe el registro\",\"developerMessage\":null,\"errores\":[],\"message\":\"No existe el registro\",\"suppressed\":[],\"localizedMessage\":\"No existe el registro\"}"))),
                        @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"25/09/2025 11:27:26\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"Mensaje que ve el usuario\",\"developerMessage\":\"Mensaje que ve el programador\",\"errores\":[],\"message\":\"Mensaje que ve el usuario\",\"suppressed\":[],\"localizedMessage\":\"Mensaje que ve el usuario\"}"))),
                        @ApiResponse(responseCode = "401", description = "Usuario no validado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 10:52:09\",\"status\":\"UNAUTHORIZED\",\"userMessage\":\"Usuario o Contraseña Invalido/s\",\"developerMessage\":\"Usuario o Contraseña Invalidos\",\"errores\":[],\"message\":\"Usuario o Contraseña Invalido/s\",\"suppressed\":[],\"localizedMessage\":\"Usuario o Contraseña Invalido/s\"}"))),
                        @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"No static resource paginaNoExistente.\",\"instance\":\"/api/paginaNoExistente\",\"properties\":null}"))),
        })
        @SecurityRequirement(name = "basicAuth")
        public ResponseEntity<DTO> getByUuid(
                        @Parameter(description = "UUID del registro", required = true) @PathVariable("uuid") String uuid)
                        throws CustomException {
                this.hookUuidAuth(uuid);
                return new ResponseEntity<>(this.service.getByUuid(uuid), HttpStatus.OK);
        }

        /**
         * Obtiene un paginado con base en filtros
         */
        @PostMapping("/paginate")
        @Operation(summary = "Paginado con filtros avanzados", description = "Devuelve un listado paginado de registros según filtros y ordenes enviados")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Página obtenida correctamente"),
                        @ApiResponse(responseCode = "400", description = "Error en filtros o parámetros", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 11:16:31\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"Error al obtener el paginado\",\"developerMessage\":\"Error en paginate: JDBC exception executing SQL [SELECT count(*) FROM usuarios t WHERE t.id > 0 AND (t.columna_falsa = ?) AND (t.status = ?)] [Unknown column 't.columna_falsa' in 'where clause'] [n/a]; SQL [n/a]\",\"errores\":[],\"message\":\"Error al obtener el paginado\",\"suppressed\":[],\"localizedMessage\":\"Error al obtener el paginado\"}"))),
                        @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"25/09/2025 11:27:26\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"Mensaje que ve el usuario\",\"developerMessage\":\"Mensaje que ve el programador\",\"errores\":[],\"message\":\"Mensaje que ve el usuario\",\"suppressed\":[],\"localizedMessage\":\"Mensaje que ve el usuario\"}"))),
                        @ApiResponse(responseCode = "401", description = "Usuario no validado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 10:52:09\",\"status\":\"UNAUTHORIZED\",\"userMessage\":\"Usuario o Contraseña Invalido/s\",\"developerMessage\":\"Usuario o Contraseña Invalidos\",\"errores\":[],\"message\":\"Usuario o Contraseña Invalido/s\",\"suppressed\":[],\"localizedMessage\":\"Usuario o Contraseña Invalido/s\"}"))),
                        @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"No static resource paginaNoExistente.\",\"instance\":\"/api/paginaNoExistente\",\"properties\":null}"))),
        })
        @SecurityRequirement(name = "basicAuth")
        public ResponseEntity<Page<PAGINATEDTO>> paginate(
                        @Parameter(description = "Filtros, ordenes y relaciones de tablas", required = true) @RequestBody PaginateList paginateList)
                        throws Exception {

                this.hookPaginateAuth(paginateList);
                paginateList = this.hookPaginate(paginateList);
                return new ResponseEntity<>(this.service.paginate(paginateList), HttpStatus.OK);
        }

        /**
         * Guarda o edita un registro
         */
        @PostMapping("/save")
        @Operation(summary = "Guardar o editar un registro", description = "Permite crear un nuevo registro o actualizar uno existente si existe el uuid")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Registro guardado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 11:18:06\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"Error con los datos enviados\",\"developerMessage\":\"Error en el cuerpo del request: Unrecognized field \\\"parametroFalso\\\" (class com.blue.open.Code.Entity.DTO.UsuarioDTO), not marked as ignorable (9 known properties: \\\"contra\\\", \\\"status\\\", \\\"telefono\\\", \\\"dateCreated\\\", \\\"uuid\\\", \\\"email\\\", \\\"usuario\\\", \\\"rol\\\", \\\"abrstract\\\"])\\n at [Source: (org.springframework.util.StreamUtils$NonClosingInputStream); line: 2, column: 24] (through reference chain: com.blue.open.Code.Entity.DTO.UsuarioDTO[\\\"parametroFalso\\\"])\",\"errores\":[],\"message\":\"Error con los datos enviados\",\"suppressed\":[],\"localizedMessage\":\"Error con los datos enviados\"}"))),
                        @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"25/09/2025 11:27:26\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"Mensaje que ve el usuario\",\"developerMessage\":\"Mensaje que ve el programador\",\"errores\":[],\"message\":\"Mensaje que ve el usuario\",\"suppressed\":[],\"localizedMessage\":\"Mensaje que ve el usuario\"}"))),
                        @ApiResponse(responseCode = "401", description = "Usuario no validado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 10:52:09\",\"status\":\"UNAUTHORIZED\",\"userMessage\":\"Usuario o Contraseña Invalido/s\",\"developerMessage\":\"Usuario o Contraseña Invalidos\",\"errores\":[],\"message\":\"Usuario o Contraseña Invalido/s\",\"suppressed\":[],\"localizedMessage\":\"Usuario o Contraseña Invalido/s\"}"))),
                        @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"No static resource paginaNoExistente.\",\"instance\":\"/api/paginaNoExistente\",\"properties\":null}"))),
        })
        @SecurityRequirement(name = "basicAuth")
        public ResponseEntity<DTO> saveOrCreate(
                        @Parameter(description = "DTO del registro", required = true) @RequestBody DTO dto)
                        throws Exception {
                this.hookSaveAuth(dto);
                return new ResponseEntity<>(this.service.saveOrCreate(dto), HttpStatus.OK);
        }

        /**
         * Borrar un registro
         */
        @DeleteMapping("/{uuid}")
        @Operation(summary = "Eliminar registro", description = "Elimina de manera logica un registro existente a partir de su UUID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Registro eliminado correctamente"),
                        @ApiResponse(responseCode = "400", description = "UUID inválido o registro inexistente", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 11:19:23\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"No existe o ya fue eliminado\",\"developerMessage\":null,\"errores\":[],\"message\":\"No existe o ya fue eliminado\",\"suppressed\":[],\"localizedMessage\":\"No existe o ya fue eliminado\"}"))),
                        @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"25/09/2025 11:27:26\",\"status\":\"BAD_REQUEST\",\"userMessage\":\"Mensaje que ve el usuario\",\"developerMessage\":\"Mensaje que ve el programador\",\"errores\":[],\"message\":\"Mensaje que ve el usuario\",\"suppressed\":[],\"localizedMessage\":\"Mensaje que ve el usuario\"}"))),
                        @ApiResponse(responseCode = "401", description = "Usuario no validado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"cause\":null,\"stackTrace\":[],\"localTime\":\"01/10/2025 10:52:09\",\"status\":\"UNAUTHORIZED\",\"userMessage\":\"Usuario o Contraseña Invalido/s\",\"developerMessage\":\"Usuario o Contraseña Invalidos\",\"errores\":[],\"message\":\"Usuario o Contraseña Invalido/s\",\"suppressed\":[],\"localizedMessage\":\"Usuario o Contraseña Invalido/s\"}"))),
                        @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"No static resource paginaNoExistente.\",\"instance\":\"/api/paginaNoExistente\",\"properties\":null}"))),
        })
        @SecurityRequirement(name = "basicAuth")
        public ResponseEntity<DTO> delete(
                        @Parameter(description = "UUID del registro", required = true) @PathVariable("uuid") String uuid)
                        throws Exception {
                this.hookDeleteAuth(uuid);
                return new ResponseEntity<>(this.service.delete(uuid), HttpStatus.OK);
        }
}

package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.PersonalDTO;
import com.blue.open.Code.Service.Personal.PersonalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Personal del club (administradores, recepción y profesores) con roles combinables. */
@RestController
@RequestMapping("personal")
public class PersonalController {

    @Autowired
    private PersonalService personalService;

    @GetMapping
    public ResponseEntity<PersonalDTO.Listado> listar(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(personalService.listar(search));
    }

    @GetMapping("/buscar")
    public ResponseEntity<PersonalDTO.Encontrada> buscar(@RequestParam String q) {
        return ResponseEntity.ok(personalService.buscar(q));
    }

    @PostMapping
    public ResponseEntity<PersonalDTO.Listado> alta(@RequestBody PersonalDTO.Alta command) {
        return ResponseEntity.ok(personalService.alta(command));
    }

    @PutMapping("/uuid/{userUuid}/roles")
    public ResponseEntity<PersonalDTO.Listado> cambiarRoles(
            @PathVariable String userUuid,
            @RequestBody PersonalDTO.Roles roles) {
        return ResponseEntity.ok(personalService.cambiarRoles(userUuid, roles));
    }
}

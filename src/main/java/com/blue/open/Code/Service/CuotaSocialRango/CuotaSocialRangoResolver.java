package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Resuelve el {@link CuotaSocialRango} que le corresponde a un socio nuevo según su edad
 * reglamentaria (año de la fecha de alta − año de nacimiento), igual criterio de año
 * calendario que {@code CategoriaEdadUtil}.
 */
@Service
public class CuotaSocialRangoResolver {

    @Autowired
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    /**
     * @return el rango correspondiente, o {@code null} si el club no tiene la cuota social
     *         habilitada (en ese caso no aplica resolver nada).
     * @throws CustomException si la cuota social está habilitada pero falta la fecha de
     *         nacimiento, o no hay ningún rango del club que cubra esa edad.
     */
    public CuotaSocialRango resolver(Company company, LocalDate fechaNacimiento, LocalDate fechaAlta) {
        if (!company.tieneCuotaSocialConfigurada()) {
            return null;
        }

        if (fechaNacimiento == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La fecha de nacimiento es requerida para asignar el rango de cuota social",
                    "CuotaSocialRangoResolver | companyId: " + company.getId() + " fechaNacimiento null");
        }

        int anioReferencia = (fechaAlta != null ? fechaAlta : LocalDate.now()).getYear();
        int edad = anioReferencia - fechaNacimiento.getYear();

        return cuotaSocialRangoRepository.findByCompanyIdAndEdadHasta(company.getId(), edad)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST,
                        "El gimnasio no tiene un rango de cuota social configurado para la edad del socio",
                        "CuotaSocialRangoResolver | companyId: " + company.getId() + " edad: " + edad));
    }
}

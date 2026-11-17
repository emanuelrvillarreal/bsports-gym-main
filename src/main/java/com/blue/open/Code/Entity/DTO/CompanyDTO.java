package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CertificadoVigenciaModoEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.Enums.RecargoTipoEnum;
import com.blue.open.Core.Entity.DTO.BaseCompanyDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CompanyDTO extends BaseCompanyDTO {

    private String cuit;
    private String nombreComercial;
    private String razonSocial;
    private CompanyDireccionDTO domicilio;
    private CompanyDireccionDTO domicilioFacturacion;
    private String telefono;
    private String emailFacturaElectronica;
    private String emailInstitucional;
    private String logoUrl;
    private String colorPrimario;
    private String colorSecundario;
    private Boolean cuotaSocialHabilitada;
    private PeriodicidadCuotaEnum periodicidadCuotaSocial;
    private Integer diasVencimientoCuota;
    private Integer diasVencimientoMoroso;
    private Integer diasInhabilitacion;
    private Boolean adminCreado;

    // Vigencia del certificado médico
    private CertificadoVigenciaModoEnum certificadoVigenciaModo;
    private Integer certificadoVigenciaMeses;
    private Integer certificadoRenovacionDia;
    private Integer certificadoRenovacionMes;

    // Recargo por pago fuera de término (opcional por club)
    private Boolean recargoHabilitado;
    private RecargoTipoEnum recargoTipo;
    private BigDecimal recargoValor;
    private Integer recargoDiasGracia;

    // Suscripcion vigente del club al plan del CRM. Null si el club nunca se suscribio
    private CompanySuscripcionCrmDTO suscripcionCrm;

    // Ultima factura del club (por fechaGeneracion). Los 3 quedan null si el club no tiene
    // ninguna factura todavia
    private FacturaEstadoEnum ultimaFacturaEstado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate ultimaFacturaFechaGeneracion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate ultimaFacturaFechaVencimiento;
}

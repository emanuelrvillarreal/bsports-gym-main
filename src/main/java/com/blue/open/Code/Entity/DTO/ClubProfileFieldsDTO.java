package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CertificadoVigenciaModoEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.Enums.RecargoTipoEnum;
import com.blue.open.Code.Validation.ValidCuit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ClubProfileFieldsDTO {

    @NotBlank(message = "Nombre del club es obligatorio.")
    private String nombreComercial;

    @NotBlank(message = "Razón social es obligatoria.")
    private String razonSocial;

    @NotBlank(message = "CUIT es obligatorio.")
    @ValidCuit(message = "El CUIT ingresado no es válido según su dígito verificador.")
    private String cuit;

    @NotNull(message = "El domicilio es obligatorio.")
    @Valid
    private CompanyDireccionDTO domicilio;

    @NotNull(message = "El domicilio de facturación es obligatorio.")
    @Valid
    private CompanyDireccionDTO domicilioFacturacion;

    @NotBlank(message = "Teléfono es obligatorio.")
    private String telefono;

    @NotBlank(message = "Email para factura electrónica es obligatorio.")
    @Email(message = "Email para factura electrónica inválido.")
    private String emailFacturaElectronica;

    @NotBlank(message = "Email institucional es obligatorio.")
    @Email(message = "Email institucional inválido.")
    private String emailInstitucional;

    @NotBlank(message = "El path es obligatorio.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "El path solo puede contener letras, números, guiones y guiones bajos.")
    private String path;

    @NotBlank(message = "Color primario es obligatorio.")
    private String colorPrimario;

    @NotBlank(message = "Color secundario es obligatorio.")
    private String colorSecundario;

    @NotNull(message = "Debe indicar si la cuota social está habilitada.")
    private Boolean cuotaSocialHabilitada;

    private PeriodicidadCuotaEnum periodicidadCuotaSocial;

    private Integer diasVencimientoCuota;

    private Integer diasVencimientoMoroso;

    private Integer diasInhabilitacion;

    private CertificadoVigenciaModoEnum certificadoVigenciaModo;

    private Integer certificadoVigenciaMeses;

    private Integer certificadoRenovacionDia;

    private Integer certificadoRenovacionMes;

    private Boolean recargoHabilitado;

    private RecargoTipoEnum recargoTipo;

    private BigDecimal recargoValor;

    private Integer recargoDiasGracia;
}

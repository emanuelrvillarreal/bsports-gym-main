package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.CertificadoMedicoDTO;
import com.blue.open.Code.Entity.Db.CertificadoMedico;
import org.springframework.stereotype.Service;

@Service
public class CertificadoMedicoMapper {

    public CertificadoMedicoDTO modelToDto(CertificadoMedico certificado, String memberUuid) {
        if (certificado == null) {
            return null;
        }
        CertificadoMedicoDTO dto = new CertificadoMedicoDTO();
        dto.setCertificadoUuid(certificado.getUuid());
        dto.setMemberUuid(memberUuid);
        dto.setFechaCarga(certificado.getFechaCarga());
        dto.setFechaExpiracion(certificado.getFechaExpiracion());
        dto.setNombreArchivo(certificado.getNombreArchivo());
        dto.setUrlArchivo(certificado.getUrlArchivo());
        dto.setEstado(certificado.getEstado());
        return dto;
    }
}

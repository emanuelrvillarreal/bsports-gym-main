package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberBulkImportResultDetailDTO;
import com.blue.open.Code.Entity.DTO.MemberBulkImportResultItemDTO;
import com.blue.open.Code.Entity.DTO.MemberBulkImportResultsPageDTO;
import com.blue.open.Code.Entity.Db.MemberBulkImportResult;
import com.blue.open.Code.Repository.MemberBulkImportResultRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class MemberBulkImportResultService {

    @Autowired
    private MemberBulkImportResultRepository resultRepository;

    public MemberBulkImportResultsPageDTO obtenerResultados(Long companyId, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Parámetros de paginación inválidos",
                    "MemberBulkImportResultService | page: " + page + ", size: " + size);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MemberBulkImportResult> resultados = resultRepository.findByCompanyIdOrderByDateCreatedDesc(companyId, pageable);

        MemberBulkImportResultsPageDTO response = new MemberBulkImportResultsPageDTO();
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(resultados.getTotalElements());
        response.setTotalPages(resultados.getTotalPages());
        response.setHasNext(resultados.hasNext());
        response.setHasPrevious(resultados.hasPrevious());

        response.setContent(resultados.getContent().stream()
                .map(this::toItemDTO)
                .toList());

        return response;
    }

    public MemberBulkImportResultDetailDTO obtenerDetalle(String importId) {
        MemberBulkImportResult entity = resultRepository.findByImportId(importId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Importación no encontrada",
                        "MemberBulkImportResultService | importId: " + importId));

        MemberBulkImportResultDetailDTO detail = new MemberBulkImportResultDetailDTO();
        detail.setImportId(entity.getImportId());
        detail.setTotalRows(entity.getTotalRows());
        detail.setSuccessCount(entity.getSuccessCount());
        detail.setErrorCount(entity.getErrorCount());
        detail.setWarningCount(entity.getWarningCount());
        detail.setImportStatus(entity.getImportStatus());
        detail.setErrorMessage(entity.getErrorMessage());
        detail.setDateCreated(convertTimestampToLocalDateTime(entity.getDateCreated()));
        detail.setRows(entity.getRows());

        return detail;
    }

    private MemberBulkImportResultItemDTO toItemDTO(MemberBulkImportResult entity) {
        MemberBulkImportResultItemDTO dto = new MemberBulkImportResultItemDTO();
        dto.setImportId(entity.getImportId());
        dto.setTotalRows(entity.getTotalRows());
        dto.setSuccessCount(entity.getSuccessCount());
        dto.setErrorCount(entity.getErrorCount());
        dto.setWarningCount(entity.getWarningCount());
        dto.setImportStatus(entity.getImportStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setDateCreated(convertTimestampToLocalDateTime(entity.getDateCreated()));
        return dto;
    }

    private LocalDateTime convertTimestampToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}

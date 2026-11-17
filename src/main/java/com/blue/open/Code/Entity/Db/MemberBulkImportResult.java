package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Table(name = "member_bulk_import_results")
@Entity
public class MemberBulkImportResult extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "import_id", length = 36, nullable = false, unique = true)
    private String importId;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "import_status", nullable = false, length = 20)
    private String importStatus;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "rows_data", columnDefinition = "LONGTEXT")
    private String rowsData;

    public List<com.blue.open.Code.Entity.DTO.MemberBulkImportRowResultDTO> getRows() {
        if (rowsData == null || rowsData.isEmpty()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(rowsData, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public void setRows(List<com.blue.open.Code.Entity.DTO.MemberBulkImportRowResultDTO> rows) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.rowsData = mapper.writeValueAsString(rows);
        } catch (Exception e) {
            this.rowsData = null;
        }
    }
}

package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MemberBulkImportResultDetailDTO {

    private String importId;
    private int totalRows;
    private int successCount;
    private int errorCount;
    private int warningCount;
    private String importStatus;
    private String errorMessage;
    @JsonFormat(pattern = "dd/MM/yyyy - HH:mm", shape = JsonFormat.Shape.STRING)
    private LocalDateTime dateCreated;
    private List<MemberBulkImportRowResultDTO> rows = new ArrayList<>();
}

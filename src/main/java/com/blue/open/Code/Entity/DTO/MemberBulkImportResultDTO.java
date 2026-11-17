package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MemberBulkImportResultDTO {

    private int totalRows;
    private int successCount;
    private int errorCount;
    private int warningCount;
    private List<MemberBulkImportRowResultDTO> rows = new ArrayList<>();
    private List<MemberBulkImportRowResultDTO> errorRows = new ArrayList<>();
}

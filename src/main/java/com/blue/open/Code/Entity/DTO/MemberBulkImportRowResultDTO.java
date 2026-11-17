package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MemberBulkImportRowResultDTO {

    private int rowNumber;
    private String status;
    private String tipo;
    private String memberUuid;
    private String numeroSocio;
    private String errorMessage;
    private List<String> warnings = new ArrayList<>();
}

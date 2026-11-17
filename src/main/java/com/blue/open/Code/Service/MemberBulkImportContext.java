package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;

import java.util.List;

/**
 * Datos ya validados en el hilo de la request (permisos, company y parseo del
 * Excel) que se entregan al procesamiento asíncrono de la importación masiva.
 */
public record MemberBulkImportContext(String importId, Company company, List<MemberBulkImportRow> rows) {
}

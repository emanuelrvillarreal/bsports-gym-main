package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.DTO.*;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// ULTIMA ACTUALIZACION
// POR: TAIEL MARTINEZ
// FECHA: 30/09/2025

public class RepositoryImpl<T extends AbstractEntity> {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryImpl.class);
    @PersistenceContext
    protected EntityManager entityManager;

    private RepositoryImplQuery getSql(PaginateList paginateList, List<KeyValue> keyValues) {
        String whereString = " WHERE t.id > 0";
        String orderString = "";
        String joinString = "";
        String groupString = ""; // nuevo

        int offset = (paginateList.getPage() - 1) * paginateList.getSize();
        String limitString = " LIMIT " + paginateList.getSize() + " OFFSET " + offset;

        Set<String> usedTables = new HashSet<>();
        usedTables.add("t");

        int paramIndex = 0;
        for (List<PaginateFiltro> grupo : paginateList.getFiltros()) {
            // separamos condiciones normales
            List<PaginateFiltro> condiciones = new ArrayList<>();
            for (PaginateFiltro filtro : grupo) {
                if ("group".equalsIgnoreCase(filtro.getOperacion())) {
                    if (groupString.isEmpty()) {
                        groupString = " GROUP BY ";
                    } else {
                        groupString += ", ";
                    }
                    groupString += filtro.getTabla() + "." + filtro.getColumna();
                    usedTables.add(filtro.getTabla());
                } else {
                    condiciones.add(filtro);
                }
            }

            if (!condiciones.isEmpty()) {
                whereString += " AND (";
                for (int i = 0; i < condiciones.size(); i++) {
                    PaginateFiltro filtro = condiciones.get(i);
                    usedTables.add(filtro.getTabla());

                    if (i > 0) whereString += " OR ";
                    String paramName = "param_" + paramIndex;

                    if ("is null".equalsIgnoreCase(filtro.getOperacion())
                            || "is not null".equalsIgnoreCase(filtro.getOperacion())) {
                        whereString += filtro.getTabla() + "." + filtro.getColumna() + " " + filtro.getOperacion();
                    } else if ("in".equalsIgnoreCase(filtro.getOperacion())) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            List<String> valores = mapper.readValue(
                                    filtro.getValor(),
                                    new TypeReference<List<String>>() {
                                    }
                            );
                            String inClause = valores.stream()
                                    .map(v -> "'" + v + "'")
                                    .collect(Collectors.joining(", ", "(", ")"));
                            whereString += filtro.getTabla() + "." + filtro.getColumna() + " IN " + inClause;
                        } catch (Exception e) {
                            throw new CustomException("Error al realizar el paginado",
                                    "Error al intentar formatear el filtro 'in': " + e.getMessage());
                        }
                    } else {
                        whereString += filtro.getTabla() + "." + filtro.getColumna()
                                + " " + filtro.getOperacion() + " :" + paramName;
                        keyValues.add(new KeyValue(paramName, filtro.getValor()));
                    }
                    paramIndex++;
                }
                whereString += ")";
            }
        }

        if (!paginateList.getOrdenes().isEmpty()) {
            orderString = " ORDER BY ";
            for (int i = 0; i < paginateList.getOrdenes().size(); i++) {
                PaginateOrden orden = paginateList.getOrdenes().get(i);
                usedTables.add(orden.getTabla());

                if (i > 0) orderString += ",";
                orderString += " " + orden.getTabla() + "." + orden.getColumna() + " " + orden.getDireccion();
            }
        }

        // Expand usedTables to include intermediate tables for join chains
        boolean expanded;
        do {
            expanded = false;
            for (PaginateRelacion relacion : paginateList.getRelaciones()) {
                if (usedTables.contains(relacion.getHastaTabla()) && !usedTables.contains(relacion.getDesdeTabla())) {
                    usedTables.add(relacion.getDesdeTabla());
                    expanded = true;
                }
            }
        } while (expanded);

        Set<String> joinedTables = new HashSet<>();
        joinedTables.add("t");
        boolean added;
        do {
            added = false;
            for (PaginateRelacion relacion : paginateList.getRelaciones()) {
                if (usedTables.contains(relacion.getHastaTabla()) && !joinedTables.contains(relacion.getHastaTabla()) && joinedTables.contains(relacion.getDesdeTabla())) {
                    joinString += " " + relacion.getTipo().toUpperCase() + " JOIN " +
                            relacion.getHastaTabla() + " ON " +
                            relacion.getDesdeTabla() + "." + relacion.getDesdeColumna() + " = " +
                            relacion.getHastaTabla() + "." + relacion.getHastaColumna();
                    joinedTables.add(relacion.getHastaTabla());
                    added = true;
                }
            }
        } while (added);

        String queryString;
        String queryCountString;

        if (!groupString.isEmpty()) {
            // Fix grouping logic: include joins and use t alias in subquery
            queryString = "SELECT t.* FROM " + paginateList.getTable() + " t "
                    + joinString
                    + " INNER JOIN (SELECT MAX(t.id) AS id FROM " + paginateList.getTable() + " t "
                    + joinString
                    + whereString
                    + groupString
                    + ") sub ON t.id = sub.id "
                    + orderString + limitString;

            // Conteo de grupos
            queryCountString = "SELECT count(*) FROM (SELECT MAX(t.id) as id FROM "
                    + paginateList.getTable() + " t "
                    + joinString
                    + whereString
                    + groupString + ") sub";

        } else {
            queryString = "SELECT t.* FROM " + paginateList.getTable() + " t"
                    + joinString + whereString + groupString + orderString + limitString;

            if (!joinString.isEmpty()) {
                queryCountString = "SELECT count(DISTINCT t.id) FROM " + paginateList.getTable() + " t"
                        + joinString + whereString + groupString;
            } else {
                queryCountString = "SELECT count(*) FROM " + paginateList.getTable() + " t"
                        + joinString + whereString + groupString;
            }
        }

        return new RepositoryImplQuery(queryString, queryCountString);
    }

    private void logQuery(RepositoryImplQuery querys, List<KeyValue> keyValues) {
        // Logea la consulta realizada
        try {
            String queryString = querys.getSelect();
            for (KeyValue keyValue : keyValues) {
                queryString = queryString.replaceAll(":" + keyValue.getKey(), "\"" + keyValue.getValue().toString() + "\"");
            }
            logger.info("SQl impl select: {}", queryString);
        } catch (Exception e) {
            logger.warn("SQl impl select: Error al hacer el log");
        }
    }

    private void setParametersToQuery(Query queryDest, List<KeyValue> keyValues) {
        for (KeyValue keyValue : keyValues) {
            queryDest.setParameter(keyValue.getKey(), keyValue.getValue());
        }
    }

    public Page<T> createSql(PaginateList paginateList) {
        List<KeyValue> keyValues = new ArrayList<>();
        if (paginateList.getTable().isEmpty()) {
            throw new CustomException("No se seteo la tabla de busqueda");
        }
        
        RepositoryImplQuery querys = this.getSql(paginateList, keyValues);
        this.logQuery(querys, keyValues);

        // Conteo total
        Query queryCount = entityManager.createNativeQuery(querys.getCount());
        setParametersToQuery(queryCount, keyValues);
        long resultCount = Long.parseLong(queryCount.getSingleResult().toString());

        // Paginación real
        Pageable pageable = PageRequest.of(paginateList.getPage() - 1, paginateList.getSize());

        // Resultados
        Query querySelect = entityManager.createNativeQuery(querys.getSelect(), paginateList.getEntityClass());
        setParametersToQuery(querySelect, keyValues);
        @SuppressWarnings("unchecked")
        List<T> paginateListResult = querySelect.getResultList();

        return new PageImpl<>(paginateListResult, pageable, resultCount);
    }
}

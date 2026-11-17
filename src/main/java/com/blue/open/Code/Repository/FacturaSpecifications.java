package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class FacturaSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private FacturaSpecifications() {
    }

    public static Specification<Factura> forCompany(Long companyId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("company").get("id"), companyId),
                cb.equal(root.get("status"), STATUS_ACTIVE));
    }

    public static Specification<Factura> withEstados(List<FacturaEstadoEnum> estados) {
        return (root, query, cb) -> {
            if (estados == null || estados.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("estado").in(estados);
        };
    }
}

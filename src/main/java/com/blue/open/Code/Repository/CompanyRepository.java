package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyRepository extends BaseCompanyRepository {

    Company findByPath(String path);

    Company findFirstByCuit(String cuit);

    Company findFirstByCuitAndStatus(String cuit, Integer status);

    Company findFirstByEmailInstitucionalIgnoreCaseAndStatus(String emailInstitucional, Integer status);

    List<Company> findByCuotaSocialHabilitadaTrueAndStatus(Integer status);

    List<Company> findByStatus(Integer status);

    @Query(value = """
            SELECT c FROM Company c
            LEFT JOIN FETCH c.mercadoPago
            WHERE c.status = 0
              AND (
                   :search IS NULL
                   OR LOWER(c.cuit) LIKE :search
                   OR LOWER(c.razonSocial) LIKE :search
                   OR LOWER(c.nombreComercial) LIKE :search
              )
            ORDER BY c.razonSocial ASC, c.id ASC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Company c
            WHERE c.status = 0
              AND (
                   :search IS NULL
                   OR LOWER(c.cuit) LIKE :search
                   OR LOWER(c.razonSocial) LIKE :search
                   OR LOWER(c.nombreComercial) LIKE :search
              )
            """)
    Page<Company> paginate(@Param("search") String search, Pageable pageable);

    /** Indica si ya hay un club con ese cliente del CRM o con ese CUIT, sin filtrar por status. */
    boolean existsByUuidClienteOrCuit(String uuidCliente, String cuit);

    /** Indica si el path ya esta en uso, sin filtrar por status (el indice unico tampoco filtra). */
    boolean existsByPath(String path);

    Company findFirstByUuidCliente(String uuidCliente);

    Company findFirstByUuidClienteAndStatus(String uuidCliente, Integer status);
}

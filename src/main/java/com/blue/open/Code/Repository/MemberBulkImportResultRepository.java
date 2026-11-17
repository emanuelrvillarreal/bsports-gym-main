package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.MemberBulkImportResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberBulkImportResultRepository extends JpaRepository<MemberBulkImportResult, Long> {

    Optional<MemberBulkImportResult> findByImportId(String importId);

    Page<MemberBulkImportResult> findByCompanyIdOrderByDateCreatedDesc(Long companyId, Pageable pageable);
}

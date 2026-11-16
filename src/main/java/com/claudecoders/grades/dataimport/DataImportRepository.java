package com.claudecoders.grades.dataimport;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataImportRepository extends JpaRepository<DataImport, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DataImport d where d.id = :id")
    Optional<DataImport> findForConfirmation(@Param("id") Long id);
}

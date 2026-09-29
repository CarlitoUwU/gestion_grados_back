package com.claudecoders.grades.expedientstatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpedientStatusRepository extends JpaRepository<ExpedientStatus, Long> {

    boolean existsByNameIgnoreCase(String name);
}

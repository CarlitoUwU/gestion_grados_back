package com.claudecoders.grades.expedient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpedientRepository extends JpaRepository<Expedient, Long> {
    boolean existsByNumberIgnoreCase(String number);
}

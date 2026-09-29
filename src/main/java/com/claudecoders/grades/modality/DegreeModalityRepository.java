package com.claudecoders.grades.modality;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DegreeModalityRepository extends JpaRepository<DegreeModality, Long> {

    boolean existsByNameIgnoreCase(String name);
}

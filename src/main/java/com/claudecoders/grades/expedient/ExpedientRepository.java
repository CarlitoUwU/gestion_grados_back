package com.claudecoders.grades.expedient;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpedientRepository extends JpaRepository<Expedient, Long> {
    boolean existsByNumberIgnoreCase(String number);

    java.util.Optional<Expedient> findByNumberIgnoreCase(String number);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Expedient e where lower(e.number) = lower(:number)")
    java.util.Optional<Expedient> findForUpdateByNumber(@Param("number") String number);

    boolean existsByGraduateIdAndIdNot(Long graduateId, Long id);

    String SEARCH_FILTER =
            """
            from Expedient e
            left join e.graduate g
            left join g.school s
            left join e.modality m
            left join e.status st
            left join Resolution r on r.expedient = e
            left join ExpedientAdvisor a on a.expedient = e
            left join a.teacher at
            left join JuryMember jm on jm.expedient = e
            left join jm.teacher jt
            where (:schoolId is null or s.id = :schoolId)
              and (:statusId is null or st.id = :statusId)
              and (:modalityId is null or m.id = :modalityId)
              and (:year is null or year(e.startDate) = :year)
              and (
                    :search is null
                    or lower(e.number) like lower(concat('%', :search, '%'))
                    or lower(g.firstNames) like lower(concat('%', :search, '%'))
                    or lower(g.lastNames) like lower(concat('%', :search, '%'))
                    or lower(coalesce(g.fullName, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(g.documentNumber, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(g.studentCode, '')) like lower(concat('%', :search, '%'))
                    or exists (select rw.id from ResearchWork rw where rw.expedient = e
                               and lower(rw.title) like lower(concat('%', :search, '%')))
                    or lower(coalesce(s.name, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(g.academicProgram, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(r.number, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(at.fullName, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(jt.fullName, '')) like lower(concat('%', :search, '%'))
              )
            """;

    @EntityGraph(
            attributePaths = {
                "graduate",
                "graduate.school",
                "modality",
                "status",
                "createdBy",
                "updatedBy"
            })
    @Query("select distinct e " + SEARCH_FILTER + " order by e.startDate desc, e.id desc")
    List<Expedient> search(
            @Param("search") String search,
            @Param("schoolId") Long schoolId,
            @Param("statusId") Long statusId,
            @Param("modalityId") Long modalityId,
            @Param("year") Integer year);

    @EntityGraph(
            attributePaths = {
                "graduate",
                "graduate.school",
                "modality",
                "status",
                "createdBy",
                "updatedBy"
            })
    @Query(
            value = "select distinct e " + SEARCH_FILTER + " order by e.startDate desc, e.id desc",
            countQuery = "select count(distinct e) " + SEARCH_FILTER)
    org.springframework.data.domain.Page<Expedient> search(
            @Param("search") String search,
            @Param("schoolId") Long schoolId,
            @Param("statusId") Long statusId,
            @Param("modalityId") Long modalityId,
            @Param("year") Integer year,
            org.springframework.data.domain.Pageable pageable);
}

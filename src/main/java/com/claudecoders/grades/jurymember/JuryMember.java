package com.claudecoders.grades.jurymember;

import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.shared.audit.CreatedEntity;
import com.claudecoders.grades.teacher.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "jury_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_jury_members_expedient_teacher", columnNames = { "expedient_id", "teacher_id" })
})
public class JuryMember extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expedient_id", nullable = false)
    private Expedient expedient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolution_id")
    private Resolution resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "jury_role", nullable = false, length = 30)
    private JuryRole juryRole;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expedient getExpedient() {
        return expedient;
    }

    public void setExpedient(Expedient expedient) {
        this.expedient = expedient;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public JuryRole getJuryRole() {
        return juryRole;
    }

    public void setJuryRole(JuryRole juryRole) {
        this.juryRole = juryRole;
    }
}

package com.claudecoders.grades.jurydrawmember;

import com.claudecoders.grades.jurydraw.JuryDraw;
import com.claudecoders.grades.shared.audit.CreatedEntity;
import com.claudecoders.grades.teacher.Teacher;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "jury_draw_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_jury_draw_members_draw_teacher", columnNames = { "jury_draw_id", "teacher_id" })
})
public class JuryDrawMember extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jury_draw_id", nullable = false)
    private JuryDraw juryDraw;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JuryDraw getJuryDraw() {
        return juryDraw;
    }

    public void setJuryDraw(JuryDraw juryDraw) {
        this.juryDraw = juryDraw;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
}

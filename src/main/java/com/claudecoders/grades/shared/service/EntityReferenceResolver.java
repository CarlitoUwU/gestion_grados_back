package com.claudecoders.grades.shared.service;

import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedient.ExpedientRepository;
import com.claudecoders.grades.expedientstatus.ExpedientStatus;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.graduate.Graduate;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.jurydraw.JuryDraw;
import com.claudecoders.grades.jurydraw.JuryDrawRepository;
import com.claudecoders.grades.modality.DegreeModality;
import com.claudecoders.grades.modality.DegreeModalityRepository;
import com.claudecoders.grades.resolution.Resolution;
import com.claudecoders.grades.resolution.ResolutionRepository;
import com.claudecoders.grades.school.School;
import com.claudecoders.grades.school.SchoolRepository;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.teacher.Teacher;
import com.claudecoders.grades.teacher.TeacherRepository;
import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class EntityReferenceResolver {
    private final ExpedientRepository expedients;
    private final ExpedientStatusRepository statuses;
    private final GraduateRepository graduates;
    private final JuryDrawRepository juryDraws;
    private final DegreeModalityRepository modalities;
    private final ResolutionRepository resolutions;
    private final SchoolRepository schools;
    private final TeacherRepository teachers;
    private final UserRepository users;

    public EntityReferenceResolver(
            ExpedientRepository expedients,
            ExpedientStatusRepository statuses,
            GraduateRepository graduates,
            JuryDrawRepository juryDraws,
            DegreeModalityRepository modalities,
            ResolutionRepository resolutions,
            SchoolRepository schools,
            TeacherRepository teachers,
            UserRepository users) {
        this.expedients = expedients;
        this.statuses = statuses;
        this.graduates = graduates;
        this.juryDraws = juryDraws;
        this.modalities = modalities;
        this.resolutions = resolutions;
        this.schools = schools;
        this.teachers = teachers;
        this.users = users;
    }

    public Expedient expedient(Long id) {
        return expedients
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
    }

    public ExpedientStatus status(Long id) {
        return id == null
                ? null
                : statuses.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("ExpedientStatus", id));
    }

    public Graduate graduate(Long id) {
        return graduates
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Graduate", id));
    }

    public JuryDraw juryDraw(Long id) {
        return juryDraws
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JuryDraw", id));
    }

    public DegreeModality modality(Long id) {
        return id == null
                ? null
                : modalities
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("DegreeModality", id));
    }

    public Resolution resolution(Long id) {
        return id == null
                ? null
                : resolutions
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Resolution", id));
    }

    public School school(Long id) {
        return id == null
                ? null
                : schools.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("School", id));
    }

    public Teacher teacher(Long id) {
        return teachers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }

    public User user(Long id) {
        return id == null
                ? null
                : users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}

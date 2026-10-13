-- PK y UNIQUE de V1 ya crean sus propios índices; evitar duplicarlos.
-- Filtros y orden de la búsqueda de expedientes.
CREATE INDEX idx_graduates_school_id ON graduates (school_id);
CREATE INDEX idx_expedients_graduate_id ON expedients (graduate_id);
CREATE INDEX idx_expedients_status_id ON expedients (status_id);
CREATE INDEX idx_expedients_modality_id ON expedients (modality_id);
CREATE INDEX idx_expedients_start_date_id ON expedients (start_date DESC, id DESC);

-- Consultas de detalle e historial, ordenadas dentro de cada expediente/docente.
CREATE INDEX idx_resolutions_expedient_date_id
    ON resolutions (expedient_id, resolution_date DESC, id DESC);
CREATE INDEX idx_expedient_advisors_expedient_date_id
    ON expedient_advisors (expedient_id, assigned_at DESC, id DESC);
CREATE INDEX idx_expedient_advisors_teacher_date_id
    ON expedient_advisors (teacher_id, assigned_at DESC, id DESC);
CREATE INDEX idx_jury_members_teacher_id ON jury_members (teacher_id);
CREATE INDEX idx_jury_draws_expedient_date_id
    ON jury_draws (expedient_id, draw_date DESC, id DESC);

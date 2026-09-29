-- Esquema inicial del sistema de grados y títulos.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    google_subject VARCHAR(255) UNIQUE,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN_GT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE graduates (
    id BIGSERIAL PRIMARY KEY,
    document_number VARCHAR(30),
    first_names VARCHAR(150) NOT NULL,
    last_names VARCHAR(150) NOT NULL,
    school_id BIGINT REFERENCES schools(id),
    academic_program VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE degree_modalities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expedient_statuses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expedients (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(100) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    graduate_id BIGINT NOT NULL REFERENCES graduates(id),
    modality_id BIGINT REFERENCES degree_modalities(id),
    status_id BIGINT REFERENCES expedient_statuses(id),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE research_works (
    id BIGSERIAL PRIMARY KEY,
    expedient_id BIGINT NOT NULL UNIQUE REFERENCES expedients(id) ON DELETE CASCADE,
    work_type VARCHAR(50),
    title TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    document_number VARCHAR(30),
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resolutions (
    id BIGSERIAL PRIMARY KEY,
    expedient_id BIGINT NOT NULL REFERENCES expedients(id) ON DELETE CASCADE,
    number VARCHAR(100) NOT NULL,
    resolution_date DATE,
    resolution_type VARCHAR(50),
    file_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_resolutions_expedient_number UNIQUE (expedient_id, number)
);

CREATE TABLE expedient_advisors (
    id BIGSERIAL PRIMARY KEY,
    expedient_id BIGINT NOT NULL REFERENCES expedients(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    resolution_id BIGINT REFERENCES resolutions(id),
    assigned_at DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jury_members (
    id BIGSERIAL PRIMARY KEY,
    expedient_id BIGINT NOT NULL REFERENCES expedients(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    resolution_id BIGINT REFERENCES resolutions(id),
    jury_role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_jury_role CHECK (
        jury_role IN ('PRESIDENTE', 'SECRETARIO', 'VOCAL', 'ACCESITARIO')
    ),
    CONSTRAINT uq_jury_members_expedient_teacher UNIQUE (expedient_id, teacher_id)
);

CREATE TABLE jury_draws (
    id BIGSERIAL PRIMARY KEY,
    expedient_id BIGINT NOT NULL REFERENCES expedients(id) ON DELETE CASCADE,
    resolution_id BIGINT REFERENCES resolutions(id),
    draw_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jury_draw_members (
    id BIGSERIAL PRIMARY KEY,
    jury_draw_id BIGINT NOT NULL REFERENCES jury_draws(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_jury_draw_members_draw_teacher UNIQUE (jury_draw_id, teacher_id)
);

CREATE TABLE defenses (
    id BIGSERIAL PRIMARY KEY,
    expedient_id BIGINT NOT NULL UNIQUE REFERENCES expedients(id) ON DELETE CASCADE,
    defense_date DATE,
    result VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE data_imports (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    imported_by BIGINT NOT NULL REFERENCES users(id),
    total_records INTEGER NOT NULL DEFAULT 0,
    imported_records INTEGER NOT NULL DEFAULT 0,
    rejected_records INTEGER NOT NULL DEFAULT 0,
    observed_records INTEGER NOT NULL DEFAULT 0,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

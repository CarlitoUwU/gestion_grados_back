ALTER TABLE expedients
    ADD COLUMN degree VARCHAR(150),
    ADD COLUMN observations TEXT;

ALTER TABLE graduates
    ADD COLUMN full_name VARCHAR(300),
    ADD COLUMN student_code VARCHAR(50);

CREATE INDEX idx_graduates_document_number ON graduates (document_number);
CREATE INDEX idx_graduates_student_code ON graduates (student_code);

ALTER TABLE data_imports
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN staged_records TEXT;

ALTER TABLE data_imports
    ADD CONSTRAINT chk_data_imports_status
        CHECK (status IN ('PREVIEW', 'COMPLETED'));

package com.claudecoders.grades.dataimport;

import com.claudecoders.grades.dataimport.dto.DataImportRequest;
import com.claudecoders.grades.dataimport.dto.DataImportResponse;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataImportService {
    private final DataImportRepository repository;
    private final EntityReferenceResolver refs;

    public DataImportService(DataImportRepository repository, EntityReferenceResolver refs) {
        this.repository = repository;
        this.refs = refs;
    }

    public List<DataImportResponse> findAll() {
        return repository.findAll().stream().map(DataImportResponse::from).toList();
    }

    public DataImportResponse findById(Long id) {
        return DataImportResponse.from(get(id));
    }

    @Transactional
    public DataImportResponse create(DataImportRequest r) {
        return DataImportResponse.from(repository.save(toEntity(new DataImport(), r)));
    }

    @Transactional
    public DataImportResponse update(Long id, DataImportRequest r) {
        return DataImportResponse.from(toEntity(get(id), r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private DataImport get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DataImport", id));
    }

    private DataImport toEntity(DataImport v, DataImportRequest r) {
        v.setFileName(r.fileName().trim());
        v.setImportedBy(refs.user(r.importedById()));
        v.setTotalRecords(r.totalRecords());
        v.setImportedRecords(r.importedRecords());
        v.setRejectedRecords(r.rejectedRecords());
        v.setObservedRecords(r.observedRecords());
        return v;
    }
}

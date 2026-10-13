package com.claudecoders.grades.dataimport;

import com.claudecoders.grades.auth.CurrentUserService;
import com.claudecoders.grades.dataimport.dto.DataImportRequest;
import com.claudecoders.grades.dataimport.dto.DataImportResponse;
import com.claudecoders.grades.dataimport.dto.ImportPreviewResponse;
import com.claudecoders.grades.dataimport.dto.ImportRowPreview;
import com.claudecoders.grades.dataimport.dto.StagedGraduateRecord;
import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedient.ExpedientRepository;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.graduate.Graduate;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.modality.DegreeModalityRepository;
import com.claudecoders.grades.researchwork.ResearchWork;
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.school.SchoolRepository;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.shared.service.EntityReferenceResolver;
import com.claudecoders.grades.user.User;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
public class DataImportService {
    private static final List<String> REQUIRED_COLUMNS =
            List.of("expedientNumber", "startDate", "firstNames", "lastNames", "researchTitle");
    private final DataImportRepository repository;
    private final EntityReferenceResolver refs;
    private final CurrentUserService currentUser;
    private final ExpedientRepository expedients;
    private final GraduateRepository graduates;
    private final ResearchWorkRepository researchWorks;
    private final SchoolRepository schools;
    private final DegreeModalityRepository modalities;
    private final ExpedientStatusRepository statuses;
    private final JsonMapper objectMapper;
    private final Clock clock;

    public DataImportService(
            DataImportRepository repository,
            EntityReferenceResolver refs,
            CurrentUserService currentUser,
            ExpedientRepository expedients,
            GraduateRepository graduates,
            ResearchWorkRepository researchWorks,
            SchoolRepository schools,
            DegreeModalityRepository modalities,
            ExpedientStatusRepository statuses,
            JsonMapper objectMapper,
            Clock clock) {
        this.repository = repository;
        this.refs = refs;
        this.currentUser = currentUser;
        this.expedients = expedients;
        this.graduates = graduates;
        this.researchWorks = researchWorks;
        this.schools = schools;
        this.modalities = modalities;
        this.statuses = statuses;
        this.objectMapper = objectMapper;
        this.clock = clock;
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

    @Transactional
    public ImportPreviewResponse preview(MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Seleccione un archivo XLSX no vacío");
        }
        String fileName =
                file.getOriginalFilename() == null ? "import.xlsx" : file.getOriginalFilename();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw badRequest("El archivo debe tener extensión .xlsx");
        }

        User user = currentUser.from(authentication);
        List<ImportRowPreview> rowResults = new ArrayList<>();
        List<StagedGraduateRecord> validRows = new ArrayList<>();
        Set<String> seenNumbers = new HashSet<>();
        int rejected = 0;
        int observed = 0;
        try (var workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw badRequest("El libro no contiene hojas");
            var sheet = workbook.getSheetAt(0);
            Row header = findFirstNonEmptyRow(sheet);
            if (header == null) throw badRequest("La hoja no contiene encabezados");
            Map<String, Integer> columns = headerIndexes(header);
            for (String required : REQUIRED_COLUMNS) {
                if (!columns.containsKey(required)) {
                    throw badRequest("Falta la columna obligatoria: " + headerName(required));
                }
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int dataRows = 0;
            for (int index = header.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (isEmpty(row, formatter, evaluator)) continue;
                dataRows++;
                List<String> errors = new ArrayList<>();
                List<String> notes = new ArrayList<>();
                String number = cell(row, columns, "expedientNumber", formatter, evaluator);
                LocalDate startDate = readDate(row, columns.get("startDate"), formatter, evaluator);
                String firstNames = cell(row, columns, "firstNames", formatter, evaluator);
                String lastNames = cell(row, columns, "lastNames", formatter, evaluator);
                String title = cell(row, columns, "researchTitle", formatter, evaluator);
                if (number.isBlank()) errors.add("Número de expediente obligatorio");
                if (startDate == null) errors.add("Fecha de inicio inválida; use AAAA-MM-DD");
                else if (startDate.isAfter(LocalDate.now(clock)))
                    errors.add("La fecha de inicio no puede ser posterior a hoy en Lima");
                if (firstNames.isBlank()) errors.add("Nombres obligatorios");
                if (lastNames.isBlank()) errors.add("Apellidos obligatorios");
                if (title.isBlank()) errors.add("Título del trabajo obligatorio");
                if (number.length() > 100) errors.add("Número de expediente supera 100 caracteres");
                if (firstNames.length() > 150) errors.add("Nombres superan 150 caracteres");
                if (lastNames.length() > 150) errors.add("Apellidos superan 150 caracteres");
                String documentNumber = cell(row, columns, "documentNumber", formatter, evaluator);
                String academicProgram =
                        cell(row, columns, "academicProgram", formatter, evaluator);
                if (documentNumber.length() > 30) errors.add("Documento supera 30 caracteres");
                if (academicProgram.length() > 200)
                    errors.add("Programa académico supera 200 caracteres");

                if (errors.isEmpty()) {
                    String normalized = number.toLowerCase(Locale.ROOT);
                    if (!seenNumbers.add(normalized)
                            || expedients.existsByNumberIgnoreCase(number)) {
                        notes.add("Expediente duplicado en el archivo o en el sistema");
                    }
                    String schoolName = cell(row, columns, "schoolName", formatter, evaluator);
                    String modalityName = cell(row, columns, "modalityName", formatter, evaluator);
                    String statusName = cell(row, columns, "statusName", formatter, evaluator);
                    if (!schoolName.isBlank() && schools.findByNameIgnoreCase(schoolName).isEmpty())
                        notes.add("Escuela no encontrada en el catálogo");
                    if (!modalityName.isBlank()
                            && modalities.findByNameIgnoreCase(modalityName).isEmpty())
                        notes.add("Modalidad no encontrada en el catálogo");
                    if (!statusName.isBlank()
                            && statuses.findByNameIgnoreCase(statusName).isEmpty())
                        notes.add("Estado no encontrado en el catálogo");
                    if (notes.isEmpty()) {
                        validRows.add(
                                new StagedGraduateRecord(
                                        index + 1,
                                        number,
                                        startDate,
                                        documentNumber,
                                        firstNames,
                                        lastNames,
                                        schoolName,
                                        academicProgram,
                                        modalityName,
                                        statusName,
                                        title));
                    }
                }
                String resultStatus =
                        !errors.isEmpty() ? "REJECTED" : notes.isEmpty() ? "VALID" : "OBSERVED";
                if (resultStatus.equals("REJECTED")) rejected++;
                if (resultStatus.equals("OBSERVED")) observed++;
                rowResults.add(
                        new ImportRowPreview(
                                index + 1, resultStatus, errors.isEmpty() ? notes : errors));
            }
            if (dataRows == 0) throw badRequest("La hoja no contiene filas de datos");
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ResponseStatusException responseStatusException)
                throw responseStatusException;
            throw badRequest("No se pudo leer el XLSX: verifique que no esté dañado o protegido");
        }

        DataImport dataImport = new DataImport();
        dataImport.setFileName(fileName);
        dataImport.setImportedBy(user);
        dataImport.setTotalRecords(rowResults.size());
        dataImport.setImportedRecords(0);
        dataImport.setRejectedRecords(rejected);
        dataImport.setObservedRecords(observed);
        dataImport.setStatus("PREVIEW");
        try {
            dataImport.setStagedRecords(objectMapper.writeValueAsString(validRows));
        } catch (JacksonException exception) {
            throw new IllegalStateException("No se pudo guardar la previsualización", exception);
        }
        dataImport = repository.save(dataImport);
        return new ImportPreviewResponse(
                dataImport.getId(),
                fileName,
                dataImport.getStatus(),
                dataImport.getTotalRecords(),
                validRows.size(),
                rejected,
                observed,
                List.of(
                        "numero_expediente",
                        "fecha_inicio",
                        "nombres",
                        "apellidos",
                        "titulo_trabajo"),
                rowResults);
    }

    @Transactional
    public DataImportResponse confirm(Long id, Authentication authentication) {
        DataImport dataImport = get(id);
        if (!"PREVIEW".equals(dataImport.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La importación ya fue confirmada");
        }
        User user = currentUser.from(authentication);
        if (!dataImport.getImportedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "La previsualización pertenece a otro usuario");
        }
        List<StagedGraduateRecord> records;
        try {
            records =
                    objectMapper.readValue(dataImport.getStagedRecords(), new TypeReference<>() {});
        } catch (JacksonException exception) {
            throw new IllegalStateException("No se pudo recuperar la previsualización", exception);
        }
        for (StagedGraduateRecord record : records) {
            if (expedients.existsByNumberIgnoreCase(record.expedientNumber())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El expediente "
                                + record.expedientNumber()
                                + " se creó desde la previsualización");
            }
            if (blankToNull(record.schoolName()) != null
                    && schools.findByNameIgnoreCase(record.schoolName()).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El catálogo de escuelas cambió desde la previsualización");
            }
            if (blankToNull(record.modalityName()) != null
                    && modalities.findByNameIgnoreCase(record.modalityName()).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El catálogo de modalidades cambió desde la previsualización");
            }
            if (blankToNull(record.statusName()) != null
                    && statuses.findByNameIgnoreCase(record.statusName()).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El catálogo de estados cambió desde la previsualización");
            }
            Graduate graduate = new Graduate();
            graduate.setDocumentNumber(blankToNull(record.documentNumber()));
            graduate.setFirstNames(record.firstNames());
            graduate.setLastNames(record.lastNames());
            graduate.setAcademicProgram(blankToNull(record.academicProgram()));
            graduate.setSchool(
                    blankToNull(record.schoolName()) == null
                            ? null
                            : schools.findByNameIgnoreCase(record.schoolName()).orElse(null));
            graduate = graduates.save(graduate);

            Expedient expedient = new Expedient();
            expedient.setNumber(record.expedientNumber());
            expedient.setStartDate(record.startDate());
            expedient.setGraduate(graduate);
            expedient.setCreatedBy(user);
            expedient.setUpdatedBy(user);
            expedient.setModality(
                    blankToNull(record.modalityName()) == null
                            ? null
                            : modalities.findByNameIgnoreCase(record.modalityName()).orElse(null));
            expedient.setStatus(
                    blankToNull(record.statusName()) == null
                            ? null
                            : statuses.findByNameIgnoreCase(record.statusName()).orElse(null));
            expedient = expedients.save(expedient);

            ResearchWork researchWork = new ResearchWork();
            researchWork.setExpedient(expedient);
            researchWork.setTitle(record.researchTitle());
            researchWorks.save(researchWork);
        }
        dataImport.setImportedRecords(records.size());
        dataImport.setStatus("COMPLETED");
        dataImport.setStagedRecords(null);
        return DataImportResponse.from(dataImport);
    }

    private DataImport get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DataImport", id));
    }

    private DataImport toEntity(DataImport value, DataImportRequest request) {
        value.setFileName(request.fileName().trim());
        value.setImportedBy(refs.user(request.importedById()));
        value.setTotalRecords(request.totalRecords());
        value.setImportedRecords(request.importedRecords());
        value.setRejectedRecords(request.rejectedRecords());
        value.setObservedRecords(request.observedRecords());
        return value;
    }

    private static Row findFirstNonEmptyRow(org.apache.poi.ss.usermodel.Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        for (Row row : sheet) if (!isEmpty(row, formatter, null)) return row;
        return null;
    }

    private static Map<String, Integer> headerIndexes(Row row) {
        Map<String, Integer> result = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String header = normalize(formatter.formatCellValue(row.getCell(i)));
            String field =
                    switch (header) {
                        case "numero expediente", "numero de expediente", "expedientenumber" ->
                                "expedientNumber";
                        case "fecha inicio", "fecha de inicio", "startdate" -> "startDate";
                        case "documento", "numero documento", "documentnumber" -> "documentNumber";
                        case "nombres", "firstnames" -> "firstNames";
                        case "apellidos", "lastnames" -> "lastNames";
                        case "escuela", "schoolname" -> "schoolName";
                        case "programa academico", "programa académico", "academicprogram" ->
                                "academicProgram";
                        case "modalidad", "modalityname" -> "modalityName";
                        case "estado", "statusname" -> "statusName";
                        case "titulo trabajo", "titulo del trabajo", "researchtitle" ->
                                "researchTitle";
                        default -> null;
                    };
            if (field != null) result.put(field, i);
        }
        return result;
    }

    private static String cell(
            Row row,
            Map<String, Integer> columns,
            String key,
            DataFormatter formatter,
            org.apache.poi.ss.usermodel.FormulaEvaluator evaluator) {
        Integer index = columns.get(key);
        if (index == null || row.getCell(index) == null) return "";
        return formatter.formatCellValue(row.getCell(index), evaluator).trim();
    }

    private static LocalDate readDate(
            Row row,
            Integer index,
            DataFormatter formatter,
            org.apache.poi.ss.usermodel.FormulaEvaluator evaluator) {
        if (index == null || row.getCell(index) == null) return null;
        var cell = row.getCell(index);
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = formatter.formatCellValue(cell, evaluator).trim();
        for (DateTimeFormatter pattern :
                List.of(
                        DateTimeFormatter.ISO_LOCAL_DATE,
                        DateTimeFormatter.ofPattern("d/M/uuuu"),
                        DateTimeFormatter.ofPattern("d-M-uuuu"))) {
            try {
                return LocalDate.parse(value, pattern);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static boolean isEmpty(
            Row row,
            DataFormatter formatter,
            org.apache.poi.ss.usermodel.FormulaEvaluator evaluator) {
        if (row == null) return true;
        for (var cell : row)
            if (!formatter.formatCellValue(cell, evaluator).isBlank()) return false;
        return true;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : java.text.Normalizer.normalize(
                                value.trim().toLowerCase(Locale.ROOT),
                                java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replace('_', ' ')
                        .replaceAll("\\s+", " ");
    }

    private static String headerName(String field) {
        return switch (field) {
            case "expedientNumber" -> "numero_expediente";
            case "startDate" -> "fecha_inicio";
            case "firstNames" -> "nombres";
            case "lastNames" -> "apellidos";
            default -> "titulo_trabajo";
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

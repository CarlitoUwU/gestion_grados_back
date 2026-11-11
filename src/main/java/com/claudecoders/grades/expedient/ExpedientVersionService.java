package com.claudecoders.grades.expedient;

import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Versión del agregado del expediente para concurrencia optimista. El ETag se calcula sobre el
 * detalle completo, por lo que cualquier cambio en datos generales, resoluciones, asesor, jurado,
 * sorteos o sustentación produce una versión distinta.
 */
@Service
public class ExpedientVersionService {
    private final ExpedientQueryService queries;
    private final ExpedientRepository expedients;
    private final JsonMapper mapper;

    public ExpedientVersionService(
            ExpedientQueryService queries, ExpedientRepository expedients, JsonMapper mapper) {
        this.queries = queries;
        this.expedients = expedients;
        this.mapper = mapper;
    }

    public String etag(ExpedientApiModels.Detail detail) {
        try {
            byte[] hash =
                    MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(detail));
            return "\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }

    /**
     * Bloquea el expediente y comprueba If-Match dentro de la transacción de escritura. La cabecera
     * es obligatoria: sin ella responde 428 para no sobrescribir cambios ajenos.
     */
    public void requireMatch(Long id, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank())
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "Falta If-Match: consulta el detalle del expediente y envía su ETag.");
        expedients
                .findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expedient", id));
        if ("*".equals(ifMatch.trim())) return;
        String current = etag(queries.detail(id));
        boolean matches =
                Arrays.stream(ifMatch.split(",")).map(String::trim).anyMatch(current::equals);
        if (!matches)
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "El expediente cambió en el servidor desde la última consulta. Recarga el"
                            + " detalle antes de guardar.");
    }
}

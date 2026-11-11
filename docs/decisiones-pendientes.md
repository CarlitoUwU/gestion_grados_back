# Decisiones y alcance de los ajustes backend

Fecha de registro: 2026-11-01.

## Correcciones compatibles implementadas

1. **Autorización efectiva en todos los perfiles**
   - Las rutas de negocio exigen JWT; salud, Swagger y OpenAPI siguen públicas.
   - Se valida firma/`issuer`/`audience`/expiración con la configuración OAuth2.
   - 401 para token ausente, inválido o vencido; 403 para identidad válida no
     registrada, inactiva, con rol no permitido o fuera de los dominios
     institucionales configurados.
   - `dev` y `test` ya no permiten operaciones anónimas fuera de Expedientes.
   - Configuración: `ALLOWED_ROLES` y `ALLOWED_EMAIL_DOMAINS`.

2. **Dashboard compatible**
   - Se conservan los nombres `statusDistribution`, `upcomingDefenses` y
     `recentExpedients`.
   - Se agregan `asOfDate` y `timeZone`, usando un único corte con el `Clock` de
     `America/Lima`.
   - `year` (1900–9999) y `schoolId` (positivo) se validan con 400 y se aplican a
     conteos, distribución y listas.
   - Se corrigió el orden de recientes para que los `updatedAt` nulos queden al
     final y los empates se resuelvan por ID descendente.
   - Las listas siguen siendo arrays de `ExpedientApiModels.Summary`.

3. **Metadatos de última actualización**
   - Crear, actualizar o eliminar una resolución actualiza `updatedAt`/`updatedBy`
     del expediente en la misma transacción, con el usuario del JWT.
   - El detalle, las búsquedas y los recientes devuelven esos metadatos.
   - Eliminar una resolución referenciada devuelve 409; mover una resolución
     referenciada a otro expediente también devuelve 409.
   - La migración `V4` conserva los campos nullable `degree`, `observations`,
     `studentCode` y `full_name` del contrato de expedientes unificado.

4. **Errores y OpenAPI**
   - `ApiError` se documenta con `timestamp`, `status`, `error`, `message` y
     `validationErrors`.
   - Las rutas de negocio documentan Bearer JWT y respuestas 400/401/403/404/409/500
     según correspondan.
   - Se agregó un manejador 500 sin exponer detalles internos.

## Criterios *PO* adoptados para QA, pendientes de ratificación

Estos criterios están implementados como propuesta para permitir la comparación
QA con datos reales. No se consideran una decisión de negocio cerrada:

| Tema | Criterio implementado |
| --- | --- |
| `active` | Nombre de estado `Registrado`, `En trámite` u `Observado` (sin distinguir mayúsculas ni tildes). |
| `observed` | Nombre de estado que contiene `observ`, sin distinguir mayúsculas. |
| `upcomingDefenses` | Hasta 5 con fecha desde el corte inclusive y resultado nulo, vacío o `Pendiente`; fecha ASC, ID ASC. |
| `recentExpedients` | Hasta 8 por `updatedAt` DESC, nulos al final, ID DESC como desempate. |
| `defended` | Fecha hasta el corte inclusive y resultado no nulo, no vacío y distinto de `Pendiente`. |

**Decisión provisional (2026-11-10):** mientras PO no confirme, el año de los
reportes y del panel es el **año de inicio** del expediente (`startDate`). Las
estadísticas de graduados no distinguen aún año de egreso.

Si PO ratifica otro criterio, deben actualizarse conjuntamente el servicio, la
consulta SQL de comparación y este contrato. El frontend muestra lo que responde
la API; no se renombraron campos ni se envolvieron arrays en objetos.

## Documento digital implementado y ampliaciones pendientes

1. **Documento digital de resolución — implementado el 2026-11-02**
   - Contrato de BACK_OBSERVACIONES.md: `POST/GET
     /api/v1/resolutions/{id}/document`, PDF de hasta 10 MiB y descarga autenticada.
   - El documento se almacena en PostgreSQL. Ver README para validaciones y
     respuestas 400/404/413. El frontend no se modifica.

2. **Concurrencia optimista del reemplazo académico — implementada el 2026-11-10**
   - `GET /expedients/{id}/detail` devuelve un `ETag` fuerte calculado sobre el
     detalle completo; cualquier mutación del agregado (datos generales, CRUD de
     resoluciones, asesor, jurado, sorteos, sustentación, PDF) cambia la versión.
   - `PUT /expedients/{id}/academic` exige `If-Match` (decisión del 2026-11-10): sin
     cabecera responde 428. Con cabecera bloquea la fila del expediente, compara la
     versión y responde 412 sin modificar datos si no coincide; `*` solo exige que
     exista.
   - CORS permite `If-Match` y expone `ETag`. El frontend envía la versión leída antes
     de cada reemplazo académico y trata el 412 como sección modificada.
   - Los clientes que integren `/academic` deben leer antes el detalle y reenviar
     su `ETag`; el navegador lo recibe gracias a `Access-Control-Expose-Headers`.

3. **Documento digital en el frontend — integrado el 2026-11-10**
   - El control de subida está activo por defecto (`VITE_FEATURE_RESOLUTION_PDF=false`
     lo oculta), con el límite de 10 MiB del backend. Los PDF almacenados se
     descargan con Bearer JWT; los enlaces externos siguen abriéndose como antes.

## Límites que no se agregaron como bloqueantes

- RF-GT-P01 (18 meses), P02 (15 días) y P03 (aprobación de plan) siguen pendientes
  de formalización; no se incorporaron como validaciones bloqueantes.
- Los tipos de resolución siguen siendo texto libre, no un catálogo cerrado.
- Docente no tiene escuela propia; no se inventó esa restricción.
- La búsqueda por número de resolución continúa en cliente sobre el array
  completo; filtros/paginación de servidor son una mejora futura.

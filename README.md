# Gestión de grados - Backend

Backend Spring Boot para el registro de graduandos, expedientes y sustentaciones.
PostgreSQL se inicializa con Flyway al iniciar la API; no se requiere instalar Java
ni PostgreSQL en el host.

## Levantar con Docker

Desde la raíz del proyecto, levantar la API y PostgreSQL:

```sh
docker compose -f compose.dev.yml up --build -d
```

Ver los logs de la API:

```sh
docker compose -f compose.dev.yml logs -f api
```

Detener los servicios:

```sh
docker compose -f compose.dev.yml down
```

## Health check

Con los contenedores levantados, abrir:

```text
http://localhost:8080/health
```

Respuesta esperada:

```text
gestion-grados-backend OK
```

La documentación OpenAPI, cuando se agreguen controladores, estará en `/api/docs`;
las rutas de controladores se versionarán bajo `/api/v1`.

## API y Swagger

La API pública del backend está versionada bajo:

```text
http://localhost:8080/api/v1
```

La documentación Swagger UI está disponible en:

```text
http://localhost:8080/api/docs
```

El frontend debe configurar su URL base como:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

El sistema usa autenticación Bearer JWT de Google en producción. El JWT debe
corresponder a un usuario activo registrado en `users`; actualmente solo se maneja
el rol operativo `ADMIN_GT`.

Endpoints agregados para sincronizar con el frontend:

- `GET /api/v1/auth/me`
- `GET /api/v1/catalogs/schools`
- `GET /api/v1/catalogs/degree-modalities`
- `GET /api/v1/catalogs/expedient-statuses`
- `GET /api/v1/catalogs/teachers`
- `GET /api/v1/dashboard`
- `GET /api/v1/expedients/search`
- `GET /api/v1/expedients/{id}/detail`
- `GET /api/v1/teachers/{id}/history`
- `GET /api/v1/reports/statistics`
- `GET /api/v1/reports/teachers`
- `GET /api/v1/reports/draws`
- `GET /api/v1/reports/defended-works`
- `GET /api/v1/accesses`
- `POST /api/v1/accesses`
- `PATCH /api/v1/accesses/{id}/active`

## API unificada de expedientes

`GET /api/v1/expedients` acepta `search`, `schoolId`, `statusId`, `modalityId`,
`year`, `page` (desde 0) y `size` (1 a 200, por defecto 20). Devuelve `content`,
`page`, `size`, `totalElements`, `totalPages`, `first` y `last`. La búsqueda también
incluye nombre completo, documento y código del estudiante.

`GET`, `PUT` y `DELETE /api/v1/expedients/{number}` usan el código del expediente,
por ejemplo `EXP-2026-0001`. El detalle incluye `summary`, `graduate`, `research`,
`degree`, `observations`, `resolutions`, `advisor`, `jury`, `draws` y `defense`.
Se conservan `GET /expedients/search` y `GET /expedients/{id}/detail`.

`POST /api/v1/expedients` y `PUT /api/v1/expedients/{number}` reciben:

```json
{
  "number": "EXP-2026-0001",
  "startDate": "2026-03-15",
  "degree": "Título profesional",
  "modalityId": 1,
  "statusId": 1,
  "observations": "Expediente ingresado en mesa de partes.",
  "graduate": {
    "fullName": "Sofia Valdivia Paredes",
    "documentNumber": "72345678",
    "studentCode": "20193456",
    "schoolId": 1,
    "academicProgram": "Pregrado"
  },
  "research": { "title": "Arquitectura de microservicios para gestión académica" }
}
```

La operación es transaccional. Se reutiliza un graduando identificado por documento
o código de estudiante; las coincidencias ambiguas devuelven `409`. El nombre completo
se conserva tal como se recibe. Los datos del graduando reutilizado se actualizan con
el payload, por lo que el cambio también se refleja en otros expedientes del graduando.
`research` es opcional; si se omite en una actualización, el trabajo existente se conserva.
Los IDs de escuela, modalidad y estado deben existir en sus catálogos.

Las siguientes operaciones devuelven el detalle actualizado:

| Método | Ruta bajo `/api/v1/expedients/{number}` | Payload |
| --- | --- | --- |
| PATCH | `/status` | `status` (nombre de catálogo), `observations` |
| PUT | `/advisor` | `teacherId`, `resolutionNumber`, `assignedAt` |
| PUT | `/jury` | `resolutionNumber`, `members: [{teacherId, role}]` |
| POST | `/draw` | `drawDate`, `resolutionNumber`, `teacherIds` |
| PUT | `/defense` | `defenseDate`, `result` |

El jurado requiere exactamente tres docentes distintos con los cargos Presidente,
Secretario y Vocal, sin distinguir mayúsculas. Asesor y sustentación se actualizan si
ya existen. Las resoluciones se reutilizan por número dentro del mismo expediente.
Eliminar un expediente también elimina sus registros académicos asociados; el graduando
permanece disponible para otros expedientes.

Con la API de desarrollo levantada, ejecutar la verificación de integración:

```sh
python scripts/verify_expedient_api.py
```

La verificación crea datos temporales y los elimina al terminar. Comprueba CRUD,
paginación, reutilización de graduando, validaciones, rollback y proceso académico.
Para otra URL, pasar la base `/api/v1` como argumento. En un entorno autenticado,
configurar `API_TOKEN` con el JWT del usuario autorizado.

Después de cambiar dependencias Gradle, reconstruir y recrear el proceso de la API
para actualizar el classpath de Spring Boot:

```sh
docker compose -f compose.dev.yml build --no-cache api
docker compose -f compose.dev.yml up -d --force-recreate api
```

## Importación histórica XLSX

La carga histórica tiene dos pasos y requiere un usuario autorizado. Envíe el libro
`.xlsx` como multipart en `POST /api/v1/data-imports/preview`, con el campo `file`.
Se procesa la primera hoja; los encabezados pueden estar en español o en los nombres
de campo camelCase. Las columnas requeridas son `numero_expediente`, `fecha_inicio`,
`nombres`, `apellidos` y `titulo_trabajo`. También se aceptan `documento`, `escuela`,
`programa_academico`, `modalidad` y `estado`. Las escuelas, modalidades y estados
opcionales deben existir previamente en sus catálogos.

La respuesta clasifica cada fila como válida, observada (duplicado o catálogo sin
coincidencia) o rechazada (dato requerido inválido). Solo las filas válidas quedan
preparadas para carga. Confirme explícitamente con `POST /api/v1/data-imports/{id}/confirm`;
la inserción de graduando, expediente y trabajo se realiza en una transacción. El lote
pendiente conserva el usuario, nombre de archivo y conteos para auditoría.

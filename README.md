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

La caché de dependencias Gradle se conserva en el volumen Docker `gradle-cache`.
Esto evita leer los JAR de dependencias a través del montaje del disco de Windows.

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

El sistema usa autenticación Bearer JWT de Google en todos los perfiles. La API
valida firma, `issuer`, `audience` y expiración desde la configuración OAuth2. Un
token ausente, inválido o vencido devuelve 401. Una identidad válida que no está
registrada en `users`, está inactiva, no tiene un rol permitido o usa un correo
fuera de los dominios institucionales devuelve 403.

La política efectiva se configura sin secretos:

```env
ALLOWED_ROLES=ADMIN_GT
ALLOWED_EMAIL_DOMAINS=unsa.edu.pe
```

Ambas listas aceptan valores separados por coma. También se valida `hd` del token
cuando Google lo incluye y `email`/`email_verified` cuando están presentes. El
perfil `dev` y `test` ya no habilitan operaciones anónimas fuera de salud,
Swagger y el documento OpenAPI. Para QA se recomienda el perfil `prod` con
`GOOGLE_OAUTH_ISSUER_URI` y `GOOGLE_OAUTH_AUDIENCE` explícitos; así se aplican las
mismas reglas que en producción.

`GET /api/v1/auth/me` requiere JWT y devuelve `id`, `email`, `fullName`,
`googleSubject` (puede ser null), `role` y `active`.

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
Para otra URL, pasar la base `/api/v1` como argumento. Configurar `API_TOKEN` con el JWT de un usuario registrado y activo, tambi?n en desarrollo.

Después de cambiar dependencias Gradle, reconstruir y recrear el proceso de la API
para actualizar el classpath de Spring Boot:

```sh
docker compose -f compose.dev.yml build --no-cache api
docker compose -f compose.dev.yml up -d --force-recreate api
```

### Dashboard

`GET /api/v1/dashboard?year=2026&schoolId=5` acepta ambos filtros opcionales:
`year` entre 1900 y 9999 (año de inicio) y `schoolId` positivo. Se aplican a todos
los conteos, la distribución y las listas; un valor inválido devuelve 400 con
`validationErrors`. La respuesta conserva los nombres `statusDistribution`,
`upcomingDefenses` y `recentExpedients` (no `distribution`/`upcoming`/`recent`) y
agrega:

- `asOfDate`: fecha de corte `YYYY-MM-DD` calculada con el `Clock` de negocio en
  `America/Lima`.
- `timeZone`: siempre `America/Lima`.

Las listas son arrays de `ExpedientApiModels.Summary` y se devuelven vacías cuando
no hay datos. `Summary` conserva todos los campos existentes e incluye `degree`,
`observations` y `studentCode` como campos nullable. `updatedAt` conserva el formato
local existente, sin offset, y su zona documentada es `America/Lima`. Los criterios
marcados *PO* en el documento de ajustes (definición de `active`, filtro de
sustentaciones pendientes y orden de recientes) se implementaron como propuesta
para QA; están pendientes de ratificación formal. Ver
[decisiones pendientes](docs/decisiones-pendientes.md).

### Resoluciones

El CRUD existente se conserva:

- `GET /api/v1/resolutions` devuelve un array completo de `ResolutionResponse`.
- `GET /api/v1/resolutions/{id}` devuelve 200 o 404.
- `POST /api/v1/resolutions` devuelve 201 con `Location`.
- `PUT /api/v1/resolutions/{id}` devuelve 200 o 404.
- `DELETE /api/v1/resolutions/{id}` devuelve 204, 404 o 409 si la resolución está
  referenciada por asesor, jurado o sorteo.

`expedientId` y `number` son obligatorios; `number` tiene máximo 100 caracteres y
no puede repetirse dentro del expediente sin distinguir mayúsculas. `resolutionDate`,
`resolutionType` (máximo 50, texto libre) y `fileUrl` admiten null. Una resolución
referenciada no se puede mover a otro expediente. Crear, actualizar o eliminar una
resolución actualiza en la misma transacción `updatedAt`/`updatedBy` del expediente
con el usuario del JWT; el cliente no puede enviar un responsable arbitrario.

### Expedientes completos y reemplazo académico

La escritura de expedientes exige JWT de un usuario registrado y activo, incluso en
desarrollo y pruebas. `createdById` y `updatedById` en el CRUD son campos obsoletos:
se ignoran y se obtienen del JWT. Las fechas se reciben como `yyyy-MM-dd` y «hoy»
se calcula en `America/Lima`.

- `POST /api/v1/expedients/complete`: crea graduando, expediente e investigación.
- `PUT /api/v1/expedients/{id}/complete`: actualiza los tres; si cambiar los datos
  de un graduando afectaría otros expedientes, devuelve 409. `graduate.fullName`
  se guarda íntegro como `firstNames` y `lastNames` vacío cuando no se envían
  nombres separados.
- `PATCH /api/v1/expedients/{id}/status`: recibe `{ "statusId": 1 }` y acepta
  cualquier estado existente y activo.
- `PUT /api/v1/expedients/{id}/academic`: **reemplazo total**, no parche. Se
  exigen `resolutions` y `draws` (listas que pueden estar vacías); `jury: null`,
  `advisor: null` y `defense: null` los eliminan. Si `jury` es una lista, debe
  tener al menos un miembro. Cada resolución puede declarar `clientId` (referencia
  temporal no persistida); asesor,
  jurado y sorteos pueden referirla mediante `resolutionClientId` (opcional).
  Cada sorteo contiene `teacherIds`. Los registros con `id` conservan su ID;
  los omitidos se eliminan. `fileUrl` de resoluciones conservadas no cambia.
  Una referencia interna inválida devuelve 400 y un ID de entidad inexistente
  devuelve 404.

Ejemplo de `/complete`:

```json
{
  "number": "EXP-2025-001", "startDate": "2025-01-01", "modalityId": 1, "statusId": 1,
  "graduate": {"documentNumber": "12345678", "fullName": "Nombre Completo", "schoolId": 1},
  "research": {"title": "Título de investigación", "workType": "TESIS"}
}
```

Ejemplo de `/academic`:

```json
{
  "resolutions": [{"clientId": "r1", "number": "R-001", "date": "2025-01-02", "type": "NOMBRAMIENTO"}],
  "advisor": {"teacherId": 1, "resolutionClientId": "r1"},
  "jury": [{"teacherId": 2, "role": "PRESIDENTE", "resolutionClientId": "r1"}],
  "draws": [{"date": "2025-01-03", "resolutionClientId": "r1", "teacherIds": [2]}],
  "defense": null
}
```

**Límites de este contrato provisional:** el pedido no especifica el patrón del
número ni el catálogo de tipos de resolución ni reglas adicionales de resultados;
no se valida todavía la compatibilidad entre el tipo de resolución y asesor,
jurado o sorteo. Se aplican las longitudes de la BD y las reglas de fechas
existentes. Sin un
índice único insensible a mayúsculas, la comprobación de duplicados por servicio
no garantiza unicidad insensible a mayúsculas bajo escrituras concurrentes.
El intercambio de docentes entre dos IDs de jurado conservados puede chocar con
la restricción UNIQUE inmediata; para soportarlo se requiere una reasignación
escalonada.

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

### Compatibilidad al integrar main

Se conservan `/complete`, `/{id}/complete` y `/{id}/academic`. El PATCH de estado
acepta el contrato por c?digo con `status` o el contrato por ID num?rico con
`statusId`; se debe enviar solo una de esas opciones. La creaci?n heredada con
`graduateId` est? disponible en `POST /api/v1/expedients/legacy`.
La API principal mantiene el payload anidado y las rutas por c?digo.

Flyway conserva `V3__expedient_query_indexes.sql` de main. Los campos de la API
unificada pasan a `V4__expedient_unified_fields.sql`. Las bases locales que ya
aplicaron la antigua V3 de esta rama requieren reconciliar su historial antes de
arrancar esta versi?n; no se deben borrar sus datos ni ejecutar repair sin revisar
el historial y el esquema.

## Pruebas de integración

Con PostgreSQL de desarrollo levantado (`docker compose -f compose.dev.yml up -d postgres`):

```sh
DB_URL=jdbc:postgresql://localhost:5431/grades_management \
DB_USER=root DB_PASSWORD=root \
./gradlew test --tests 'com.claudecoders.grades.auth.AuthorizationIntegrationTest' \
  --tests 'com.claudecoders.grades.dashboard.DashboardIntegrationTest' \
  --tests 'com.claudecoders.grades.dashboard.DashboardOrderingTest' \
  --tests 'com.claudecoders.grades.resolution.ResolutionIntegrationTest' \
  --tests 'com.claudecoders.grades.expedient.ExpedientIntegrationTest'
```

Las pruebas usan `MockMvc`, usuarios y JWT de prueba; no desactivan usuarios reales.
La prueba de expiración firma tokens con una clave RSA exclusiva de test y configura
un `JwtDecoder` que valida firma, `issuer`, `audience` y expiración. Las pruebas con
`.with(jwt())` verifican la política de registro, estado, rol y dominio; no acreditan
por sí solas la validación criptográfica, que cubre la prueba con token firmado.

## Configuración de QA

No se almacenan secretos en el repositorio. QA debe definir por entorno, como mínimo:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<host>:<puerto>/<base>
DB_USER=<usuario>
DB_PASSWORD=<password>
GOOGLE_OAUTH_ISSUER_URI=https://accounts.google.com
GOOGLE_OAUTH_AUDIENCE=<client-id-de-google>
GOOGLE_OAUTH_CLIENT_ID=<client-id-de-google>
ALLOWED_ROLES=ADMIN_GT
ALLOWED_EMAIL_DOMAINS=unsa.edu.pe
CORS_ALLOWED_ORIGINS=<origen-del-frontend>
```

El perfil `prod` activa la validación de `audience` y desactiva mensajes internos en
las respuestas de error. El documento OpenAPI `/api/docs/openapi.json` describe el
esquema `ApiError` y las respuestas 400/401/403/404/409/500 de las rutas de negocio.

## Observaciones de integración: búsqueda, docentes y PDF

- `GET /api/v1/expedients/search` acepta `search` ausente, vacío o con espacios.
  La búsqueda compara sin distinguir mayúsculas ni acentos e incluye número,
  nombres/apellidos (también nombre completo), documento y título. Flyway V5
  habilita `unaccent`; el usuario de migraciones debe poder instalar esa extensión.
- `POST /api/v1/expedients/complete` reutiliza el graduando por documento no vacío,
  sin sobrescribir su ficha compartida. Las altas completas concurrentes con el
  mismo documento se serializan en PostgreSQL. Ante duplicados históricos se
  reutiliza el ID menor; no se borran ni fusionan datos existentes. Un número de
  expediente repetido devuelve 409 y `El número de expediente ya está registrado`.
- `GET /api/v1/teachers/{id}/history` devuelve las designaciones vigentes, sin
  sorteos. Para asesores se toma la última fecha de asignación (fecha de registro
  si falta), desempatando por ID. Para jurados se toma el último ID por cargo.
  Los tres campos de resolución se incluyen como null cuando no hay resolución.
- `GET /api/v1/reports/teachers?teacherId=...&year=2026` acepta ambos filtros
  opcionales. `year` tiene cuatro dígitos (1000–9999): asesorías por `assignedAt`
  y jurados por fecha de resolución; si falta esa fecha se usa la de registro de
  la participación. No usa el año de inicio del expediente.
- `POST /api/v1/resolutions/{id}/document`: multipart con parte `file`, MIME
  `application/pdf`, cabecera PDF y hasta **10 MiB**. Devuelve 200 con
  `ResolutionResponse`; archivo ausente/inválido: 400, resolución inexistente:
  404, tamaño excesivo: 413. El límite multipart global sigue disponible para
  otras cargas; su exceso también devuelve 413.
- El PDF se almacena en PostgreSQL (migración V6), en la misma transacción que
  `fileUrl` y los metadatos del expediente. Reemplazarlo sustituye el contenido.
  `GET /api/v1/resolutions/{id}/document` descarga el PDF y **requiere Bearer JWT**;
  el cliente debe descargarlo con Authorization. No es un enlace público.
  En despliegues con proxy, configurar correctamente el esquema/host reenviado
  para que las URLs generadas correspondan a la dirección externa.
- Los PUT existentes mantienen su contrato: el CRUD guarda `fileUrl` recibido;
  el reemplazo académico preserva el documento de las resoluciones conservadas
  por ID. El frontend lo consume desde el 2026-11-10 (descarga autenticada, 10 MiB).
- `GET /api/v1/expedients/{id}/detail` devuelve `ETag`; `PUT /api/v1/expedients/{id}/academic`
  exige `If-Match` con ese ETag: 428 sin cabecera, 412 si el expediente cambió desde esa lectura.

La verificación automática está en `.github/workflows/backend.yml`: Java 25,
PostgreSQL 18, `gradlew check` y publicación del informe de pruebas. Las nuevas
pruebas están en `BackendObservationsIntegrationTest`.

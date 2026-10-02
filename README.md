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

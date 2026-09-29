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

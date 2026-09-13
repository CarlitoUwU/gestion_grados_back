# Gestión de grados - Backend

## Levantar con Docker

Desde la raíz del proyecto:

```powershell
docker compose -f compose.dev.yml up --build -d
```

Ver los logs de la API:

```powershell
docker compose -f compose.dev.yml logs -f api
```

Detener los servicios:

```powershell
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

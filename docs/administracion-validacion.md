# Cierre de Administración

Se conserva el contrato documentado en `ADMINISTRACION_PARA_BACKEND.md` (revisión
del frontend del 3 de noviembre de 2026). No cambian las rutas ni los DTO existentes.

## Cambios

- La confirmación bloquea el lote en PostgreSQL dentro de la transacción. Dos
  solicitudes del mismo lote producen un único agregado y respuestas 200/409.
- El manejador específico de `ResponseStatusException`, ya presente en esta rama,
  conserva el estado y motivo en `ApiError`. La nueva cobertura comprueba XLSX o
  columna inválidos: 400; confirmación repetida: 409; lote ajeno: 403; inexistente: 404.
- La confirmación importa solo `VALID`. Un catálogo cambiado o un duplicado
  sobrevenido produce 409 y rollback de todas las filas. `OBSERVED` y `REJECTED`
  mantienen su semántica; no se convierten a filas importables.
- El PATCH de acceso exige `active` explícito. Un body vacío devuelve 400 y no
  desactiva la cuenta. Repetir `false` conserva `false`.
- Las altas respetan `app.security.allowed-email-domains` (por defecto
  `unsa.edu.pe`), normalizan el correo a minúsculas y conservan la comprobación
  de duplicados y el rol `ADMIN_GT`.
- Swagger documenta el 403 de confirmación y la respuesta 400 de previsualización.

## Verificación

`AdministrationIntegrationTest` usa MockMvc, JWT de prueba y PostgreSQL en perfil
`dev`. Crea usuarios sintéticos y elimina únicamente sus fixtures. Las ejecuciones
se realizan sobre `grades_merge_qa`, separada de `grades_management`.

Seis pruebas cubren:

1. Preview 1 VALID / 1 OBSERVED / 1 REJECTED sin crear entidades; confirmación de
   exactamente un expediente, graduando y trabajo; lectura persistida y repetición 409.
2. Archivo inválido, columna ausente, lote ajeno, inexistente y solicitud sin JWT.
3. Dos confirmaciones simultáneas: 200/409 y un solo expediente.
4. Duplicado aparecido después del preview: rollback sin importación parcial.
5. Catálogo cambiado después del preview: 409 sin importación parcial.
6. Alta institucional 201, duplicado 409, PATCH vacío 400, persistencia e idempotencia
   de activación; la cuenta desactivada recibe 403 en `/auth/me` y `/dashboard`,
   y vuelve a recibir 200 al reactivarse.

Comando reproducible desde el backend, con la base QA ya creada:

```sh
docker compose -f compose.dev.yml run --rm -T --no-deps \
  -e DB_URL=jdbc:postgresql://postgres:5432/grades_merge_qa \
  -e DB_USER=root -e DB_PASSWORD=root \
  -e GRADLE_USER_HOME=/home/gradle/.gradle \
  api gradle spotlessCheck test --tests '*AdministrationIntegrationTest' bootJar --no-daemon
```

El frontend conserva `ServerImportPanel`, `ServerAccessPanel` y el cliente HTTP
existentes. Su suite `tests/administracion-backend.spec.ts` comprueba 14 recorridos:
plantilla, multipart, confirmación explícita, respuesta perdida, errores, historial,
altas y activación, autorización y vistas móvil/escritorio.

Autodesactivación (decisión del 2026-11-10): `PATCH /accesses/{id}/active` con
`active=false` sobre la cuenta de la sesión actual responde 409 y no cambia el
estado; el frontend ya deshabilitaba esa acción ("Tu cuenta"). Como cada cuenta
solo puede ser desactivada por otra, siempre queda al menos un administrador activo.

Este cierre abarca Administración. Reportes, Docentes, Expedientes y PDF conservan
su estado anterior y requieren entregas posteriores. No se ha realizado commit ni push.

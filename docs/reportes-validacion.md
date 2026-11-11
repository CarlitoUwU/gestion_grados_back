# Cierre de Reportes

Se conservan las cuatro rutas `GET /api/v1/reports/*` y el DTO `ReportResponse
{ title, columns, rows }`. Los cambios solo agregan columnas al final o corrigen
criterios; no se renombraron columnas existentes.

## Cambios

- `/reports/teachers` agrega `ID docente` y `Docente` al final de cada fila, para
  identificar al docente cuando se consultan todos. Un `teacherId` inexistente
  devuelve 404.
- `/reports/defended-works` incluye solo sustentaciones con fecha hasta hoy
  (`Clock` de `America/Lima`) y resultado no vacío distinto de `Pendiente`
  (sin distinguir mayúsculas ni tildes). Es el mismo criterio de `defended` del
  dashboard.
- `/reports/statistics` agrega `Sustentación` y `Resultado` para que el cliente
  calcule ingresados, sustentados y completados sobre las mismas filas.
- `year` (1900–9999), `schoolId` y `modalityId` (positivos) se validan con 400 en
  las rutas que los aceptan.

## Pendiente de decisión

- `/reports/teachers` no acepta `schoolId`: el frontend filtra por la escuela del
  expediente sobre el conjunto completo. Si el volumen lo exige, agregar el filtro
  en servidor.
- El año de graduados (ingreso frente a egreso) sigue pendiente de validación con PO;
  las estadísticas usan el año de inicio del expediente.

## Verificación

`BackendObservationsIntegrationTest.reportsIdentifyTeacherAndExcludeFutureOrPendingDefenses`
cubre la columna de docente, el 404, la exclusión de fechas futuras, de `pendiente`
y de resultados vacíos, las columnas nuevas de estadísticas y los 400 de parámetros.
Suite completa sobre `grades_merge_qa`: 36 pruebas, 0 fallos.

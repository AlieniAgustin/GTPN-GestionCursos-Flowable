# ADR: Qué vive como variable del proceso y qué va a la BD de la institución

- **Estado:** Aceptado
- **Fecha:** 2026-11-13
- **Contexto:** enunciado §3.2, `backend/src/main/java/com/backend/proxy/entity/`, `flowable/src/main/java/com/flowable/app/listeners/SyncCourseDetailsListener.java`
- **Relacionado:** [Arquitectura](./arquitecture.md)

## Contexto

El enunciado (§3.2) pide una base de datos propia de la institución con las empresas solicitantes y la información de los cursos —nombre, descripción, programa, presupuesto, fechas e informe de resultados—, y que cada curso quede vinculado con su instancia de proceso en Flowable. En la misma sección aclara que **la lista de participantes y los recursos asignados pueden mantenerse como variables del proceso**.

Hay entonces dos almacenes con responsabilidades distintas y riesgo de duplicar información:

- El **motor** guarda variables por instancia en `ACT_RU_VARIABLE` / `ACT_HI_VARINST`.
- La **BD de la institución** guarda `companies` y `courses` (mismo PostgreSQL, tablas propias).

Sin un criterio explícito, cada integrante decide distinto y terminamos con el mismo dato en los dos lados, desincronizado.

## Decisión

**El criterio es la vida útil del dato.**

> Si el dato le sirve a la institución después de que la instancia terminó, va a la BD.
> Si solo existe para que el proceso tome decisiones mientras corre, queda como variable.

### Va a la BD de la institución

Es lo que el enunciado enumera en §3.2 más el vínculo con el proceso:

| Dato | Columna | Lo produce |
|---|---|---|
| Empresa solicitante | `companies` | El alta de la solicitud (§4.1.1) |
| Credenciales de la empresa | `companies.access_username` / `access_password_hash` | *Habilitar acceso de la empresa* (§4.1.2) |
| Nombre y descripción | `courses.name` / `description` | El alta de la solicitud |
| Fechas posibles | `courses.possible_dates` | El alta de la solicitud |
| Programa | `courses.program` | *Registrar y elaborar propuesta* |
| Presupuesto | `courses.budget` | *Registrar y elaborar propuesta* |
| Fecha de inicio acordada | `courses.agreed_date` | La aceptación de la empresa (§3.5) |
| Informe de resultados | `courses.report` | *Elaborar informe de resultados* |
| Estado del curso | `courses.status` | El `UpdateCourseStatusListener` al cerrar (§3.4) |
| Vínculo con la instancia | `courses.process_instance_id` | El alta, con la respuesta de Flowable |

### Queda como variable del proceso

Todo lo demás. Son datos de control de la ejecución: se consultan desde el historial de Flowable si hace falta auditarlos, pero no tienen lugar en el modelo de la institución.

| Grupo | Variables |
|---|---|
| Viabilidad (§3.1) | `viable` |
| Negociación | `propuesta`, `plazoRespuesta`, `recordatorios`, `fechaCurso`, `fechaAcordada` |
| Preparación — recursos | `requiereAula`, `requierePlataforma`, `requiereEquipamiento`, `aulaAsignada`, `aulaLiberada`, `aulaVirtualLiberada`, `equipoContratado`, `equipoLiberado` |
| Preparación — inscripciones | `listaDefinitiva` |
| Desarrollo | `docenteSeleccionado`, `cursoDesarrollado` |
| Cierre | `informe`, `informeEnviado`, `expedienteArchivado`, `resultadoCurso` |
| Cancelación | `cancelado`, `cursoActivo` |
| Acceso de la empresa | `empresaAccesoHabilitado`, `empresaUsuario`, `empresaPasswordTemporal`, `empresaAccesoCreado`, `empresaId` |
| Eco del alta | `courseId`, `courseName`, `companyName`, `companyEmail` |

`listaDefinitiva` y los recursos quedan como variables **por decisión explícita del enunciado**, que lo permite. `informe` es el único dato que vive en los dos lados a propósito: como variable mientras el proceso lo necesita para la tarea de envío, y como columna porque la institución lo conserva.

### Cómo viaja el dato a la BD

El motor empuja, el BFF persiste. El motor nunca escribe SQL de la institución, y el BFF no sondea al motor.

```
userTask completada ──take──> SyncCourseDetailsListener
                                   │ PUT /api/flowable/courses/{processId}/details
                                   ▼
                              CourseService.updateCourseDetails
                                   │
                                   ▼
                              courses (BD institución)
```

El listener está enganchado en cuatro flujos del modelo, uno por etapa que produce datos:

| Flujo | Qué sincroniza |
|---|---|
| `flow_propuesta_a_enviarpropuesta` | programa, presupuesto |
| `flow_revisarpropuesta_a_enviarpropuesta` | programa y presupuesto de la nueva versión |
| `flow_aceptacion_a_habilitaracceso` | fecha acordada (corre después de `resolveCourseDate`) |
| `flow_informe_a_enviarinforme` | informe de resultados |

Tres propiedades sostienen el diseño:

1. **Idempotente y parcial.** El listener manda las variables que ya existen; `Course.applyDetails` solo pisa los campos que llegan con valor. Llamarlo de más no borra nada, y por eso el reenvío de una propuesta revisada no pierde la fecha ni el informe.
2. **Va por el motor, no por el BFF.** El enunciado permite completar varias tareas desde la interfaz de Flowable, sin pasar por el sitio web (§4.1.3, §4.2.1). Enganchando el listener en el modelo, la base queda sincronizada se complete por donde se complete.
3. **No bloquea la instancia.** Si el BFF no responde, el listener registra el error y el proceso sigue. La BD de la institución es un registro paralelo, no una condición para avanzar — el mismo criterio que ya usan `UpdateCourseStatusListener` y `EnableCompanyAccessDelegate`.

## Consecuencias

**A favor**

- Un solo lugar donde buscar cada dato, y un criterio para ubicar los que vengan.
- El historial de Flowable queda libre para el análisis de §4.3 y la minería de §4.4, sin cargar datos de negocio que no aportan al event log.
- La BD de la institución sobrevive al borrado del historial del motor.

**En contra**

- El dato llega a la BD **con el desfasaje** de la etapa: el programa recién aparece cuando la propuesta se envía, no mientras se escribe. Para consultas en vivo durante una tarea abierta hay que leer las variables de la instancia.
- `informe` duplicado en los dos lados. Lo asumimos: la alternativa era que la tarea de envío leyera la BD, acoplando el modelo al esquema de la institución.
- Si el BFF está caído en el momento exacto del `take`, ese dato no se vuelve a intentar. Es aceptable mientras el proceso siga avanzando; si en algún momento molesta, el arreglo es un reintento en el listener, no cambiar el criterio.

## Alternativas descartadas

- **Todo en variables, y la BD se arma al final.** Más simple, pero la empresa no podría consultar el estado de sus cursos durante la preparación (§4.1.2) sin pegarle al motor en cada request, y perderíamos el registro si se purga el historial.
- **Todo en la BD, el proceso lee de ahí.** Acopla el modelo BPMN al esquema de la institución y obliga a un service task por cada decisión. El motor dejaría de ser autónomo para evaluar sus propias gateways.
- **Que el BFF persista al completar la tarea**, en vez del listener. Se cae cuando la tarea se completa desde Flowable UI, que el enunciado permite explícitamente.

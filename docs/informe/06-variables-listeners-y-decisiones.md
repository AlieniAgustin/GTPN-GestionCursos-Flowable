# 06 — Variables, listeners y decisiones de implementación

> **Explica:** Damián · **Cubre:** §3.4 (los dos listeners), §3.5 (datos y temporizadores), §5 (el informe de variables, listeners y decisiones)
>
> Criterio de persistencia: [`adr/datos-proceso-vs-bd.md`](../../adr/datos-proceso-vs-bd.md)

Este es el documento que pide §5 de forma explícita. Los otros cuentan cada etapa; este junta los componentes que se incorporaron al motor.

---

## 1. Variables del proceso (§3.5)

El enunciado pide definir las variables necesarias para gestionar las propuestas, las respuestas de la empresa, los recordatorios, la fecha del curso, los recursos requeridos, el aula asignada, los participantes y el informe. Están todas, agrupadas por etapa:

| Etapa | Variables | Quién las escribe |
|---|---|---|
| Alta de la solicitud | `courseId`, `courseName`, `companyName`, `companyEmail` | El BFF al iniciar la instancia |
| Viabilidad (§3.1) | `cantidadCursosActivos`, `viable` | `CountActiveCoursesListener` y la tabla DMN |
| Propuesta | `propuesta`, `programa`, `presupuesto`, `fechasPropuestas`, `versionPropuesta` | *Registrar / Revisar propuesta* |
| Respuestas de la empresa | `observaciones` (modificaciones), `fechaCurso` / `fechaAcordada` (aceptación), `participantes` (nómina) | Los mensajes que envía la app de comunicaciones |
| Recordatorios | `plazoRespuesta`, `recordatorios` | `ApplyResponseDeadlineListener` y *Enviar recordatorio* |
| Acceso de la empresa | `empresaAccesoHabilitado`, `empresaUsuario`, `empresaAccesoCreado`, `empresaId`, `empresaPasswordTemporal` | `EnableCompanyAccessDelegate` |
| Curso activo | `cursoActivo` | Listener del flujo que entra a la preparación |
| Recursos requeridos | `requiereAula`, `requierePlataforma`, `requiereEquipamiento` | *Evaluar Recursos* |
| Recursos gestionados | `aulaAsignada`, `equipoContratado` | `ReserveClassroomDelegate`, task listener de *Contratar equipamiento* |
| Recursos liberados | `aulaLiberada`, `aulaVirtualLiberada`, `equipoLiberado` | Las actividades de compensación |
| Inscripciones | `listaDefinitiva` | *Confeccionar lista definitiva* |
| Dictado | `docenteSeleccionado`, `cursoDesarrollado` | *Definir docente*, *Desarrollar curso* |
| Cierre | `informe`, `informeEnviado`, `expedienteArchivado`, `resultadoCurso` | Las tareas de cierre y los listeners de flujo |
| Cancelación | `cancelado` | El subproceso de evento de cancelación |

Dos decisiones sobre las variables:

- **`listaDefinitiva` y los recursos quedan como variables del proceso**, no en la BD. Es lo que el enunciado permite explícitamente en §3.2.
- **El resultado del cierre (`resultadoCurso`) se setea en cada camino de cierre**, como exige §3.4, con listeners de flujo (`take`). Hay cinco: `NO_VIABLE`, `RECHAZADO`, `SIN_RESPUESTA`, `FINALIZADO` y `CANCELADO`.

La lista completa de qué variable vive dónde, y por qué, está en [`adr/datos-proceso-vs-bd.md`](../../adr/datos-proceso-vs-bd.md).

---

## 2. Los dos listeners que pide §3.4

### Task Listener: asignación al administrativo menos ocupado

**Qué pide el enunciado.** Un `TaskListener` en el evento de **creación** de *Confeccionar lista definitiva* que consulte cuántas tareas pendientes tiene asignadas cada integrante del grupo Administración y asigne la nueva al que tenga menos. En caso de empate, criterio libre. Se cuentan las tareas **asignadas**; **no** se incluyen las que solo tienen al grupo como candidato y todavía no fueron reclamadas.

**Cómo está hecho.** [`AssignLeastBusyListener`](../../flowable/src/main/java/com/flowable/app/listeners/AssignLeastBusyListener.java), enganchado en el modelo:

```xml
<userTask id="confeccionarlistadefinitiva" name="Confeccionar lista definitiva"
          flowable:candidateGroups="administracion">
  <extensionElements>
    <flowable:taskListener event="create" delegateExpression="${assignLeastBusy}"/>
  </extensionElements>
</userTask>
```

La lógica está en [`TaskAssignmentService`](../../flowable/src/main/java/com/flowable/app/services/TaskAssignmentService.java):

1. Lista los miembros del grupo `administracion` desde el IDM, ordenados por id.
2. Para cada uno cuenta `taskService.createTaskQuery().taskAssignee(usuario).count()` — **solo las asignadas**, en todas las instancias, que es la lectura literal del enunciado.
3. Gana el de menor cuenta; **el empate lo resuelve el orden alfabético de id**, porque la comparación es estricta y el primero que aparece se queda.
4. Asigna con `taskService.setAssignee(...)`, no con `claim`, para que la tarea figure como asignada y no como pendiente de reclamo.

Dos detalles de diseño: la tarea **conserva el grupo candidato** (así el grupo la ve si hace falta reasignarla) y si el modelo ya trajera un responsable, el listener lo respeta y no hace nada.

**Test:** `TaskAssignmentTest` deja a `administrativo1` con una tarea reclamada y verifica que la lista definitiva se asigne sola a `jefeadministrativo`, que tiene cero.

### Execution Listener: estado del curso al finalizar

**Qué pide el enunciado.** Un `ExecutionListener` que, al finalizar la instancia, actualice el estado del curso en la BD de la institución según el resultado: finalizado, cancelado, rechazado, cerrado por falta de respuesta o no viable.

**Cómo está hecho.** [`UpdateCourseStatusListener`](../../flowable/src/main/java/com/flowable/app/listeners/UpdateCourseStatusListener.java), declarado a **nivel de proceso** con `event="end"`, así que corre una sola vez cuando la instancia termina, por cualquiera de sus cinco caminos:

```xml
<process id="Process_Curso" isExecutable="true">
  <extensionElements>
    <flowable:executionListener event="end" delegateExpression="${updateCourseStatus}"/>
  </extensionElements>
```

Lee `resultadoCurso` y le avisa al BFF con `PUT /api/flowable/courses/{processInstanceId}` y el cuerpo `{"status": "..."}`. Si la variable no está, lo registra como advertencia y no inventa un estado. Si el BFF no responde, lo registra y **no** bloquea el cierre de la instancia.

**Tests:** cada escenario verifica su resultado, `HappyPathTest` verifica además que el listener efectivamente le mandó el estado al BFF, y `ClosingPathsTest` recorre el modelo desplegado comprobando que **todos** los caminos que llegan a un evento de fin pasan por un listener que setea la variable.

---

## 3. El resto de los componentes incorporados al motor

| Componente | Tipo | Dónde está enganchado | Qué hace |
|---|---|---|---|
| [`CountActiveCoursesListener`](../../flowable/src/main/java/com/flowable/app/listeners/CountActiveCoursesListener.java) | ExecutionListener (`start`) | Decision task *Evaluar Viabilidad* | Cuenta los cursos activos del motor para la regla DMN (§3.1) |
| [`ApplyResponseDeadlineListener`](../../flowable/src/main/java/com/flowable/app/listeners/ApplyResponseDeadlineListener.java) | ExecutionListener (`start`) | *Enviar propuesta* | Fija `plazoRespuesta` desde la configuración si la instancia no trae uno |
| [`ResolveCourseDateListener`](../../flowable/src/main/java/com/flowable/app/listeners/ResolveCourseDateListener.java) | ExecutionListener (`take`) | Flujo de salida de *Aceptación* | Normaliza y garantiza `fechaCurso` antes del temporizador (§3.5) |
| [`SyncCourseDetailsListener`](../../flowable/src/main/java/com/flowable/app/listeners/SyncCourseDetailsListener.java) | ExecutionListener (`take`) | Cuatro flujos: propuesta, propuesta revisada, aceptación e informe | Empuja los datos del curso a la BD de la institución (§3.2) |
| [`CloseCancellationListener`](../../flowable/src/main/java/com/flowable/app/listeners/CloseCancellationListener.java) | ExecutionListener (`start`) | Fin del subproceso *Preparar curso* | Da de baja la escucha de la cancelación para que el subproceso cierre |
| [`EnableCompanyAccessDelegate`](../../flowable/src/main/java/com/flowable/app/delegates/EnableCompanyAccessDelegate.java) | JavaDelegate | *Habilitar acceso de la empresa* | Crea o reutiliza la cuenta de la empresa (§4.1.2) |
| [`ReserveClassroomDelegate`](../../flowable/src/main/java/com/flowable/app/delegates/ReserveClassroomDelegate.java) | JavaDelegate | *Reservar aula* | Web service de aulas simulado; lanza `BpmnError` si no hay |
| [`ReleaseClassroomDelegate`](../../flowable/src/main/java/com/flowable/app/delegates/ReleaseClassroomDelegate.java) | JavaDelegate | *Liberar Aula* (compensación) | Devuelve el aula al web service simulado |

Además hay listeners **en línea**, como expresiones, para lo que no justifica una clase: los cinco `resultadoCurso`, `cursoActivo`, `cancelado`, `equipoContratado` y `equipoLiberado`.

---

## 4. Temporizadores y plazos (§3.5)

Ninguna duración ni fecha está fija en el modelo:

| Temporizador | Expresión en el BPMN | De dónde sale el valor |
|---|---|---|
| Espera de respuesta a la propuesta | `<timeDuration>${plazoRespuesta}</timeDuration>` | Variable de la instancia; por defecto `proceso.plazo-respuesta` (`P5D`) |
| Fecha de inicio del curso | `<timeDate>${fechaCurso}</timeDate>` | La acuerda la empresa en la aceptación; si falta, `proceso.fecha-curso-offset` (`P7D`) |

Para la demo se acortan sin tocar el modelo ni recompilar, con las dos variables de entorno que están comentadas en [`docker-compose.yml`](../../docker-compose.yml):

```yaml
PROCESO_PLAZO_RESPUESTA: PT1M
PROCESO_FECHA_CURSO_OFFSET: PT2M
```

Un plazo puesto en la instancia le gana a la configuración, así que también se puede iniciar una solicitud con su propio `plazoRespuesta`.

---

## 5. Decisiones de implementación, en una tabla

| Decisión | Por qué |
|---|---|
| El seed de usuarios corre **en todos los perfiles**, no solo en `dev` | El despliegue Docker usa `prod`; con el seed limitado a `dev` la base levantaba sin usuarios y había que arrancar la app local a mano antes de la demo |
| El motor **empuja** los datos a la BD por listener, en vez de que el BFF los escriba al completar la tarea | El enunciado permite completar tareas desde Flowable UI, sin pasar por el sitio. Enganchado en el modelo, la base queda sincronizada se complete por donde se complete |
| Las integraciones con el BFF **no bloquean** la instancia | El proceso es la fuente de verdad; la BD de la institución es un registro paralelo. Si el BFF está caído, el proceso avanza y queda el error en el log |
| La compensación se usa también en el **cierre normal**, no solo en la cancelación | El enunciado pide liberar los recursos en los dos casos; reutilizar la compensación evita duplicar las actividades de liberación |
| Las responsabilidades de §2 viven en el **modelo** (`assignee`, `candidateGroups`), no en código | Cambiar quién hace qué no debería requerir recompilar nada |
| La regla de viabilidad vive en una **tabla DMN** | Lo pide §3.1, y además se puede editar desde el Modeler sin tocar Java |
| Las dos apps de escritorio **no usan Spring** | Son clientes de escritorio; el starter de Flowable las habría convertido en una segunda aplicación del motor sobre la misma base |

---

## 6. Preguntas probables

**¿Por qué el listener de asignación cuenta tareas de todas las instancias y no solo de esta?**
Porque el enunciado habla de cuántas tareas tiene asignadas **cada integrante**, no cuántas en ese curso. La carga de trabajo de una persona no se reparte por instancia.

**¿Cómo se resuelve el empate?**
Por id de usuario, en orden alfabético. El enunciado deja el criterio libre; elegimos uno determinista para que el comportamiento sea reproducible en los tests y en la demo.

**¿Qué pasa si una instancia termina sin `resultadoCurso`?**
El listener lo registra como advertencia y no actualiza la base, para no escribir un estado inventado. Que no pase lo garantiza `ClosingPathsTest`, que analiza el modelo desplegado y falla si algún evento de fin es alcanzable sin pasar por un listener de resultado.

**¿Por qué hay un listener que "arregla" el cierre del subproceso?**
Porque Flowable no cierra un subproceso que tiene a la vez eventos de borde y un subproceso de evento: la ejecución que espera el mensaje de cancelación queda viva. Es una limitación del motor, no del modelo, y está documentada en [`adr/cancelacion-no-interruptiva.md`](../../adr/cancelacion-no-interruptiva.md).

# BPMN ejecutable y listeners — resumen de cambios

Este documento resume lo que se hizo para que el proceso **Organizar curso de capacitación** sea ejecutable en Flowable 7 (motor embebido en Spring Boot) y para implementar los listeners de la sección 3.4 de la consigna.

## 1. Archivos que se tocaron

| Archivo | Qué es |
|---|---|
| `flowable/src/main/resources/processes/Organizar_curso_de_capacitación.bpmn20.xml` | El BPMN que despliega la app al arrancar |
| `flowable/pom.xml` | Se agregó la dependencia `org.apache.groovy:groovy-jsr223` (motor de los script tasks) |
| `flowable/src/main/java/com/flowable/app/listeners/AssignLeastBusyListener.java` | Task listener de asignación |
| `flowable/src/main/java/com/flowable/app/listeners/UpdateCourseStatusListener.java` | Execution listener de fin de proceso |
| `flowable/src/main/java/com/flowable/app/listeners/CountActiveCoursesListener.java` | Execution listener que cuenta los cursos activos antes de evaluar la viabilidad |
| `flowable/src/main/java/com/flowable/app/listeners/CloseCancellationListener.java` | Execution listener que deja cerrar el subproceso Preparar curso (ver 3.5) |
| `flowable/src/main/resources/dmn/evaluarViabilidad.dmn` | Tabla de decisión de viabilidad (se despliega sola al arrancar) |

## 2. Cambios en el BPMN

### 2.1 Asignaciones de tareas de usuario

Los IDs de usuarios y grupos son los que crea `App.java`.

| Tarea | Asignación |
|---|---|
| Registrar y elaborar propuesta | Assignee `jefeadministrativo` |
| Revisar propuesta | Assignee `jefeadministrativo` |
| Archivar solicitud | Assignee `jefeadministrativo` |
| Enviar informe a empresa y archivar expediente | Assignee `jefeadministrativo` |
| Definir docente que desarrolla el curso | Assignee `jefedocente` |
| Elaborar informe de resultados | Assignee `jefedocente` |
| Desarrollar curso | Assignee `${docenteSeleccionado}` (lo elige el jefe docente) |
| Evaluar recursos, Preparar materiales, Solicitar aula virtual, Contratar equipamiento, Liberar equipamiento | Candidate group `administracion` |
| Confeccionar lista definitiva | Candidate group `administracion` + **task listener** que la asigna al administrativo menos cargado |

### 2.2 Mensajes

Flowable correlaciona los mensajes por **name** (no por id). Los que usa el proceso:

| Evento | Mensaje (name) |
|---|---|
| Inicio "Solicitud recibida" | `Solicitud de curso` |
| Aceptación | `Aceptación` |
| Rechazo | `Rechazo` |
| Modificaciones | `Solicitud de modificaciones` |
| Recibir nómina | `Nómina de participantes` |
| Recibir cancelación (subproceso de evento) | `Cancelación` |

### 2.3 Temporizadores

| Evento | Definición | Variable |
|---|---|---|
| "5 días" | `timeDuration` = `${plazoRespuesta}` | Formato ISO-8601. Si no viene al iniciar, *Enviar propuesta* la carga en `P5D`. Para pruebas se puede iniciar con `PT1M`. |
| "Fecha programada" | `timeDate` = `${fechaCurso}` | Fecha ISO (ej. `2026-11-20T09:00:00`). Tiene que estar definida antes de llegar al evento. |

### 2.4 Script tasks (Groovy)

Todas tienen *Script format* `groovy`. Por ahora son simulaciones (imprimen en consola).

| Tarea | Qué hace |
|---|---|
| Notificar Decisión | Imprime que la solicitud no es viable |
| Enviar propuesta | Si no existe `plazoRespuesta` la carga en `P5D`, pone `recordatorios = 0` |
| Enviar recordatorio | Incrementa `recordatorios` e imprime el recordatorio |
| Habilitar acceso de la empresa | Imprime |
| Enviar confirmación | Imprime |
| Notificar Cierre | Imprime |
| Liberar Aula Virtual (compensación) | Pone `aulaVirtualLiberada = true` |

### 2.5 Service tasks

| Tarea | Expresión |
|---|---|
| Reservar aula | `${execution.setVariable('aulaAsignada', 'AULA-1')}` |
| Liberar aula (compensación) | `${execution.setVariable('aulaLiberada', true)}` |

### 2.6 Decision task: Evaluar Viabilidad

Es una *decision task* (DMN) que usa la tabla `evaluarViabilidad` (*Decision table reference key*). La tabla está en `resources/dmn/evaluarViabilidad.dmn` y se armó en el editor de tablas de decisión de Flowable UI.

| Entrada | Salida |
|---|---|
| `cantidadCursosActivos` (number) | `viable` (boolean) |

Hit policy **FIRST**, con dos reglas:

| `cantidadCursosActivos` | `viable` |
|---|---|
| `< 5` | `true` |
| `>= 5` | `false` |

La entrada la carga el listener `${countActiveCourses}` (ver 3.4) en el evento `start` de la tarea, así que cuando corre la tabla la variable ya existe. La salida `viable` queda como variable del proceso y la usa la compuerta ¿Es viable?.

### 2.7 Condiciones de las compuertas

| Compuerta | Flujo | Condición |
|---|---|---|
| ¿Es viable? | sí / no | `${viable}` / `${!viable}` |
| ¿Se enviaron 2 recordatorios? | no (vuelve a esperar) / sí (archivar) | `${recordatorios <= 2}` / `${recordatorios > 2}` |
| Recursos necesarios (inclusiva) | Presencial / Virtual / Equipamiento especial | `${requiereAula}` / `${requierePlataforma}` / `${requiereEquipamiento}` |

### 2.8 Liberación de recursos al finalizar (1.4)

Entre *Enviar informe a empresa y Archivar expediente* y el fin *Curso finalizado* se agregó un **evento intermedio de lanzamiento de compensación** ("Liberar recursos"). Lanza las compensaciones de los recursos gestionados en *Preparar curso* (Liberar aula, Liberar Aula Virtual, Liberar Equipamiento) y el proceso termina recién cuando terminan. Probado: con aula presencial, al finalizar queda `aulaLiberada = true`.

## 3. Listeners (sección 3.4 de la consigna)

### 3.1 Task listener — asignar al menos cargado

- **Dónde:** tarea *Confeccionar lista definitiva*, evento `create`.
- **BPMN:** `delegateExpression="${assignLeastBusy}"`.
- **Clase:** `AssignLeastBusyListener` (bean de Spring `@Component("assignLeastBusy")`). Está basada en el ejemplo `AsignarTarea` de la cátedra.
- **Lógica:**
  1. Si la tarea ya tiene assignee, lo respeta.
  2. Busca los integrantes del grupo `administracion`, ordenados por ID.
  3. A cada uno le cuenta las tareas **asignadas** pendientes (`taskAssignee`). No cuenta las tareas que solo tienen al grupo como candidato.
  4. Asigna con `taskService.setAssignee(...)` al que tiene menos. **Empate:** gana el primero por ID.

### 3.2 Marcas de resultado (`resultadoCurso`)

La consigna pide que el resultado se establezca en cada camino de cierre. Se hace con execution listeners de tipo **expression** (no necesitan clase) sobre la flecha que entra a cada fin:

| Camino | Flecha | Valor |
|---|---|---|
| No viable | Notificar Decisión → Solicitud finalizada | `NO_VIABLE` |
| Rechazo | Rechazo → Archivar solicitud | `RECHAZADO` |
| Sin respuesta | "sí" (2 recordatorios) → Archivar solicitud | `SIN_RESPUESTA` |
| Curso dictado | Enviar informe → Liberar recursos | `FINALIZADO` |
| Cancelación | Notificar Cierre → Fin | `CANCELADO` |

Expresión: `${execution.setVariable('resultadoCurso','VALOR')}`, evento **`take`** (las flechas solo disparan `take`; con `start` el listener no se ejecuta).

### 3.3 Execution listener — fin de proceso

- **Dónde:** a nivel del proceso, evento `end`.
- **BPMN:** `delegateExpression="${updateCourseStatus}"`.
- **Clase:** `UpdateCourseStatusListener` (bean `@Component("updateCourseStatus")`).
- **Por ahora:** imprime la instancia, el business key del curso y `resultadoCurso`. Si una instancia termina sin `resultadoCurso`, imprime un aviso.
- **Falta:** guardar el estado en la base de la institución (está marcado con un comentario `Pendiente`).

### 3.4 Execution listener — contar cursos activos

- **Dónde:** en la tarea Evaluar Viabilidad, evento `start`.
- **BPMN:** `delegateExpression="${countActiveCourses}"`.
- **Clase:** `CountActiveCoursesListener` (bean `@Component("countActiveCourses")`).
- **Qué hace:** consulta al motor las instancias activas de `Process_Curso`, descarta la propia comparando por id y guarda el total en `cantidadCursosActivos`.
- Se filtra por id en vez de restar 1 porque la instancia actual puede no estar guardada todavía en la base cuando corre el listener.

### 3.5 Execution listener — cerrar la espera de cancelación

- **Dónde:** en el evento de fin interno de *Preparar curso* (`SubFin`), evento `start`.
- **BPMN:** `delegateExpression="${closeCancellation}"`.
- **Clase:** `CloseCancellationListener` (bean `@Component("closeCancellation")`).
- **Por qué:** Flowable (6.8 y 7.0) no cierra un subproceso que tiene a la vez un evento de borde (el error de cancelación) y un subproceso de evento (Recibir Cancelación). La espera del mensaje de cancelación queda viva y el proceso se traba al terminar la preparación.
- **Qué hace:** cuando la preparación termina, borra esa espera. El subproceso se cierra y el proceso sigue a *Enviar confirmación*.
- **Ojo:** este listener se agregó a mano en el XML. Hay que agregarlo también en Flowable UI antes de volver a exportar.

### 3.6 ¿Por qué delegate expression y no class?

Como el motor corre **embebido en Spring Boot**, los listeners son beans (`@Component`) y el BPMN los llama por nombre (`${assignLeastBusy}`). No hace falta escribir el paquete completo ni empaquetar un JAR para el contenedor de Flowable UI (lo que muestra el PDF de tips de la cátedra aplica al motor en Docker).

## 4. Variables del proceso

| Variable | Quién la define | Para qué |
|---|---|---|
| `cantidadCursosActivos` | `CountActiveCoursesListener` | Entrada de la tabla DMN |
| `viable` | Tabla DMN de Evaluar Viabilidad | Compuerta ¿Es viable? |
| `plazoRespuesta` | Al iniciar, o Enviar propuesta (`P5D`) | Timer de 5 días |
| `recordatorios` | Enviar propuesta / Enviar recordatorio | Compuerta de recordatorios |
| `requiereAula`, `requierePlataforma`, `requiereEquipamiento` | Evaluar recursos (al completar la tarea) | Compuerta inclusiva |
| `aulaAsignada`, `aulaLiberada`, `aulaVirtualLiberada` | Service/script tasks | Recursos |
| `docenteSeleccionado` | Definir docente (al completar la tarea) | Assignee de Desarrollar curso |
| `fechaCurso` | Al acordar la propuesta | Timer "Fecha programada" |
| `resultadoCurso` | Marcas de cierre | Listener de fin de proceso |
| `propuesta`, `informe`, `listaDefinitiva` | Data objects | Documentos del proceso |

## 5. Ojo al editar el BPMN en Flowable UI

El modeler de Flowable UI **pierde tres cosas al exportar**, que hay que volver a corregir a mano en el XML antes de copiarlo a `processes/`:

1. `isExecutable="false"` → `isExecutable="true"`.
2. Los data objects pierden el `name` → agregar `name="propuesta"`, `name="informe"`, `name="listaDefinitiva"`.
3. Las 3 conexiones de compensación salen como `<sequenceFlow>` → cambiarlas a `<association ... associationDirection="One">` (mismo id, sourceRef y targetRef).

Si se copia el export sin estos arreglos, el proceso no despliega o las compensaciones no funcionan.

## 6. Cómo probarlo

```bash
cd flowable
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Se usa el puerto 8081 porque el 8080 lo ocupa Flowable UI. En el log tienen que aparecer el despliegue del BPMN (`Organizar_curso_de_capacitación.bpmn20.xml`) y el de la tabla (`evaluarViabilidad.dmn`), y luego `Started App`.

Para verificarlo: `http://localhost:8081/process-api/repository/process-definitions` (usuario de App.java, por ejemplo `jefeadministrativo`). Tiene que aparecer `Process_Curso`.

## 7. Pendientes

- **Guardar el estado del curso (3.2):** completar `UpdateCourseStatusListener` cuando exista la base de la institución.
- **Cancelación:** el camino feliz ya funciona (con `closeCancellation`). Falta probar mandar el mensaje `Cancelación` durante la preparación: compensación de los recursos, error hacia el borde y Notificar Cierre.

# Escenario 9 — Registro de una solicitud desde el sitio e inicio de su instancia

> **Explica:** Cesari · **Cubre:** §4.1.1 del enunciado
> · Detalle técnico: [`09-sitio-web.md`](../09-sitio-web.md)

---

## Lo importante a mostrar

**Que un formulario público, sin cuenta, termina creando una instancia del proceso en
Flowable**, y que esa instancia queda **vinculada con el curso** de la base de la
institución en los dos sentidos: el curso guarda el id de la instancia, y la instancia
usa el id del curso como *business key*.

Y que, si la solicitud es viable, **la primera tarea aparece sola** en la bandeja del jefe
administrativo, sin que nadie la asigne a mano.

---

## Punto de partida

Stack levantado y `preparar_demo.py` corrido (ver [00](00-preparacion-de-la-demo.md)).
No hace falta ninguna instancia previa: este escenario **arranca de cero**.

> Ojo con el cupo: si ya hay **cinco cursos en ejecución**, la solicitud nueva sale *no
> viable* (es el escenario 5) y no aparece ninguna tarea. Con `preparar_demo.py` recién
> corrido hay cinco ocupados, así que conviene mostrar este escenario **después** de alguno
> que libere lugar (3, 4 o 6), o mostrarlo con la solicitud que deja preparada el script
> (*"Introduccion a BPMN"*, de Molinos del Sur, ya en la bandeja del jefe).

---

## Paso a paso

### 1. Mandar la solicitud, como empresa

En `http://localhost:8081`, **sin loguearse**:

1. Completar el formulario de la portada:
   - Empresa, email y teléfono de contacto.
   - Temática (por ejemplo *"Gestión de procesos con BPMN"*) y descripción.
   - Cantidad estimada de participantes.
   - Fechas preferidas, separadas por coma, en formato `2027-03-01, 2027-03-08`.
2. Apretar **Enviar**.

Para mostrar la validación antes de enviar bien: poner una fecha pasada o escrita mal
(`01/03/2027`). El sitio marca el error **al lado del campo** y **no se pierde lo cargado**.

### 2. Ver la confirmación

Aparece la pantalla **"Solicitud enviada"** con el número de solicitud.

### 3. Ver que la instancia nació

En Flowable (`http://localhost:8088/flowable-ui/`, `admin` / `test`) → Task App → procesos:
hay una **instancia nueva** del proceso. Ya pasó por *Evaluar viabilidad* y está esperando
en *Registrar y elaborar propuesta*.

### 4. Ver la tarea en la bandeja

En el sitio, entrar con **`jefeadministrativo` / `password`** → **Mis tareas**:

- En **Asignadas a mí** está **Registrar y elaborar propuesta**, con el nombre del curso
  recién pedido. Viene **ya asignada**: es una tarea exclusiva del jefe administrativo.

Ese es el empalme con el [escenario 2](02-modificaciones-y-nueva-propuesta.md).

---

## Dónde está implementado (para abrir si piden el código)

| Paso | Archivo | Qué hace |
|---|---|---|
| El formulario | `backend/src/main/resources/templates/publico/inicio.html` | Los campos y los errores por campo |
| Los datos del form | `backend/src/main/java/com/backend/proxy/web/form/SolicitudForm.java` → `parsedDates()` | Parsea las fechas escritas como texto |
| Recibir y validar | `backend/.../web/PublicSiteController.java` → `registrarSolicitud()` (`POST /solicitudes`) | Valida, rechaza fechas pasadas y llama al servicio |
| Guardar e iniciar | `backend/.../service/CourseService.java` → `processCourse()` | Guarda empresa y curso, pide al motor iniciar, guarda el id de la instancia |
| Llamar al motor | `backend/.../client/FlowableClient.java` → `startCourseProcess()` | `POST /api/course-requests` al motor, por HTTP |
| Iniciar en el motor | `flowable/src/main/java/com/flowable/app/CourseProcessController.java` → `services/CourseProcessService.java` → `startCourseRequest()` | `startProcessInstanceByMessage("Solicitud de curso", idDelCurso, variables)` |
| Lo que sigue solo | BPMN: start de mensaje → *Evaluar viabilidad* (`CountActiveCoursesListener` + `evaluarViabilidad.dmn`) → *Registrar y elaborar propuesta* (`assignee="jefeadministrativo"`) | |
| La bandeja | `backend/.../web/TaskSiteController.java` → `bandeja()` → `service/TaskService.java` → `findInbox()` | `GET /process-api/runtime/tasks?candidateOrAssigned=jefeadministrativo` |

## Cómo lo resolvimos

**El vínculo curso ↔ instancia en los dos sentidos.** `CourseService.processCourse()`
primero guarda el curso (para tener su id), después pide iniciar el proceso mandando ese id,
y con la respuesta guarda el `processInstanceId` en el curso:

```java
Course course = courseRepository.save(new Course(company, ...));       // 1. el curso tiene id
CourseProcessStartResponse response = flowableClient.startCourseProcess(
        new CourseProcessStartRequest(course.getId(), course.getName(),
                                      company.getName(), company.getEmail()));   // 2. iniciar
course.setProcessInstanceId(response.processInstanceId());             // 3. curso → instancia
courseRepository.save(course);
```

Del lado del motor, el id del curso es el **business key** (instancia → curso):

```java
return runtimeService.startProcessInstanceByMessage(
        "Solicitud de curso",                  // el proceso arranca por mensaje, no por id
        String.valueOf(request.courseId()),    // business key = id del curso
        variables                              // courseId, courseName, companyName, companyEmail
).getId();
```

**Por qué arranca por mensaje.** El sitio no necesita saber la clave ni la versión del
proceso: pide "llegó una solicitud de curso" y el motor sabe qué proceso inicia. Si se
despliega una versión nueva del BPMN, el sitio no cambia.

**Por qué el sitio no le pega al motor por cada dato.** El sitio solo manda id, nombre del
curso y datos de la empresa. Qué variables necesita el proceso y con qué nombre lo decide
el módulo del motor, no el sitio (*"decouple keys of backend"* en el historial de git).

**Validación sin perder lo cargado.** Las fechas llegan como texto libre y se parsean a
mano para poder decir *"No se entendió la fecha '01/03/2027'"* en vez de tirar un error de
formato. Si el motor no responde, la pantalla dice *"No pudimos registrar la solicitud…"* y
el formulario queda cargado.

---

## Si preguntan

**¿El alta usa la API REST de Flowable?**
Va por **HTTP** al motor, pero no al endpoint genérico `/process-api`: a uno propio del
módulo del motor, `/api/course-requests`, que inicia el proceso por mensaje con la API Java.
Lo separamos así para que el sitio no dependa de la clave del proceso ni de los nombres de
las variables. **Todo lo demás** (bandeja, reclamar, completar, variables, actividades) sí
va por la **API REST oficial** `/process-api`.

**¿Cómo se vincula el curso con su instancia?**
En los dos sentidos: `courses.process_instance_id` guarda el id de la instancia, y la
instancia tiene como business key el id del curso (§4.1.1, la "ayuda" del enunciado).

**¿Quién asigna la primera tarea?**
Nadie a mano: en el BPMN, *Registrar y elaborar propuesta* tiene
`flowable:assignee="jefeadministrativo"`. Por eso aparece en *Asignadas a mí* y no hay que
reclamarla.

**¿Y si la solicitud no es viable?**
Un listener cuenta los cursos en ejecución antes de la tarea de decisión, y la DMN corta si
hay cinco o más: la instancia termina en *Notificar decisión* y no se crea ninguna tarea
(escenario 5).

**¿Qué pasa si escribo mal una fecha?**
El formulario la marca en el campo y no se pierde nada de lo cargado; no se crea ni el
curso ni la instancia hasta que todo es válido.

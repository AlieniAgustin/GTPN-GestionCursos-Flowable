# 09 — Sitio web de la institución

> **Explica:** Cesari · **Cubre:** §4.1.1 (solicitud de capacitación), §4.1.3 (funciones para Administración), §4.1.4 (funciones para el equipo docente)
>
> El acceso de las empresas (§4.1.2) lo explica Fran en [08](08-datos-de-la-empresa-y-acceso.md). La infraestructura, en [07](07-backend-de-la-institucion.md).

---

## 1. Qué pide el enunciado

Un sitio con una **sección pública** para solicitar capacitaciones y una **sección con credenciales**, que interactúa con Flowable por su **API REST**. Para Administración: consultar tareas asignadas y disponibles para su grupo, reclamar y completar respetando las responsabilidades, y pantallas para *Registrar y elaborar propuesta* (con PDF), *Revisar propuesta* (con nueva versión del PDF), *Archivar solicitud*, *Confeccionar lista definitiva*, *Evaluar recursos* (al menos una alternativa) y *Enviar informe y archivar expediente*. Para el equipo docente: que el jefe defina el docente entre los tres y que el docente seleccionado complete *Desarrollar curso*.

## 2. Cómo se resolvió

Páginas **server-side con Thymeleaf dentro del módulo `backend`**: mismo Maven, mismo contenedor y misma sesión que la API. Las plantillas están en [`templates/`](../../backend/src/main/resources/templates/) y los estilos en [`static/css/sitio.css`](../../backend/src/main/resources/static/css/sitio.css).

### Mapa de pantallas

| Ruta | Quién entra | Qué hace |
|---|---|---|
| `/` | público | Portada con el formulario de solicitud |
| `/solicitudes` (POST) | público | Registra la solicitud e **inicia la instancia** |
| `/login` | todos | Login unificado: personal de la institución y empresas |
| `/mis-cursos` | empresa / institución | Cursos visibles según el rol |
| `/mis-cursos/{id}` | empresa / institución | Estado en vivo, actividades en curso y versiones del PDF |
| `/mis-cursos/{id}/propuestas/{v}.pdf` | empresa / institución | Descarga de una versión de la propuesta |
| `/panel` | institución | Punto de entrada del personal |
| `/tareas` | institución | **Bandeja**: asignadas y reclamables, en dos listas |
| `/tareas/{id}/reclamar` · `/completar` (POST) | institución | Reclamo y completado directo |
| `/tareas/{id}/elaborar-propuesta` | jefe administrativo | Formulario + genera el **PDF v1** |
| `/tareas/{id}/revisar-propuesta` | jefe administrativo | Muestra las observaciones + genera la **versión siguiente** |
| `/tareas/{id}/evaluar-recursos` | administración | Aula física / virtual / equipamiento, **al menos una** |
| `/tareas/{id}/definir-docente` | jefe docente | Elige uno de los tres docentes |
| `/tareas/{id}/elaborar-informe` | jefe docente | Carga el informe de resultados |
| `/tareas/{id}/enviar-informe` | jefe administrativo | Muestra el informe y pide confirmación explícita |

### La solicitud pública (§4.1.1)

El formulario pide datos de la empresa y del contacto, temática, descripción, cantidad estimada de participantes y fechas preferidas. Es un **form bean plano** ([`SolicitudForm`](../../backend/src/main/java/com/backend/proxy/web/form/SolicitudForm.java)) y no el DTO de la API, porque un formulario HTML no manda objetos anidados ni listas tipadas: las fechas llegan como texto separado por comas y se parsean a mano para poder decir *"No se entendió la fecha '01/03/2027'"* en vez de tirar un error de binding. Los errores se muestran sobre cada campo y **no se pierde lo cargado**.

Al enviarlo reutiliza `CourseService.processCourse`, el mismo camino que la API: el curso queda en la BD de la institución con su `process_instance_id` y el proceso arranca por el mensaje `Solicitud de curso`. **Es el escenario 9 de la defensa.**

### La bandeja de tareas (§4.1.3)

Dos listas separadas, porque el enunciado trata los dos casos distinto: **asignadas a mí** (listas para completar, sin reclamo) y **disponibles para mi grupo** (hay que reclamarlas primero). Cada tarea indica a qué curso pertenece, resuelto con una sola consulta de cursos por página.

Es **la misma pantalla** para Administración y para el equipo docente: cada uno ve lo suyo según sus grupos, porque la bandeja se la pide al motor con `candidateOrAssigned={usuario}`.

Las dos reglas de §2 que el motor no verifica solo las aplica el BFF: una tarea de grupo **no se completa sin reclamarla** (*"Primero tenés que reclamar la tarea 'Preparar materiales'"*) y una tarea con responsable **solo la completa esa persona**.

Las tareas que no necesitan capturar datos se completan con un botón directo desde la bandeja; están catalogadas en [`TaskCatalog`](../../backend/src/main/java/com/backend/proxy/types/TaskCatalog.java) junto con las variables que dejan. Las que necesitan datos tienen su pantalla.

### La propuesta y su PDF

*Registrar y elaborar propuesta* muestra la solicitud de la empresa y pide programa, presupuesto y fechas posibles, **precargadas con las que pidió la empresa**. Al enviar, [`ProposalService`](../../backend/src/main/java/com/backend/proxy/service/ProposalService.java) arma el PDF con OpenPDF, lo guarda como **versión 1** en `proposal_documents` y completa la tarea con `programa`, `presupuesto`, `fechasPropuestas` y `versionPropuesta`, todo en una transacción.

*Revisar propuesta* usa el mismo formulario, pero muestra arriba las **observaciones** que mandó la empresa al pedir modificaciones (la variable `observaciones`, leída del motor) y arranca **precargado con la última versión**, para cambiar solo lo pedido. Genera la versión siguiente del PDF y el proceso vuelve a *Enviar propuesta*, con los recordatorios en cero.

El detalle del curso lista todas las versiones, con la última marcada como vigente, y los PDFs se descargan con **la misma visibilidad que el curso**.

### Evaluar recursos

Casillas para aula física, aula virtual y equipamiento especial. Sin ninguna marcada vuelve al formulario con el error y **no completa nada**, que es el "al menos una alternativa" del enunciado. Manda **siempre las tres variables**, aunque sean `false`, porque si falta alguna la compuerta inclusiva del modelo falla con `Unknown property`.

### El equipo docente (§4.1.4)

*Definir docente* ofrece un desplegable con los **tres** integrantes del equipo, leídos del IDM del motor ([`FlowableTeacherRepository`](../../backend/src/main/java/com/backend/proxy/repository/FlowableTeacherRepository.java)), así que el jefe puede elegirse a sí mismo. Solo acepta a alguien de esa lista y completa con `docenteSeleccionado`, que el modelo usa como responsable de *Desarrollar curso*.

El docente seleccionado ve esa tarea en su bandeja **ya asignada**, sin reclamo, y la completa con el botón directo. Verificado: con `docente1` elegido, `docente2` no la ve.

### Enviar informe y archivar

Muestra el informe que dejó el equipo docente y pide una **confirmación explícita**, porque es el paso que cierra el curso y no tiene vuelta atrás. El informe se lee de la **variable del proceso**, no de la base: así la pantalla funciona aunque la sincronización no haya llegado. Al confirmar, la instancia cierra con `FINALIZADO`, la compensación libera los recursos y el curso queda `FINALIZADO` en la base. **Cierra el escenario 1 de punta a punta desde el sitio.**

## 3. Cómo se demuestra

Recorrido completo sin salir del sitio, con `preparar_demo.py` corrido antes:

1. `/` → cargar una solicitud → **escenario 9**: el curso queda en la base y la tarea *Registrar y elaborar propuesta* aparece en la bandeja del jefe administrativo.
2. `/login` como `jefeadministrativo` → `/tareas` → *Elaborar propuesta* → se descarga el **PDF v1**.
3. Pedir modificaciones desde la app de comunicaciones → *Revisar propuesta* muestra las observaciones → **PDF v2** (**escenario 2**).
4. Aceptar → `administrativo1` reclama y completa *Evaluar recursos* y *Preparar materiales*; el jefe administrativo **no** puede completar la que reclamó el otro (**escenario 8**).
5. `jefedocente` define a `docente2`; `docente2` completa *Desarrollar curso*; el jefe docente carga el informe; el jefe administrativo lo envía (**escenario 1**).

Tests del sitio: `PublicSiteControllerTest`, `CourseSiteControllerTest`, `TaskSiteControllerTest`, `TaskFormSiteControllerTest`, `ProposalSiteControllerTest`, dentro de los **103** del backend.

## 4. Preguntas probables

**¿El sitio usa la API REST de Flowable, como pide el enunciado?**
Sí, siempre a través del BFF, que es quien le habla al motor por REST con la cuenta técnica. El navegador nunca le pega al motor directo, pero toda operación sobre instancias y tareas termina en una llamada REST a `/process-api`.

**¿Por qué páginas server-side y no una SPA?**
Porque comparte sesión con la API, no necesita CORS ni un build de Node, y para la defensa es un servicio menos que levantar. El valor del trabajo está en el proceso, no en el front.

**¿Por qué hay tareas que se completan con un botón y otras con formulario?**
Porque algunas solo confirman que algo se hizo (*Solicitar aula virtual*, *Archivar solicitud*) y otras **aportan datos** al proceso (la propuesta, los recursos, el docente, el informe). Las primeras no necesitan pantalla; las segundas sí, y además validan antes de completar.

**¿Qué pasa si dos administrativos abren la misma tarea de grupo?**
El primero que reclama se la queda; al segundo el motor le rechaza el reclamo y la pantalla se lo dice. Es el comportamiento que pide §2 y está cubierto por `TaskAssignmentTest`.

**¿Dónde quedó el PDF del informe de resultados?**
No es obligatorio: el enunciado V3 lo tachó. El informe se carga como texto desde la pantalla del jefe docente y queda en la variable del proceso y en `courses.report`. El PDF que sí se genera es el de la **propuesta**, que §4.1.3 pide explícitamente, con una versión por cada envío.

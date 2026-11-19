# Escenario 2 — Solicitud de modificaciones y envío de una nueva propuesta

> **Explica:** Cesari · **Cubre:** §1.1 y §4.1.3 del enunciado
> · Detalle técnico: [`02-viabilidad-y-acuerdo.md`](../02-viabilidad-y-acuerdo.md) y [`09-sitio-web.md`](../09-sitio-web.md)

---

## Lo importante a mostrar

Tres cosas:

1. **La propuesta se arma con un formulario y genera un PDF**, que queda guardado como
   versión 1.
2. **Cuando la empresa pide modificaciones, el proceso vuelve atrás** a *Revisar
   propuesta*, y esa pantalla **muestra lo que pidió la empresa** y arranca **precargada
   con la versión anterior**.
3. **Revisar genera la versión 2 del PDF** y el proceso **vuelve a esperar la respuesta**,
   con el contador de recordatorios en cero. El ciclo se puede repetir las veces que haga
   falta.

---

## Punto de partida

`preparar_demo.py` deja dos instancias que sirven:

| Curso | Empresa | Dónde está parado | Para qué |
|---|---|---|---|
| **Introduccion a BPMN** | Molinos del Sur | *Registrar y elaborar propuesta* | Mostrar el escenario **completo**, desde el PDF v1 |
| **Excel Avanzado** | Frigorifico Pampa | *Revisar propuesta* (ya pidieron *"Reducir la carga horaria a 8 horas"*) | **Atajo** si hay poco tiempo: arrancar directo en el paso 3 |

Abiertos: el sitio (`http://localhost:8081`) y la **app de comunicaciones**.

---

## Paso a paso

### 1. Elaborar la propuesta (PDF v1)

En el sitio, entrar con **`jefeadministrativo` / `password`** → **Mis tareas**:

1. En *Asignadas a mí*, **Registrar y elaborar propuesta** de *Introduccion a BPMN* →
   **Completar**.
2. La pantalla muestra la solicitud de la empresa: curso, descripción y las fechas que
   pidió. Las fechas ya vienen **precargadas** en el formulario.
3. Cargar **programa** (por ejemplo *"4 módulos: notación, compuertas, eventos, práctica.
   16 horas"*) y **presupuesto** (*150000*). Para mostrar la validación, primero mandar
   sin programa o con presupuesto en cero: marca el error en el campo y no completa nada.
4. **Generar PDF y enviar propuesta**.

Vuelve a la bandeja con *"Propuesta enviada a la empresa (versión 1)"* y un enlace
**Ver el PDF**. Abrirlo: empresa, curso, programa, presupuesto y fechas, con versión y
fecha de emisión.

### 2. La empresa pide modificaciones

En la **app de comunicaciones**:

1. Elegir **Introduccion a BPMN**.
2. Abajo aparecen *Aceptar*, *Rechazar* y *Pedir modificaciones*: **solo esas tres**,
   porque son las que el motor tiene habilitadas mientras espera la respuesta.
3. **Pedir modificaciones** → escribir las observaciones (por ejemplo *"Bajar el
   presupuesto a 120000 y sumar un módulo de DMN"*) → **Enviar**.

### 3. Revisar la propuesta (PDF v2)

En el sitio, como **`jefeadministrativo`** → **Mis tareas**:

1. Aparece **Revisar propuesta** de *Introduccion a BPMN* → **Completar**.
2. Señalar:
   - Arriba, **las observaciones de la empresa**, tal cual las escribió.
   - El formulario viene **precargado con la versión 1**: programa, presupuesto y fechas.
   - Dice que se va a generar la **versión 2**.
3. Cambiar lo pedido (presupuesto a 120000, agregar el módulo) → **Generar nueva versión
   y enviar**.

Vuelve con *"Nueva versión de la propuesta enviada a la empresa (versión 2)"* y el
enlace al PDF.

### 4. Ver las dos versiones

**Mis cursos** → *Introduccion a BPMN* → sección **Propuestas**: están la **versión 2
(vigente)** y la **versión 1**, cada una con su PDF.

### 5. El proceso volvió a esperar

En la app de comunicaciones, al elegir el curso de nuevo aparecen otra vez *Aceptar*,
*Rechazar* y *Pedir modificaciones*. Se puede cerrar el escenario **aceptando** (con una
fecha) para mostrar que de acá se sigue a la preparación.

```
… → Elaborar propuesta (v1) → Enviar propuesta → Esperar respuesta → Modificaciones
  → Revisar propuesta (v2) → Enviar propuesta → Esperar respuesta → …
```

---

## Dónde está implementado (para abrir si piden el código)

| Qué | Archivo | Detalle |
|---|---|---|
| Las dos pantallas (mismo formulario) | `backend/src/main/resources/templates/tareas/elaborar-propuesta.html` | Si es revisión, muestra las observaciones y cambia el título y el botón |
| GET / POST de elaborar y revisar | `backend/.../web/ProposalSiteController.java` → `formularioElaborar`, `enviarElaborar`, `formularioRevisar`, `enviarRevisar` | Los cuatro delegan en `mostrar()` y `enviar()`, la lógica común |
| Precargar la versión anterior | `ProposalSiteController.mostrar()` → `ProposalService.ultimaVersion()` | Solo cuando la tarea es *Revisar propuesta* |
| Leer las observaciones | `ProposalSiteController.observacionesDe()` → `TaskService.findVariablesOfTask()` | `GET /process-api/runtime/process-instances/{id}/variables`, variable `observaciones` |
| Validar | `ProposalSiteController.enviar()` | Programa no vacío, presupuesto número > 0, fechas futuras; error por campo |
| Generar el PDF y completar | `backend/.../service/ProposalService.java` → `enviarPropuesta()` | Versión siguiente, PDF, guardado y completado en **una transacción** |
| Armar el PDF | `backend/.../service/ProposalPdfBuilder.java` | Con OpenPDF (`backend/pom.xml`) |
| Guardar las versiones | `entity/ProposalDocument.java` + `repository/ProposalDocumentRepository.java` | Tabla `proposal_documents`: una fila por versión |
| Descargar el PDF | `ProposalSiteController.descargar()` | `/mis-cursos/{id}/propuestas/{v}.pdf`, misma visibilidad que el curso |
| Listar versiones | `web/CourseSiteController.java` → `templates/cursos/detalle.html` | La primera marcada *Vigente* |
| La empresa pide modificaciones | `comunicaciones/.../service/CommunicationsService.java` → `send()` | `PUT /runtime/executions/{id}` con `messageEventReceived` y la variable `observaciones` |
| El ciclo en el modelo | BPMN: `esperarrespuesta` → `modificaciones` → `revisarpropuesta` → `enviarpropuesta` | `enviarpropuesta` pone `recordatorios = 0` |
| Programa y presupuesto a la BD | BPMN, flechas a *Enviar propuesta* con `${syncCourseDetails}` → `listeners/SyncCourseDetailsListener.java` | Copia los valores nuevos a `courses` |

## Cómo lo resolvimos

**Un solo formulario para las dos tareas.** *Elaborar* y *Revisar* piden lo mismo
(programa, presupuesto, fechas) y terminan igual (PDF nuevo y tarea completada). Lo que
cambia es de dónde arranca el formulario y si se muestran las observaciones. Por eso hay
una sola plantilla y un solo `enviar()`:

```java
private String mostrar(String taskId, String tipo, ...) {
    ...
    List<LocalDate> fechas = contexto.fechasSolicitadas();          // elaborar: lo que pidió la empresa
    if (tipo.equals(REVISAR_PROPUESTA) && anterior.isPresent()) {   // revisar: la última versión
        programa = anterior.get().programa();
        presupuesto = anterior.get().presupuesto().toPlainString();
        fechas = anterior.get().fechas();
    }
}
```

**La versión siguiente, siempre.** `ProposalService.enviarPropuesta()` cuenta cuántas
versiones tiene el curso y genera la próxima. Elaborar da la 1; cada revisión, la 2, la 3…:

```java
@Transactional
public int enviarPropuesta(...) {
    int version = proximaVersion(course.getId());                   // count + 1
    byte[] pdf = pdfBuilder.build(new ProposalPdfBuilder.Datos(...));
    proposalRepository.save(new ProposalDocument(course, version, ..., pdf));
    taskService.complete(taskId, Map.of(
            "propuesta", "Propuesta versión " + version,
            "programa", programa, "presupuesto", presupuesto,
            "fechasPropuestas", ..., "versionPropuesta", version), usuario);
    return version;
}
```

**Todo o nada.** `@Transactional`: si el motor rechaza el completado (por ejemplo porque la
tarea ya no está), la versión **no** queda guardada. Nunca hay un PDF de una propuesta que
el proceso no recibió.

**Dónde vive cada dato.** El **PDF y cada versión** quedan en la BD de la institución
(`proposal_documents`), porque sirven después de que el proceso termina. **Programa y
presupuesto vigentes** también van a `courses`, los copia el listener al pasar por *Enviar
propuesta*. **Las observaciones** quedan como variable del proceso.

**El reinicio de recordatorios.** No lo hace el sitio: lo hace el BPMN. *Enviar propuesta*
es un script que pone `recordatorios = 0` cada vez que sale una versión, que es lo que
pide §1.1 ("el contador de recordatorios se reinicia con cada nueva versión enviada").

---

## Si preguntan

**¿Cómo sabe el sitio qué pidió la empresa?**
La app de comunicaciones manda las observaciones como variable del mensaje
*Solicitud de modificaciones*. El sitio las lee del motor por la API REST
(`/process-api/runtime/process-instances/{id}/variables`).

**¿Dónde quedan las versiones del PDF?**
En la tabla `proposal_documents`, una fila por versión con el PDF adentro. Se descargan
desde el detalle del curso, con la misma visibilidad: una empresa solo baja las de sus
cursos.

**¿Qué pasa si el motor falla justo al completar?**
La transacción se deshace: no queda guardada esa versión y el usuario puede reintentar.

**¿Se puede pedir modificaciones más de una vez?**
Sí: *Revisar propuesta* vuelve a *Enviar propuesta* y de ahí a *Esperar respuesta*. El
modelo no limita las vueltas, y cada una genera una versión nueva.

**¿Por qué el formulario de revisión arranca con la versión anterior?**
Para cambiar solo lo que pidió la empresa. Si arrancara vacío, habría que volver a cargar
todo y es fácil equivocarse.

**¿La empresa ve el PDF?**
La empresa recién tiene cuenta después de aceptar (*Habilitar acceso*, §4.1.2). Durante la
negociación el envío está **simulado** —el enunciado lo permite— y los PDF quedan en el
sistema; una vez aceptada, los ve en *Mis cursos*.

# 02 — Viabilidad y acuerdo

> **Explica:** Alieni · **Cubre:** §1.1 (viabilidad y acuerdo), §3.1 (regla de negocio de viabilidad)
>
> Diseño detallado: [`docs/viablidad-acuerdo.md`](../viablidad-acuerdo.md)

---

## 1. Qué pide el enunciado

**§1.1.** Al recibir una solicitud se evalúa la viabilidad. Con cinco o más cursos en ejecución se comunica la imposibilidad y se finaliza. Con menos, el jefe administrativo registra la solicitud y elabora una propuesta con programa, presupuesto y fechas posibles; la envía y espera. La empresa puede aceptar, rechazar o pedir modificaciones, y el intercambio puede repetirse. Si pasan cinco días sin respuesta se envía un recordatorio, **como máximo dos**; cinco días después del segundo se archiva la solicitud y el proceso termina. El rechazo también lleva al archivo. **El contador de recordatorios se reinicia con cada nueva versión enviada.**

**§3.1.** La evaluación inicial se implementa con una **decision task** de Flowable. Su entrada es la cantidad de instancias que todavía no finalizaron, **excluyendo la que evalúa su propia viabilidad** y **considerando las distintas versiones desplegadas**. La cantidad se obtiene del motor, no se ingresa a mano.

## 2. Cómo se resolvió

### La decisión de viabilidad

Tres piezas:

1. **`CountActiveCoursesListener`** ([código](../../flowable/src/main/java/com/flowable/app/listeners/CountActiveCoursesListener.java)), un `ExecutionListener` de evento *start* sobre la decision task. Consulta el motor y deja el resultado en la variable `cantidadCursosActivos`. La consulta vive en [`CourseProcessService`](../../flowable/src/main/java/com/flowable/app/services/CourseProcessService.java):

   ```java
   runtimeService.createProcessInstanceQuery()
           .processDefinitionKey("Process_Curso")   // todas las versiones (§3.1)
           .variableValueEquals("cursoActivo", true)
           .count();
   ```

2. **La decision task** `evaluarviabilidad`, de tipo `dmn`, apuntando a la tabla `evaluarViabilidad`.

3. **La tabla DMN** ([`evaluarViabilidad.dmn`](../../flowable/src/main/resources/dmn/evaluarViabilidad.dmn)), con política de acierto `FIRST`:

   | Cursos activos | → Viable |
   |---|---|
   | `< 5` | `true` |
   | `>= 5` | `false` |

   La salida `viable` es la que evalúa la compuerta *¿Es viable?*.

**La instancia no se cuenta a sí misma** porque `cursoActivo` se setea más adelante, al tomar el flujo que entra a la preparación: cuando una instancia evalúa su viabilidad todavía no tiene esa variable, así que queda fuera del conteo por construcción, sin filtrar por id.

> **Decisión que conviene tener a mano.** Contamos como "curso en ejecución" **solo los que ya fueron aceptados** (`cursoActivo = true`), no las solicitudes que están negociando. El enunciado, leído al pie de la letra, dice "incluyendo las que están en acuerdo o preparación". Lo decidimos así porque una solicitud en negociación todavía puede rechazarse y no consume recursos de la institución, y está asentado como regla de negocio en [`docs/viablidad-acuerdo.md`](../viablidad-acuerdo.md). Si en la defensa se pide el criterio literal, el cambio es de una línea: quitar el `variableValueEquals` y contar todas las instancias activas del proceso.

### La negociación

- **Registrar y elaborar propuesta** (`registraryelaborarpropuesta`): userTask asignada a `jefeadministrativo`. Desde el sitio se completa con un formulario que genera el **PDF de la propuesta** (ver [09](09-sitio-web.md)) y deja `programa`, `presupuesto`, `fechasPropuestas` y `versionPropuesta`.
- **Enviar propuesta** (`enviarpropuesta`): scriptTask automática. Hace dos cosas: **reinicia `recordatorios` a 0** —el reinicio que pide §1.1 con cada nueva versión— y deja el plazo de espera listo.
- **Esperar respuesta** (`esperarrespuesta`): compuerta **basada en eventos** con cuatro salidas: los mensajes `Aceptación`, `Rechazo` y `Solicitud de modificaciones`, y el temporizador de cinco días. Gana el primero que llegue y los otros tres se cancelan solos; no hay que programar nada para descartarlos.
- **Modificaciones** → *Revisar propuesta* (jefe administrativo) → otra vez *Enviar propuesta*. El ciclo puede repetirse indefinidamente, y cada vuelta reinicia el contador de recordatorios.
- **Rechazo** → *Archivar solicitud* (jefe administrativo) → fin, con `resultadoCurso = RECHAZADO`.

### Los recordatorios

```text
Esperar respuesta ──5 días──► Enviar recordatorio ──► ¿Se enviaron 2 recordatorios?
                                                            ├─ no  ─► volver a Esperar respuesta
                                                            └─ sí  ─► Archivar solicitud (SIN_RESPUESTA)
```

El temporizador usa `${plazoRespuesta}`, una variable, no una duración fija: para la demo se baja a minutos con `proceso.plazo-respuesta` sin tocar el modelo (ver [06](06-variables-listeners-y-decisiones.md)). El script cuenta `recordatorios = n + 1` y solo imprime el aviso si `n <= 2`.

> **Observación de modelado que encontró la minería.** El nodo *Enviar recordatorio* se atraviesa **tres** veces: el límite de dos está dentro del script, no en la compuerta, así que el token da una vuelta de más sin enviar nada. El comportamiento es el correcto —se verificó que el motor imprime exactamente `Recordatorio 1` y `Recordatorio 2`—, pero el modelo sería más claro si la compuerta evaluara antes de la actividad. Está anotado como mejora opcional en [`TODO.md`](../../TODO.md) y en [`mineria/conclusion.md`](../../mineria/conclusion.md).

## 3. Cómo se demuestra

- **Escenario 5 (límite de cursos):** con cinco cursos aceptados, dar de alta una solicitud nueva desde el sitio. Termina sola con `resultadoCurso = NO_VIABLE` y el log imprime cuántos cursos activos contó. Test: `CourseLimitExceededTest`.
- **Escenario 2 (modificaciones):** pedir modificaciones desde la app de comunicaciones con observaciones, revisar la propuesta en el sitio y ver la **versión 2 del PDF**. Test: `ProposalModificationsTest`.
- **Escenario 3 (rechazo):** rechazar desde la app de comunicaciones y completar *Archivar solicitud* desde la bandeja. Test: `ProposalRejectionTest`.
- **Escenario 4 (sin respuesta):** levantar el motor con `PROCESO_PLAZO_RESPUESTA=PT1M`, esperar los dos recordatorios y completar *Archivar solicitud*. Test: `NoResponseTest`.

## 4. Preguntas probables

**¿Por qué una compuerta basada en eventos y no una exclusiva?**
Porque la institución no decide: espera. Los cuatro caminos son eventos externos (tres mensajes y un temporizador) y la compuerta basada en eventos es la construcción BPMN para "el primero que llegue", cancelando el resto sin código.

**¿Cómo se garantiza que no haya más de dos recordatorios?**
Por la compuerta `¿Se enviaron 2 recordatorios?`, que compara la variable `recordatorios` y deriva a *Archivar solicitud* cuando se pasa. El contador se reinicia en cada *Enviar propuesta*, así que una propuesta revisada vuelve a tener sus dos recordatorios, como pide §1.1.

**¿Por qué el conteo de cursos activos no está dentro del DMN?**
Porque §3.1 pide que la cantidad **se obtenga del motor**. El DMN solo decide; quien mide es el listener. Así la regla queda declarativa y editable desde el Modeler sin tocar Java.

**¿Qué pasa si la empresa acepta y rechaza casi al mismo tiempo?**
Gana el primer mensaje que el motor procesa; el segundo encuentra la ejecución ya cancelada y la API REST responde que no hay suscripción. La app de comunicaciones, además, solo ofrece las comunicaciones que el motor declara habilitadas en ese momento.

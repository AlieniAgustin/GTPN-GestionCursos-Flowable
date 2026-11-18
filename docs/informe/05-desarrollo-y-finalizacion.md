# 05 — Desarrollo y finalización

> **Explica:** Alieni · **Cubre:** §1.4 (desarrollo y finalización), §3.5 (temporizador de la fecha de inicio)
>
> Diseño detallado: [`docs/desarrollo-finalizacion.md`](../desarrollo-finalizacion.md)

---

## 1. Qué pide el enunciado

Cuando concluye la preparación se envía la confirmación a la empresa. Después:

1. El **jefe del equipo docente** realiza *Definir docente para desarrollar el curso* y selecciona **uno de los tres docentes, pudiendo elegirse a sí mismo**.
2. El docente seleccionado se **almacena en una variable** del proceso y queda **asignado** a la tarea *Desarrollar curso*.
3. Una vez alcanzada la **fecha programada**, ese docente registra la finalización del dictado.
4. El jefe docente elabora el informe de resultados.
5. El jefe administrativo envía el informe a la empresa y archiva el expediente.
6. Al finalizar normalmente se lanza la **compensación** prevista para liberar los recursos gestionados, y el proceso concluye **una vez completada su liberación**.

Y de §3.5: la fecha de inicio **se establece durante la ejecución**, al acordar la propuesta, y el temporizador debe usar esa variable, **sin fecha concreta en el diseño del modelo**.

## 2. Cómo se resolvió

```text
[Enviar confirmación]  (scriptTask)
        │
        ▼
[Definir Docente que desarrolla el curso]   userTask, assignee = jefedocente
        │                                    deja docenteSeleccionado
        ▼
⏱ [Fecha programada]                        timer, <timeDate>${fechaCurso}</timeDate>
        │
        ▼
[Desarrollar curso]                          userTask, assignee = ${docenteSeleccionado}
        │
        ▼
[Elaborar informe de resultados]             userTask, assignee = jefedocente → variable informe
        │
        ▼
[Enviar informe a empresa y archivar]        userTask, assignee = jefeadministrativo
        │  (listener de flujo: resultadoCurso = FINALIZADO)
        ▼
⟲ [Liberar recurso]                          compensación de todo el subproceso prepararcurso
        │
        ▼
[FIN: Curso finalizado]                      ExecutionListener de fin → estado en la BD
```

### La asignación dinámica del docente

*Definir docente* la hace el jefe docente y deja la variable `docenteSeleccionado` con el id del usuario elegido. La tarea siguiente declara `flowable:assignee="${docenteSeleccionado}"`, así que el motor resuelve el responsable **en ejecución**. Eso cubre dos puntos del enunciado de una sola vez: el docente queda guardado en una variable y queda asignado a la tarea.

La pantalla del sitio ofrece exactamente los **tres** integrantes del equipo docente —`jefedocente`, `docente1`, `docente2`, leídos del IDM del motor— y solo acepta a alguien de esa lista. Que el jefe pueda elegirse a sí mismo sale gratis: está en la lista.

Como la tarea viene asignada, el docente **no la reclama**: la ve en su bandeja lista para completar, y nadie más la ve. Es parte del escenario 8.

### El temporizador de la fecha de inicio

El evento es `<timeDate>${fechaCurso}</timeDate>`: **no hay ninguna fecha en el modelo**, como pide §3.5. El valor se acuerda con la empresa y llega en el mensaje `Aceptación` (§4.2.2 pide que la aceptación incluya la fecha acordada).

Para que la variable esté **siempre** definida antes de llegar al evento, el flujo que sale de *Aceptación* tiene un listener, [`ResolveCourseDateListener`](../../flowable/src/main/java/com/flowable/app/listeners/ResolveCourseDateListener.java), que:

- toma la fecha que trajo el mensaje (`fechaCurso` o `fechaAcordada`),
- la normaliza a ISO-8601 con offset, aceptando `2027-03-01`, `2027-03-01T09:00`, con offset o un `Date`,
- y si no llegó nada usable la deriva de `proceso.fecha-curso-offset` (7 días por defecto) avisando en el log.

Sin eso, completar *Definir docente* sin la variable reventaba con `Unknown property used in expression: ${fechaCurso}` y dejaba la instancia trabada. Detalle en [06](06-variables-listeners-y-decisiones.md).

### El cierre y la compensación final

*Enviar informe a empresa y archivar expediente* es la última tarea humana, del jefe administrativo. En el sitio tiene pantalla propia: muestra el informe que dejó el equipo docente —leído de la **variable del proceso**, que es la fuente de verdad mientras la instancia vive— y pide una confirmación explícita, porque cierra el curso y no tiene vuelta atrás.

El flujo de salida de esa tarea setea `resultadoCurso = FINALIZADO` y pasa por el evento de **lanzamiento de compensación** `liberarrecurso`, con `activityRef="prepararcurso"`: compensa todo el subproceso, liberando los recursos que se hayan gestionado, con las **mismas** actividades de compensación que usa la cancelación. El fin del proceso está **después** de la compensación, así que el proceso concluye una vez liberados los recursos, como pide el punto 6.

Al llegar al fin, el `ExecutionListener` global registra el estado del curso en la BD de la institución (ver [06](06-variables-listeners-y-decisiones.md) y [08](08-datos-de-la-empresa-y-acceso.md)).

## 3. Cómo se demuestra

Es el **escenario 1**, y desde que está el sitio se puede hacer de punta a punta sin salir de él:

1. Aceptar la propuesta desde la app de comunicaciones **con una fecha cercana**.
2. Completar la preparación (ver [03](03-preparacion-del-curso.md)).
3. `/tareas` → *Definir docente* → elegir `docente2` y completar. La tarea *Desarrollar curso* le queda asignada a `docente2` y a nadie más.
4. Esperar la fecha programada. Para la demo se baja con `PROCESO_FECHA_CURSO_OFFSET=PT2M`, o se acuerda una fecha ya pasada y el timer dispara enseguida.
5. `docente2` completa *Desarrollar curso*; el jefe docente completa *Elaborar informe*.
6. El jefe administrativo abre *Enviar informe*, lee el informe, confirma, y la instancia cierra: `resultadoCurso = FINALIZADO`, `aulaLiberada = true` por la compensación, y el curso queda `FINALIZADO` en la base.

Test: `HappyPathTest`, que verifica la asignación dinámica, la espera en el timer, el cierre y la liberación del aula. `CourseDateTest` recorre el camino completo **sin** inyectar la fecha al iniciar, que es el caso real.

## 4. Preguntas probables

**¿Dónde está la fecha de inicio del curso?**
En la variable `fechaCurso` de la instancia, y en la columna `courses.agreed_date` de la BD de la institución. En el modelo no hay ninguna fecha: el temporizador es una expresión.

**¿Qué pasa si la empresa acepta sin mandar fecha?**
El listener deriva una del offset configurado y lo deja asentado en el log. Es una decisión de robustez: el enunciado exige que el valor esté definido antes de alcanzar el evento, y preferimos una fecha derivada y avisada antes que una instancia trabada.

**¿Por qué el informe se lee de la variable y no de la base, si también está persistido?**
Porque mientras la instancia vive, la variable es la fuente de verdad y la base es un registro paralelo que se sincroniza por listener. Si el BFF estuvo caído en ese momento, la pantalla igual funciona. El criterio completo está en [`adr/datos-proceso-vs-bd.md`](../../adr/datos-proceso-vs-bd.md).

**¿Por qué se compensa al finalizar bien, si no hubo ningún error?**
Porque el enunciado lo pide así: la liberación de los recursos al terminar el curso es la misma operación que al cancelarlo. Usar compensación en los dos casos evita duplicar las actividades de liberación y garantiza que se libere solo lo que se había gestionado.

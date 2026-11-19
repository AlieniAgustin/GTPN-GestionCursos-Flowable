# Escenario 1 — Aceptación y finalización normal del curso, con liberación de recursos

> **Explica:** Alieni · **Cubre:** §1.2, §1.4 y §3.5 del enunciado
> · Detalle técnico: [`05-desarrollo-y-finalizacion.md`](../05-desarrollo-y-finalizacion.md)
>
> **Dejarlo para el final:** es el más largo, porque recorre la preparación, el dictado y
> el cierre.

---

## Lo importante a mostrar

Son tres cosas, y conviene nombrarlas al empezar:

1. **La asignación dinámica del docente.** El jefe elige a alguien del equipo y la tarea
   *Desarrollar curso* le queda asignada a esa persona y a nadie más. El modelo no sabe de
   antemano quién va a ser.
2. **El temporizador sin fecha fija.** En el diagrama no hay ninguna fecha escrita: el
   evento espera una variable que se acuerda con la empresa durante la ejecución.
3. **El proceso no termina hasta liberar los recursos.** El fin está *después* de la
   compensación, así que el curso no se da por cerrado hasta que se liberó lo que se había
   reservado.

---

## Punto de partida

El curso **"Atención al Cliente"** está en plena **preparación**, con las tres ramas
abiertas al mismo tiempo: inscripciones, materiales y recursos.

Antes de tocar nada, conviene abrir esa instancia en la **Task App** de Flowable y
mostrarlo: hay **tres tokens a la vez** dentro del subproceso *Preparar curso*. Eso es el
paralelismo del enunciado, y se ve de un vistazo.

---

## Parte A — Cerrar la preparación

Entrar a `http://localhost:8081` con **`administrativo1` / `password`** e ir a **Mis
tareas**.

Ahí están las tareas del curso *Atención al Cliente*, en *Disponibles para mi grupo*.

1. **Evaluar Recursos** → **Reclamar** → **Completar**.
   Se abre la pantalla de recursos: **tildar "Aula virtual"** y apretar **Confirmar
   recursos**.

   > Hay que elegir al menos una opción: si no se marca ninguna, la pantalla no deja
   > seguir. El enunciado pide justamente eso.

2. **Preparar materiales** → **Reclamar** → **Completar**.

3. Ahora apareció **Solicitar Aula Virtual** (porque marcamos aula virtual en el paso 1).
   **Reclamar** → **Completar**.

4. Falta la nómina, que la manda la empresa. En la **app de comunicaciones**:
   - Elegir el curso **"Atención al Cliente"**.
   - Elegir **Enviar la nómina de participantes**.
   - Escribir algunos nombres (uno por línea o separados por comas) y **Enviar**.

5. Volver a **Mis tareas**: apareció **Confeccionar lista definitiva**.
   **Reclamar** → **Completar**.

Con las tres ramas cerradas, el subproceso *Preparar curso* termina. En Flowable se ve que
el token salió del recuadro y pasó a *Enviar confirmación*.

---

## Parte B — Elegir el docente (asignación dinámica)

Salir de la sesión y entrar con **`jefedocente` / `password`**.

1. **Mis tareas** → está **Definir Docente que desarrolla el curso**, ya asignada (no hay
   que reclamarla: es tarea exclusiva del jefe docente). Apretar **Completar**.

2. Se abre un desplegable con **los tres integrantes del equipo**:
   - Marcela Ibáñez (jefedocente)
   - Hernán Rossi (docente1)
   - Sofía Quiroga (docente2)

   > **El jefe está en la lista:** puede elegirse a sí mismo, como pide el enunciado.

3. Elegir **Hernán Rossi (docente1)** y apretar **Asignar docente**.

**Para demostrar que la asignación funcionó**, vale la pena hacer esto:

- Salir y entrar con **`docente2` / `password`** → **Mis tareas**: no ve nada de ese curso.
- Salir y entrar con **`docente1` / `password`** → **Mis tareas**: ahí está *Desarrollar
  curso*, lista para completar.

---

## Parte C — El temporizador

Si la fecha acordada fuera lejana, la instancia quedaría esperando en **Fecha programada**
y no se podría seguir.

**Para esta demo no hay que esperar nada.** La fecha de inicio la acuerda la empresa al
aceptar la propuesta, y para *Atención al Cliente* el script la dejó **en el día de hoy**.
Así que cuando el token llega a *Fecha programada* el plazo ya está cumplido y el
temporizador dispara **en unos 4 segundos**.

Igual el momento se puede mostrar, y conviene hacerlo:

- **Antes** de completar *Definir docente*, señalar el evento en el diagrama: no tiene
  ninguna fecha escrita, espera una variable.
- **Al completar**, el token queda unos segundos en *Fecha programada*. Si se refresca la
  instancia en ese momento se la ve **parada ahí, sin ninguna tarea abierta**: el proceso
  está esperando.
- Refrescar de nuevo y ya apareció *Desarrollar curso*.

Lo que hay que decir es que dispara ahora porque la fecha acordada es hoy; si se hubiera
acordado marzo, la instancia se quedaría esperando en ese mismo punto hasta marzo.

> **Si querés verlo esperar un rato de verdad**, en
> [`scripts/preparar_demo.py`](../../../scripts/preparar_demo.py) está la constante
> `MINUTOS_HASTA_INICIO`, arriba del archivo. Puesta en `2` o `3`, la fecha acordada pasa a
> ser una hora puntual de hoy, y la instancia se queda parada en el temporizador hasta
> cumplirla. Hay que correr la preparación justo antes de presentar, porque esos minutos se
> cuentan desde que corre el script. **En `0`, que es como está, dispara solo.**
>
> La variable `PROCESO_FECHA_CURSO_OFFSET` del `docker-compose.yml` **no sirve para esto**:
> solo se usa cuando la empresa acepta sin mandar fecha, y acá siempre manda una.

---

## Parte D — El dictado y el informe

1. Con **`docente1` / `password`** → **Mis tareas** → **Desarrollar curso** → **Completar**.

2. Salir y entrar con **`jefedocente` / `password`** → **Mis tareas** → **Elaborar informe
   de resultados** → **Completar**.

   Se abre un cuadro de texto. Escribir algo como:

   > *Asistieron 18 de 20 inscriptos. Todos aprobaron la evaluación final.*

   Apretar **Completar tarea**.

---

## Parte E — El cierre con compensación

Salir y entrar con **`jefeadministrativo` / `password`**.

1. **Mis tareas** → **Enviar informe a empresa y Archivar expediente** → **Completar**.

2. La pantalla **muestra el informe que escribió el equipo docente**, para que el jefe
   administrativo lo revise antes de mandarlo. Eso es un requisito del enunciado.

3. **Tildar la casilla de confirmación** y apretar **Enviar y archivar**.

   > Sin tildarla no deja seguir, a propósito: esta acción cierra el curso y no tiene
   > vuelta atrás.

4. **El final, en Flowable.** Abrir la instancia por última vez. El recorrido termina así:

```
Enviar informe a empresa y Archivar expediente
        ↓
   ⟲ Liberar recurso          ← compensación de todo el subproceso de preparación
        ↓
   FIN: Curso finalizado
```

**Acá está el tercer punto de la demo:** el evento de fin está **después** de la
compensación. El proceso no se da por terminado hasta que se liberaron los recursos. Como
este curso había pedido aula virtual, lo que corrió fue *Liberar Aula Virtual*.

---

## Lo que hay por detrás (para señalar en el diagrama)

**La asignación dinámica.** La tarea *Desarrollar curso* no tiene un responsable fijo: lo
resuelve en ejecución a partir de la variable que dejó el jefe docente.

```xml
<userTask id="desarrollarcurso" name="Desarrollar curso"
          flowable:assignee="${docenteSeleccionado}">
```

Una sola línea cubre dos puntos del enunciado: el docente queda guardado en una variable
*y* queda asignado a la tarea.

**De dónde sale la lista de docentes.** No está escrita en el código: se lee del grupo
`docentes` del propio motor, con `FlowableTeacherRepository.java`. Si mañana se agrega un
docente en Flowable, el desplegable lo muestra sin tocar nada.

```java
private static final String GRUPO_DOCENTES = "docentes";
private static final String USUARIOS_URI =
        "/process-api/identity/users?memberOfGroup=" + GRUPO_DOCENTES + "&size=100";
```

**El temporizador.** El evento *Fecha programada* usa una expresión, sin fecha en el
modelo:

```xml
<timeDate>${fechaCurso}</timeDate>
```

**Quién garantiza que esa variable exista.** En el flujo que sale de *Aceptación* hacia
*Habilitar acceso de la empresa* —`flow_aceptacion_a_habilitaracceso`— hay un listener,
`resolveCourseDate`, implementado en `ResolveCourseDateListener.java`. Toma la fecha que
vino en el mensaje, la normaliza, y si no llegó ninguna deriva una por defecto:

```java
Object agreed = execution.getVariable(VAR_COURSE_DATE);   // "fechaCurso"
if (isEmpty(agreed)) {
    agreed = execution.getVariable(VAR_AGREED_DATE);      // "fechaAcordada"
}
...
execution.setVariable(VAR_COURSE_DATE, courseDate);
```

Sin ese listener, completar *Definir docente* sin la fecha rompía la instancia con
`Unknown property used in expression: ${fechaCurso}`.

**El resultado del cierre.** En el flujo que va de *Enviar informe* a la compensación
—`flow_enviarinforme_a_liberarrecurso`— está el listener que marca cómo terminó el curso:

```xml
${execution.setVariable('resultadoCurso','FINALIZADO')}
```

Ese valor es el que después registra el estado del curso en la base de datos de la
institución.

---

## Si preguntan

**¿Por qué se compensa al finalizar bien, si no hubo ningún error?**
Porque el enunciado lo pide así: liberar los recursos al terminar el curso es la misma
operación que al cancelarlo. Usar compensación en los dos casos evita duplicar las
actividades de liberación y garantiza que se libere únicamente lo que se había gestionado.

**¿Dónde está la fecha de inicio del curso?**
En la variable `fechaCurso` de la instancia y en la columna `agreed_date` del curso en la
base de la institución. En el modelo no hay ninguna fecha.

**¿Qué pasa si la empresa acepta sin mandar fecha?**
El listener deriva una a partir de un plazo configurado y lo deja asentado en el log.
Preferimos una fecha derivada y avisada antes que una instancia trabada.

# Escenario 8 — Asignación, reclamo y finalización de tareas según los responsables

> **Explica:** Damián · **Cubre:** §2 del enunciado (personal y responsabilidades) y §3.4
> (el task listener de asignación)
> · Detalle técnico: [`01-modelo-y-motor.md`](../01-modelo-y-motor.md) y
> [`06-variables-listeners-y-decisiones.md`](../06-variables-listeners-y-decisiones.md)
>
> **Va después del escenario 7**, sobre la misma instancia: *Primeros Auxilios*
> (Cooperativa La Union), que quedó con el aula resuelta y la preparación abierta. No
> necesita ningún setup propio.

---

## Lo importante a mostrar

**Que el "quién hace qué" no está programado: está en el modelo.** Cada tarea lleva en el
BPMN o un responsable fijo o un grupo candidato, y el motor lo hace cumplir. Lo único que
se decide en ejecución son dos cosas: a qué administrativo le toca la lista definitiva —lo
resuelve un listener— y qué docente dicta el curso —lo decide el jefe docente—.

Son tres palabras y hay que mostrar las tres: **asignación**, **reclamo** y
**finalización**.

---

## Los puntos a cubrir

| # | Punto | De dónde sale |
|---|---|---|
| 1 | Hay tareas que llegan **asignadas por rol** al jefe administrativo | plan propio |
| 2 | Hay tareas **abiertas al grupo** que cualquiera de los dos administrativos reclama | plan propio |
| 3 | Una tarea de grupo **no se puede completar sin reclamarla** | lo pide §2 |
| 4 | Una tarea ya tomada **solo la completa su responsable** | lo pide §2 |
| 5 | La **lista definitiva** se asigna sola al administrativo con menos tareas | plan propio + §3.4 |
| 6 | El **jefe docente** elige el docente, y puede elegirse a sí mismo | plan propio |
| 7 | Al docente elegido le llega **asignada, sin reclamo**, y los otros dos **no la ven** | plan propio, dado vuelta |
| 8 | Las **actividades automáticas** no pasan por la bandeja de nadie | lo pide §2 |

Los puntos 3, 4 y 8 son los que faltaban: el enunciado los dice explícitamente y son los
más fáciles de demostrar en pantalla.

---

## Paso a paso

Todo en el sitio, `http://localhost:8081`. Usuarios (contraseña `password` en todos):
`jefeadministrativo` (Laura Giménez), `administrativo1` (Pablo Ferreyra), `jefedocente`
(Marcela Ibáñez), `docente1` (Hernán Rossi), `docente2` (Sofía Quiroga).

### 1. Tareas asignadas por rol — punto 1

Entrar como **`jefeadministrativo`** y abrir **Tareas**. En la lista de *asignadas a mí*
hay tres, de tres cursos distintos: *Archivar solicitud*, *Revisar propuesta* y *Registrar
y elaborar propuesta*.

**Lo que hay que señalar:** ninguna tiene botón de *Reclamar*, solo de *Completar*. Le
llegaron asignadas porque el modelo dice `flowable:assignee="jefeadministrativo"`. Son
exactamente las que §2 le da al jefe administrativo.

### 2. Tareas abiertas al grupo, y que sin reclamar no se completan — puntos 2 y 3

Cerrar sesión y entrar como **`administrativo1`**. En la bandeja aparecen dos listas, y la
de *disponibles para mi grupo* tiene las del curso *Primeros Auxilios*: *Preparar
materiales* y *Evaluar Recursos* de los otros cursos.

**Lo que hay que señalar:** esas tareas **no tienen botón de completar**, solo de reclamar.
Es la regla de §2: una tarea de grupo hay que reclamarla antes de completarla. El motor por
sí solo no lo impide —se puede completar por API— así que la regla la aplica el sitio, y si
se intenta saltear responde *"Primero tenés que reclamar la tarea 'Preparar materiales'"*.

Reclamar *Preparar materiales*. Ahora sí aparece *Completar*, y la tarea se movió a
*asignadas a mí*.

### 3. Una tarea tomada es de una sola persona — punto 4

Sin completarla todavía, cerrar sesión y entrar como **`jefeadministrativo`**.

**Lo que hay que señalar:** *Preparar materiales* **ya no está en su bandeja**. Desapareció
para el resto del grupo en el momento en que Pablo la reclamó. Es la otra mitad de §2: *"no
requerirán reclamo y solo podrán ser completadas por ese usuario"*.

Volver a entrar como `administrativo1` y **completarla**. Ahí está la *finalización*: la
rama de materiales de ese curso quedó cerrada.

### 4. La lista definitiva se asigna sola — punto 5

**Primero mostrar el conteo**, que es lo que hace interesante el punto. Cuántas tareas
tiene asignadas y pendientes cada administrativo:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/tasks?assignee=administrativo1&size=100" | python3 -c "import json,sys; print('administrativo1:', json.load(sys.stdin)['total'])"
```

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/tasks?assignee=jefeadministrativo&size=100" | python3 -c "import json,sys; print('jefeadministrativo:', json.load(sys.stdin)['total'])"
```

Con el estado que deja el escenario 7, Pablo queda en 0 y Laura en 3.

Ahora, desde la **app de comunicaciones** (`java -jar comunicaciones/target/app.jar`),
elegir *Primeros Auxilios* y enviar **la nómina de participantes**.

**Lo que hay que señalar:** aparece *Confeccionar lista definitiva* y **ya está asignada a
Pablo**, sin que nadie la reclame. No lo decidió una persona ni está dibujado en el
diagrama: lo decidió el task listener que pide §3.4 en el momento en que la tarea se creó,
mirando cuántas tareas tenía asignada cada uno.

Completarla como `administrativo1`.

> **Ojo con esto en vivo.** El listener cuenta las tareas asignadas y pendientes **en todas
> las instancias**, no en este curso. Si antes quedaron tareas reclamadas por Pablo sin
> completar, puede tener más que Laura y la lista definitiva le va a tocar **a Laura**. Por
> eso conviene mostrar el conteo antes: así el resultado se predice en voz alta y no
> sorprende.

Con las tres ramas cerradas, la preparación termina y el proceso sigue a *Definir docente*.

### 5. El jefe docente es el único que asigna el dictado — punto 6

Entrar como **`jefedocente`**. En su bandeja está *Definir docente que desarrolla el
curso*, **asignada a él**: es una de sus dos tareas propias según §2, junto con *Elaborar
informe de resultados*.

Abrirla: el desplegable tiene **los tres** integrantes del equipo docente, **él incluido**.
Eso es literal del enunciado: *"pudiendo elegirse a sí mismo"*. Para que el punto siguiente
se vea mejor, elegir a **`docente2`** (Sofía) y completar.

### 6. Al docente elegido le llega asignada; los otros no la ven — punto 7

Entrar como **`docente2`**: tiene *Desarrollar curso* **en asignadas a mí, sin botón de
reclamar**. Entrar como **`docente1`**: **no la ve**.

**Lo que hay que señalar:** esa asignación no está escrita en el modelo con un nombre, sino
con una variable — `flowable:assignee="${docenteSeleccionado}"` — que el motor resuelve en
ejecución con lo que eligió el jefe. §2 la cuenta entre las asignadas *"por la selección
del jefe docente"*: sin reclamo y exclusiva.

Y una línea que cierra el cuadro: **en este proceso el grupo Docentes no tiene ninguna
tarea con grupo candidato**. Las tres tareas docentes van asignadas a una persona, así que
ningún docente reclama nunca nada. Todas las tareas de grupo son de `administracion`.

### 7. Las automáticas no son de nadie — punto 8

Para terminar, señalar en el diagrama las que **nunca aparecieron en ninguna bandeja**:
*Enviar propuesta*, *Habilitar acceso de la empresa* y *Reservar aula* —la que falló en el
escenario 7 sin que nadie la tocara—. §2 lo dice en una línea: *"Las actividades
automáticas no requieren intervención humana"*.

---

## Lo que hay por detrás (para señalar en el diagrama)

**Las dos formas de asignar, en el XML.** Es la diferencia que vale mostrar:

```xml
<!-- Exclusiva de un jefe: viene asignada, no se reclama -->
<userTask id="registraryelaborarpropuesta" flowable:assignee="jefeadministrativo" />

<!-- Abierta al grupo: hay que reclamarla -->
<userTask id="prepararmateriales" flowable:candidateGroups="administracion" />

<!-- Asignada en ejecución, con lo que eligió el jefe docente -->
<userTask id="desarrollarcurso" flowable:assignee="${docenteSeleccionado}" />
```

**El task listener de §3.4**, enganchado en la creación de la lista definitiva:

```xml
<userTask id="confeccionarlistadefinitiva" flowable:candidateGroups="administracion">
  <extensionElements>
    <flowable:taskListener event="create" delegateExpression="${assignLeastBusy}" />
  </extensionElements>
</userTask>
```

Y lo que hace, en
[`TaskAssignmentService`](../../../flowable/src/main/java/com/flowable/app/services/TaskAssignmentService.java):
lista los miembros del grupo ordenados por id, cuenta para cada uno las tareas
**asignadas** —no las de grupo sin reclamar, como aclara el enunciado— y le pone la nueva
al de menor cuenta con `setAssignee`, no con `claim`, para que figure como asignada y no
como pendiente de reclamo. La tarea **conserva el grupo candidato**, así el grupo la ve si
hubiera que reasignarla.

**Las dos reglas que agrega el BFF.** El motor permite completar por API una tarea de otro;
el enunciado no. Así que el backend verifica, antes de completar, que la tarea esté en la
bandeja del usuario, que esté reclamada si es de grupo, y que el responsable sea él. Si no,
responde con el motivo en vez de dejar pasar la operación.

**El test que lo cubre.** `TaskAssignmentTest`: verifica que la tarea del jefe venga
asignada, que la de grupo arranque sin responsable y con `administracion` como candidato,
que un segundo reclamo falle, y que la lista definitiva caiga en el administrativo con
menos tareas.

---

## Si preguntan

**¿Qué pasa si los dos administrativos tienen la misma cantidad de tareas?**
Gana el primero por orden alfabético de id. El enunciado deja el criterio libre; elegimos
uno determinista para que el resultado sea reproducible en los tests y en la demo.

**¿Por qué el listener cuenta tareas de todas las instancias y no solo de este curso?**
Porque §3.4 habla de cuántas tareas tiene asignadas **cada integrante**, no cuántas en ese
curso. La carga de trabajo de una persona no se reparte por expediente.

**¿Por qué la tarea asignada por el listener sigue teniendo grupo candidato?**
Para no perder la posibilidad de que otro integrante la tome si el asignado no puede. El
enunciado solo pide que se asigne al menos ocupado, no que se cierre el grupo.

**¿No alcanzaba con que el motor controle los permisos?**
Para la visibilidad sí: la bandeja se le pide al motor con el usuario, y por eso `docente1`
no ve la tarea de `docente2`. Pero el motor **no** impide completar por API una tarea
ajena, y §2 sí lo prohíbe. Esa parte la aplican el sitio y la app de materiales.

**¿Puede el jefe docente elegirse a sí mismo?**
Sí, y está a propósito: el desplegable trae los tres integrantes del equipo leídos del IDM
del motor, él incluido. Lo pide el enunciado §1.4.

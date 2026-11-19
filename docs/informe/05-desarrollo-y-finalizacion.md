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

Es el **escenario 1**, el camino feliz completo. Son tres cosas para dejar claras: la **asignación dinámica** del docente, el **temporizador sin fecha fija**, y que el proceso **no termina hasta liberar los recursos**.

> La preparación de la máquina está en [02 §3.0](02-viabilidad-y-acuerdo.md#30-preparar-la-máquina-una-sola-vez-antes-de-la-defensa). El diagrama con la instancia resaltada, en [02 §3.1](02-viabilidad-y-acuerdo.md#31-el-truco-para-mostrarlo-gráficamente).

### El código que hay que mostrar

| Qué | Dónde |
|---|---|
| Definir docente (la hace el jefe) | BPMN **línea 106**, `<userTask id="definirdocentequedesarrollaelcurso" flowable:assignee="jefedocente">` |
| **La asignación dinámica** | BPMN **línea 111**, `<userTask id="desarrollarcurso" flowable:assignee="${docenteSeleccionado}">` |
| **El timer sin fecha fija** | BPMN **líneas 116-118**, `<timeDate>${fechaCurso}</timeDate>` |
| El listener que garantiza la fecha | [`ResolveCourseDateListener.java`](../../flowable/src/main/java/com/flowable/app/listeners/ResolveCourseDateListener.java) |
| Elaborar informe | BPMN **línea 121**, `assignee="jefedocente"` |
| Enviar informe y archivar | BPMN **línea 247**, `assignee="jefeadministrativo"` |
| El resultado FINALIZADO | BPMN **línea 391**, `setVariable('resultadoCurso','FINALIZADO')` |
| **La compensación final** | BPMN **líneas 319-320**, `<intermediateThrowEvent id="liberarrecurso">` con `activityRef="prepararcurso"` |
| La lista de docentes, leída del motor | [`FlowableTeacherRepository.java`](../../backend/src/main/java/com/backend/proxy/repository/FlowableTeacherRepository.java) |

Las dos líneas que más conviene mostrar en pantalla son la **111** y la **116-118**: en una sola línea cada una resuelven dos puntos del enunciado (el docente queda en variable *y* asignado; el timer usa la variable *y* no tiene fecha en el modelo).

### Paso a paso

El script ya dejó instancias paradas en cada etapa, así que podés mostrar las cuatro sin esperar nada. Buscá en la lista que imprimió.

**A. La asignación dinámica.** Esta tarea el script **no la deja preparada**: solo cinco cursos pueden estar en ejecución a la vez (§3.1) y el sexto estado no entra. Se llega en treinta segundos desde el curso que quedó en preparación, y de paso se muestra el paralelismo:

1. Entrá al sitio como `administrativo1` / `password` → **Mis tareas**. Vas a ver las tareas del curso *"Atención al Cliente"*, que está en preparación.
2. Completá las dos que están en "Disponibles para mi grupo" (**Reclamar** y después **Completar** en cada una): *Evaluar Recursos* —tildá **Aula virtual**— y *Preparar materiales*. Después completá *Solicitar Aula Virtual*.
3. Falta la nómina, que la manda la empresa. Abrí la app de comunicaciones, elegí ese curso y enviá **Nómina de participantes**. Aparece *Confeccionar lista definitiva*: reclamala y completala.
4. Con las tres ramas cerradas, la preparación termina y aparece *Definir docente*. Ahora sí:

5. Entrá al sitio como `jefedocente` / `password` → **Mis tareas**.
6. Está *Definir Docente que desarrolla el curso*, **ya asignada** (no hay que reclamarla: es tarea exclusiva del jefe). Apretá **Completar**.
7. Se abre un desplegable con **los tres docentes**: Marcela Ibáñez (jefedocente), Hernán Rossi (docente1) y Sofía Quiroga (docente2). **El jefe está en la lista**: puede elegirse a sí mismo, como pide el enunciado.
8. Elegí **Hernán Rossi (docente1)** y apretá **Asignar docente**.
9. Mostrá que la tarea siguiente le quedó asignada **a él y a nadie más**:
   - Cerrá sesión (**Salir** arriba a la derecha) y entrá como `docente2` / `password` → **Mis tareas**: no la ve.
   - Entrá como `docente1` → **Mis tareas**: ahí está *Desarrollar curso*, lista para completar.

**B. El temporizador** — mostrá que el proceso quedó esperando la fecha:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/management/timer-jobs?processInstanceId=PEGAR_EL_ID" | python3 -m json.tool | grep -E "dueDate|id"
```

Se ve la fecha acordada con la empresa. En el diagrama, el círculo rojo está sobre **Fecha programada**.

Si querés mostrar que dispara, podés forzarlo sin esperar (esto es lo que hace el script internamente):

```bash
JOB=$(curl -s -u admin:test "http://localhost:8080/process-api/management/timer-jobs?processInstanceId=PEGAR_EL_ID" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'][0]['id'])")
curl -s -o /dev/null -w "%{http_code}\n" -u admin:test -X POST "http://localhost:8080/process-api/management/timer-jobs/$JOB" -H 'Content-Type: application/json' -d '{"action":"move"}'
```

Devuelve `204` y la tarea *Desarrollar curso* aparece en la bandeja del docente.

**C. El dictado y el informe:**

1. Como `docente1` → **Mis tareas** → *Desarrollar curso* → **Completar**.
2. Como `jefedocente` → **Mis tareas** → *Elaborar informe de resultados* → **Completar**.
3. Se abre un cuadro de texto. Escribí algo como *"Asistieron 18 de 20 inscriptos. Todos aprobaron la evaluación final."* y apretá **Completar tarea**.

**D. El cierre con compensación** — es el final del escenario 1:

1. Como `jefeadministrativo` → **Mis tareas** → *Enviar informe y archivar* → **Completar**.
2. La pantalla **muestra el informe que escribió el equipo docente** para que lo revise antes de mandarlo. Mostrá eso: es el requisito de §4.1.3 de que el jefe administrativo pueda consultar el informe.
3. **Tildá la casilla de confirmación** y apretá **Enviar y archivar**. Sin tildarla no deja seguir, a propósito: cierra el curso y no tiene vuelta atrás.
4. Mostrá que el proceso terminó **después** de liberar el aula:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-variable-instances?processInstanceId=PEGAR_EL_ID&size=60" | python3 -m json.tool | grep -A2 -E "aulaLiberada|resultadoCurso"
```

Tiene que decir `aulaLiberada: true` y `resultadoCurso: FINALIZADO`. Y la instancia ya no existe en runtime:

```bash
curl -s -o /dev/null -w "%{http_code} (404 = terminada)\n" -u admin:test "http://localhost:8080/process-api/runtime/process-instances/PEGAR_EL_ID"
```

5. Y en la base de la institución:

```bash
docker exec flowable-postgres psql -U flowable -d flowable -c "select id, name, agreed_date, left(report,40) as informe, status from courses where status is not null order by id desc limit 3;"
```

### Correr los tests en vivo

```bash
cd flowable && mvn test -DforkCount=0 -Dtest=HappyPathTest,CourseDateTest && cd ..
```

- `HappyPathTest` recorre todo el camino y verifica la asignación dinámica, la espera en el timer, el cierre y la liberación del aula.
- `CourseDateTest` es el que importa para §3.5: recorre el camino completo **sin** inyectar la fecha al iniciar la instancia, que es el caso real. Antes de ese listener, completar *Definir docente* reventaba con `Unknown property used in expression: ${fechaCurso}`.

### Si algo sale mal en vivo

| Síntoma | Qué hacer |
|---|---|
| El desplegable de docentes sale vacío | El motor no tiene el grupo `docentes`. Comprobá con `curl -s http://localhost:8080/groups` |
| *Desarrollar curso* no aparece | Falta que dispare el timer. Usá el comando del punto B |
| *Enviar y archivar* no hace nada | Falta tildar la casilla de confirmación |
| La instancia no cierra tras confirmar | Quedó alguna compensación pendiente: mirá si hay una tarea *Liberar Equipamiento* en la bandeja del grupo |

## 4. Preguntas probables

**¿Dónde está la fecha de inicio del curso?**
En la variable `fechaCurso` de la instancia, y en la columna `courses.agreed_date` de la BD de la institución. En el modelo no hay ninguna fecha: el temporizador es una expresión.

**¿Qué pasa si la empresa acepta sin mandar fecha?**
El listener deriva una del offset configurado y lo deja asentado en el log. Es una decisión de robustez: el enunciado exige que el valor esté definido antes de alcanzar el evento, y preferimos una fecha derivada y avisada antes que una instancia trabada.

**¿Por qué el informe se lee de la variable y no de la base, si también está persistido?**
Porque mientras la instancia vive, la variable es la fuente de verdad y la base es un registro paralelo que se sincroniza por listener. Si el BFF estuvo caído en ese momento, la pantalla igual funciona. El criterio completo está en [`adr/datos-proceso-vs-bd.md`](../../adr/datos-proceso-vs-bd.md).

**¿Por qué se compensa al finalizar bien, si no hubo ningún error?**
Porque el enunciado lo pide así: la liberación de los recursos al terminar el curso es la misma operación que al cancelarlo. Usar compensación en los dos casos evita duplicar las actividades de liberación y garantiza que se libere solo lo que se había gestionado.

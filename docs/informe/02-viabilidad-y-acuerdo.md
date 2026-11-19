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

### 3.0 Preparar la máquina (una sola vez, antes de la defensa)

Todo se corre desde la carpeta del proyecto. Abrí una terminal y entrá:

```bash
cd ~/Desktop/"Computer's Science"/"4rd year"/"Gestion y Tecnologias de Procesos de Negocio"/GTPN-GestionCursos-Flowable
```

**1. Compilar los dos jars** (las imágenes de Docker no compilan, solo copian el jar):

```bash
cd flowable && ./build.sh && cd ../backend && ./build.sh && cd ..
```

**2. Levantar los cuatro servicios:**

```bash
docker compose up -d --build
```

Esperá ~40 segundos y comprobá que los cuatro digan `healthy` o `Up`:

```bash
docker compose ps
```

**3. Dejar una instancia parada en cada escenario:**

```bash
.venv/bin/python scripts/preparar_demo.py --limpiar
```

Al terminar imprime una lista con **qué instancia quedó en cada escenario y con qué usuario verla**. Sacale una foto o dejá esa terminal abierta: vas a necesitar los ids.

> Si falla con `No module named requests`, falta el entorno de Python. Se arma una sola vez con los comandos de [`mineria/README.md`](../../mineria/README.md).

**4. Dejar abierto en el navegador:** `http://localhost:8081` — el sitio de la institución.

Los usuarios son `jefeadministrativo`, `administrativo1`, `jefedocente`, `docente1` y `docente2`. **La contraseña de todos es `password`.**

### 3.1 El truco para mostrarlo gráficamente

Esto es lo que conviene tener a mano: Flowable sabe dibujar el diagrama **con la instancia resaltada en rojo** donde está parada. Con el id de una instancia:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/process-instances/PEGAR_EL_ID_ACA/diagram" -o /tmp/diagrama.png && xdg-open /tmp/diagrama.png
```

Se abre el visor de imágenes con el BPMN completo y un **círculo rojo** sobre el nodo donde está esperando. Es la forma más directa de mostrar "miren, la instancia está acá".

> **No uses el Admin App de Flowable UI** (`localhost:8088`). Viene apuntando a otro motor por defecto y hay que reconfigurarlo; en la defensa no carga nada. Todo lo que necesitás está en el sitio propio y en este comando.

---

### Escenario 5 — Límite de cursos en ejecución (§3.1)

**Qué mostrar:** que una solicitud nueva se cierra sola, sin llegar a elaborar propuesta, cuando ya hay cinco cursos en ejecución.

**El código:**

| Qué | Dónde |
|---|---|
| Listener que cuenta | [`CountActiveCoursesListener.java`](../../flowable/src/main/java/com/flowable/app/listeners/CountActiveCoursesListener.java) |
| La consulta al motor | [`CourseProcessService.java:29`](../../flowable/src/main/java/com/flowable/app/services/CourseProcessService.java#L29) |
| La decision task | BPMN **línea 302**, `<serviceTask id="evaluarviabilidad" flowable:type="dmn">` |
| La tabla de decisión | [`evaluarViabilidad.dmn`](../../flowable/src/main/resources/dmn/evaluarViabilidad.dmn) |
| El flujo que marca el resultado | BPMN **línea 374**, `setVariable('resultadoCurso','NO_VIABLE')` |

**Paso a paso:**

1. En el navegador, entrá a `http://localhost:8081` (sin loguearte: la portada es pública).
2. Completá el formulario con cualquier dato. En **Fechas preferidas** poné una fecha futura, por ejemplo `2027-09-01`.
3. Apretá **Enviar solicitud**. Aparece "Solicitud registrada" con un número.
4. Mostrá en la terminal que se cerró sola y por qué:

```bash
docker compose logs --tail 30 flowable-app | grep -E "active courses|Otros cursos"
```

Se ve `Otros cursos activos: 5`. Como `5 >= 5`, el DMN devolvió `viable = false`.

5. Mostrá el resultado que registró la instancia:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-variable-instances?variableName=resultadoCurso&size=200" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data'][-1]['variable']['value'])"
```

Devuelve `NO_VIABLE`.

> **Ojo con este caso:** el curso queda en la tabla `courses` **sin** la columna `status`
> cargada. Hay una condición de carrera conocida: la instancia no viable termina tan
> rápido que el listener de cierre avisa al backend antes de que el alta haya guardado el
> `process_instance_id`, así que el backend responde 404 y el estado no se registra. Se
> ve en los logs del motor como `Error al actualizar el estado del curso en backend: 404`.
> Está anotado en [`TODO.md`](../../TODO.md). **En la defensa, mostrá el `resultadoCurso`
> del historial** —que sí es correcto— y no la columna `status` para este escenario en
> particular. Para los otros cuatro resultados (finalizado, cancelado, rechazado, sin
> respuesta) la columna se carga bien, porque esas instancias viven lo suficiente.

**Test que lo cubre:** `CourseLimitExceededTest`. Para correrlo en vivo:

```bash
cd flowable && mvn test -DforkCount=0 -Dtest=CourseLimitExceededTest && cd ..
```

> El `-DforkCount=0` hace falta siempre en esta máquina: la ruta del proyecto tiene un apóstrofe (`Computer's Science`) y sin eso Maven falla al lanzar el proceso de tests. Está explicado en el [README](../../README.md).

---

### Escenario 3 — Rechazo de la propuesta (§1.1)

**Qué mostrar:** la empresa rechaza, la solicitud va a *Archivar solicitud* y el proceso cierra como `RECHAZADO`.

**El código:**

| Qué | Dónde |
|---|---|
| La compuerta de espera | BPMN **línea 271**, `<eventBasedGateway id="esperarrespuesta">` |
| El evento de rechazo | BPMN **línea 261**, `<intermediateCatchEvent id="rechazo">` |
| La tarea de archivo | BPMN **línea 284**, `<userTask id="archivarsolicitud" flowable:assignee="jefeadministrativo">` |
| El flujo que marca el resultado | BPMN **línea 379**, `setVariable('resultadoCurso','RECHAZADO')` |

**Paso a paso:**

1. De la lista que imprimió el script, buscá el escenario **"2/3/4. Respuesta de la empresa"** y copiá su id de instancia.
2. Mostrá dónde está parada con el comando del diagrama (3.1): se ve el círculo rojo sobre los cuatro eventos de *Esperar respuesta* a la vez. **Ese es el punto fuerte de la compuerta basada en eventos:** el proceso espera las cuatro cosas en paralelo.
3. Abrí la **app de comunicaciones** (la que simula a la empresa). En otra terminal:

```bash
java -jar comunicaciones/target/app.jar
```

4. Se abre una ventana. En la lista de solicitudes, **hacé clic en la del curso "Seguridad e Higiene"**.
5. Abajo aparecen solo las comunicaciones habilitadas en ese momento: *Aceptación*, *Rechazo* y *Solicitud de modificaciones*. **Elegí "Rechazo"** y apretá **Enviar**.
6. Volvé al sitio, entrá como `jefeadministrativo` / `password` y andá a **Mis tareas**. Apareció **Archivar solicitud**. Apretá **Completar**.
7. Mostrá el cierre:

```bash
docker exec flowable-postgres psql -U flowable -d flowable -c "select id, name, status from courses where name='Seguridad e Higiene' order by id desc limit 1;"
```

**Test:** `ProposalRejectionTest`.

---

### Escenario 4 — Cierre por falta de respuesta (§1.1)

**Qué mostrar:** pasan los plazos, salen **dos** recordatorios y recién entonces se archiva.

**El código:**

| Qué | Dónde |
|---|---|
| El temporizador (sin fecha fija) | BPMN **línea 272**, `<intermediateCatchEvent id="plazo5dias">` con `<timeDuration>${plazoRespuesta}</timeDuration>` |
| El script que cuenta | BPMN **líneas 277-283**, `<scriptTask id="enviarrecordatorio">` |
| La compuerta del límite | BPMN **línea 289**, `¿Se enviaron 2 recordatorios?` |
| Sus dos salidas | BPMN **línea 364** (`no` → volver a esperar) y **línea 382** (`si` → archivar) |
| El flujo que marca el resultado | BPMN **línea 384**, `setVariable('resultadoCurso','SIN_RESPUESTA')` |

**Paso a paso:**

El script ya dejó una instancia con los dos recordatorios mandados y parada en *Archivar solicitud*. Buscá en la lista el escenario **"4. Cierre por falta de respuesta"**.

1. Mostrá que salieron exactamente dos:

```bash
docker compose logs flowable-app | grep "Recordatorio"
```

Sale `Recordatorio 1 enviado` y `Recordatorio 2 enviado`. **Nada más**: es la prueba de que se respeta el máximo de §1.1.

2. Mostrá el contador de la instancia:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-variable-instances?processInstanceId=PEGAR_EL_ID&variableName=recordatorios"
```

3. Entrá al sitio como `jefeadministrativo`, **Mis tareas** → **Archivar solicitud** → **Completar**.

**Si te preguntan por qué no esperamos cinco días reales:** porque el temporizador es una variable, no una fecha fija. Se baja a un minuto sin tocar el modelo. Para mostrarlo en vivo, en [`docker-compose.yml`](../../docker-compose.yml) hay una línea comentada; se descomenta:

```yaml
PROCESO_PLAZO_RESPUESTA: PT1M
```

y se recarga el motor con `docker compose up -d flowable-app`. A partir de ahí cada vuelta tarda un minuto y se ven los recordatorios apareciendo en vivo en los logs:

```bash
docker compose logs -f flowable-app
```

(Se corta con `Ctrl+C`.)

**Test:** `NoResponseTest`.

---

### Si algo sale mal en vivo

| Síntoma | Qué hacer |
|---|---|
| El sitio no abre | `docker compose ps` — si alguno no está `Up`, `docker compose up -d` |
| La solicitud nueva sale siempre NO VIABLE | Es correcto si hay 5 cursos activos. Para liberar: `.venv/bin/python scripts/preparar_demo.py --limpiar` |
| No aparece la tarea en la bandeja | Fijate con qué usuario entraste: cada tarea la ve solo su responsable o su grupo |
| La app de comunicaciones no lista nada | El motor no responde; revisá `docker compose ps` |

## 4. Preguntas probables

**¿Por qué una compuerta basada en eventos y no una exclusiva?**
Porque la institución no decide: espera. Los cuatro caminos son eventos externos (tres mensajes y un temporizador) y la compuerta basada en eventos es la construcción BPMN para "el primero que llegue", cancelando el resto sin código.

**¿Cómo se garantiza que no haya más de dos recordatorios?**
Por la compuerta `¿Se enviaron 2 recordatorios?`, que compara la variable `recordatorios` y deriva a *Archivar solicitud* cuando se pasa. El contador se reinicia en cada *Enviar propuesta*, así que una propuesta revisada vuelve a tener sus dos recordatorios, como pide §1.1.

**¿Por qué el conteo de cursos activos no está dentro del DMN?**
Porque §3.1 pide que la cantidad **se obtenga del motor**. El DMN solo decide; quien mide es el listener. Así la regla queda declarativa y editable desde el Modeler sin tocar Java.

**¿Qué pasa si la empresa acepta y rechaza casi al mismo tiempo?**
Gana el primer mensaje que el motor procesa; el segundo encuentra la ejecución ya cancelada y la API REST responde que no hay suscripción. La app de comunicaciones, además, solo ofrece las comunicaciones que el motor declara habilitadas en ese momento.

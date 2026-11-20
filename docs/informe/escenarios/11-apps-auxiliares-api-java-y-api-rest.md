# Escenario 11 — Preparación de materiales por API Java y comunicaciones por API REST

> **Explica:** Damián · **Cubre:** §4.2.1 y §4.2.2 del enunciado
> · Detalle técnico: [`10-apps-auxiliares.md`](../10-apps-auxiliares.md)
> · READMEs: [`materiales/`](../../../materiales/README.md) ·
> [`comunicaciones/`](../../../comunicaciones/README.md)
>
> Conviene mostrarlo **después del 7 y el 8**, porque reusa la misma preparación y ya
> quedó explicado qué es una tarea de grupo y qué es un reclamo.

---

## Lo importante a mostrar

**Que son dos mecanismos de integración distintos, a propósito.** La app de materiales usa
la **API Java**: levanta un motor Flowable embebido contra la misma base y trabaja con
`TaskService`. La de comunicaciones usa la **API REST**: le habla por HTTP a
`/process-api`. El enunciado pide una de cada una, y si las dos hablaran igual una estaría
mal.

**Y que las tres interfaces operan sobre las mismas instancias y tareas**, que es lo que
exige §4. Es el remate del escenario: aceptar una propuesta desde una app de escritorio y
ver aparecer la tarea en la otra, y en el sitio web, sobre el mismo curso.

> **Por qué las dos tienen ventana.** §4.2.2 permite hacer las comunicaciones por línea de
> comandos **solo a los grupos de hasta 3 personas**. Somos cinco, así que las dos apps
> tienen interfaz gráfica.

---

## Antes de empezar

Compilar los dos jars, una vez:

```bash
cd materiales && ./build.sh && cd ../comunicaciones && ./build.sh && cd ..
```

> `./build.sh` hace `clean package` y deja `target/app.jar`. Hay que repetirlo después de
> cualquier `mvn clean`: `mvn test` compila pero no empaqueta, y sin el jar sale
> `Unable to access jarfile`.

Y dejar los escenarios preparados, si no se hizo ya:

```bash
.venv/bin/python scripts/preparar_demo.py --limpiar
```

De todo lo que deja el script, este escenario usa dos cursos:

| Curso | Estado en el que quedó | Para qué se usa acá |
|---|---|---|
| *Seguridad e Higiene* | esperando la respuesta de la empresa | la app de comunicaciones |
| *Atencion al Cliente* | en preparación, con las tres ramas abiertas | la app de materiales |

Las dos apps se abren con:

```bash
java -jar comunicaciones/target/app.jar
```

```bash
java -jar materiales/target/app.jar
```

Conviene abrir las dos antes de empezar y dejarlas al lado del navegador.

---

## Paso a paso

### 1. La app de comunicaciones hace de empresa

En la ventana aparecen las solicitudes en curso, con su curso, su empresa y el id del
curso en la base de la institución. Elegir **Seguridad e Higiene**.

**Lo que hay que señalar:** abajo aparecen **tres** botones — aceptar, rechazar y pedir
modificaciones — y **no** están ni la nómina ni la cancelación. El enunciado pide mostrar
solo las comunicaciones habilitadas en el estado actual, y esta instancia está esperando
la respuesta a la propuesta: todavía no corresponde mandar participantes ni cancelar nada.

Elegir **Aceptar la propuesta**. Pide la **fecha acordada de inicio** — §4.2.2 pide que la
aceptación la incluya — y conviene poner una fecha cercana, por ejemplo mañana.

### 2. El mismo curso, ahora con otras comunicaciones

Apretar **Actualizar** y volver a elegir *Seguridad e Higiene*.

**Lo que hay que señalar:** ahora los botones son **otros dos**: enviar la nómina y
cancelar. Cambió el estado de la instancia y cambió lo que la empresa puede comunicar, sin
que la app sepa nada del proceso. No hay reglas duplicadas: la app le **pregunta al motor**
qué mensajes está esperando esa instancia.

### 3. La app de materiales, del otro lado del mostrador

Pasar a la otra ventana. Entrar con **`administrativo1` / `password`** (Pablo Ferreyra).

**Lo que hay que señalar, y es el remate del escenario:** en la lista hay **dos** tareas
*Preparar materiales*. Una es la del curso *Atencion al Cliente*, que ya estaba. **La otra
es la del curso que acabamos de aceptar desde la otra app**, que apareció porque el proceso
avanzó y abrió la rama de materiales.

Dos apps distintas, dos APIs distintas, la misma instancia.

También se ve el curso y la empresa de cada tarea: salen de las variables de la instancia,
las que escribió el sitio al dar de alta la solicitud.

### 4. Identificarse, reclamar y completar — las tres funciones de §4.2.1

- **Identificarse** ya pasó: el login es con el usuario y la contraseña **de Flowable**,
  los mismos del sitio (§3.3). Si se prueba con `docente1`, la app lo rechaza: *Preparar
  materiales* es del grupo Administración (§2).
- **Reclamar**: la tarea del curso nuevo está como *Disponible para el grupo*. Al
  reclamarla pasa a *Asignada a mí*.
- **Completar**: pide un detalle opcional de los materiales. Escribir algo como
  *"Apuntes impresos y VM de laboratorio"* y aceptar. La tarea desaparece de la lista.

### 5. Que las tres interfaces ven lo mismo

Sin cerrar nada, ir al sitio web (`http://localhost:8081`) y entrar como
**`jefeadministrativo`**.

**Lo que hay que señalar:** la tarea *Preparar materiales* que Pablo reclamó desde la app
de escritorio **no está en la bandeja de Laura**. No es que cada aplicación tenga su propia
lista: las dos le preguntan al motor, y el motor ya sabe que esa tarea tiene responsable.

Y en la pantalla del curso, la empresa ve el detalle que se cargó desde la app.

### 6. Cerrar la rama de inscripciones, encadenando las dos apps

Opcional, pero queda redondo si hay tiempo: volver a la app de comunicaciones y enviar
**la nómina de participantes** del mismo curso. En el sitio aparece *Confeccionar lista
definitiva* **ya asignada** por el listener de §3.4 (es el escenario 8), y con eso quedan
cerradas dos de las tres ramas de la preparación.

---

## Lo que hay por detrás (para contar mientras se muestra)

### La app de materiales: API Java

No usa Spring ni la API REST. Arma un `ProcessEngine` **embebido** contra el mismo
PostgreSQL del motor y trabaja con `IdentityService` y `TaskService`. Que "tiene alcance a
la BD del motor" **no** significa que escriba SQL: la API Java *es* la API del motor, el
mismo código que corre en el servidor de 8080 pero dentro del proceso de la app.

Tres recaudos para no pisar al motor, en
[`EngineFactory`](../../../materiales/src/main/java/com/materiales/app/engine/EngineFactory.java):

| Recaudo | Por qué |
|---|---|
| `databaseSchemaUpdate = false` | No crea ni migra tablas: el dueño del esquema es la app del motor. Si las versiones no coinciden, falla al arrancar en vez de tocar la base |
| Async executor apagado | No compite por los jobs ni los temporizadores del motor |
| Event registry deshabilitado | No lo usa, y arrancarlo dispara un chequeo de Liquibase |

La bandeja son **dos consultas** —`taskAssignee` y `taskCandidateGroupIn` con los grupos
que resolvió el login— en vez de `taskCandidateOrAssigned`, para que los grupos candidatos
sean explícitos y no dependan de la resolución implícita del motor.

Y la app valida lo que el motor **no** verifica: una tarea sin reclamar no se completa, y
una asignada solo la completa su responsable (§2).

### La app de comunicaciones: API REST

Una sola dependencia de runtime, Jackson para los JSON; el cliente HTTP es el del JDK.
**No pide login**: la empresa no es usuario del motor —§3.3 solo pide usuarios para el
personal— así que usa la cuenta técnica.

Lo de "solo las habilitadas" no es una tabla de estados escrita a mano. Por cada mensaje le
pregunta al motor qué ejecución está suscripta:

```http
GET /runtime/executions?processInstanceId=...&messageEventSubscriptionName=Aceptación
```

Si hay ejecución, la comunicación se ofrece, y esa misma respuesta trae el `executionId` al
que hay que mandársela:

```http
PUT /runtime/executions/{executionId}
{"action":"messageEventReceived","messageName":"Aceptación",
 "variables":[{"name":"fechaCurso","type":"string","value":"2027-03-01"}]}
```

Las cinco comunicaciones y el dato que aporta cada una:

| Comunicación | Dato | Variable |
|---|---|---|
| Aceptación | fecha acordada de inicio | `fechaCurso` |
| Rechazo | — | — |
| Solicitud de modificaciones | observaciones | `observaciones` |
| Nómina de participantes | participantes | `participantes` |
| Cancelación | — | — |

La fecha se **valida antes** de llamar al motor, para que un formato mal escrito se vea en
el momento y no como una instancia trabada en el temporizador.

### Los tests

**26** en la app de materiales, sobre un motor embebido en H2 —sin PostgreSQL y sin
pantalla—, y **16** en la de comunicaciones, usando el servidor HTTP del JDK como stub de
la API REST, así que corren sin el motor levantado. Las ventanas Swing se prueban a mano.

---

## Si preguntan

**¿Por qué una app usa la API Java y la otra la REST?**
Porque el enunciado pide exactamente eso: §4.2.1 dice "deberá utilizar la API Java" y
§4.2.2 dice "cada comunicación deberá enviarse mediante la API REST". Son dos formas de
integrarse con el motor y el trabajo pide mostrar las dos.

**¿No es riesgoso tener dos motores sobre la misma base?**
Flowable lo soporta, y el stack ya lo hacía: el contenedor de Flowable UI comparte el mismo
PostgreSQL. La app es el participante más conservador de los tres: no toca el esquema y no
toma jobs.

**¿Por qué la app de comunicaciones no pide usuario y contraseña?**
Porque la empresa no es usuario del motor. §3.3 pide usuarios solo para el personal de la
institución; la empresa tiene su cuenta en la base de la institución, y esa se usa en el
sitio web, no acá. Esta app **simula** a la empresa del otro lado.

**¿Cómo sabe qué comunicaciones ofrecer?**
Le pregunta al motor por las suscripciones a mensajes de esa instancia. Es la única fuente
de verdad y así la app no puede quedar desincronizada del proceso. Si mañana se agrega un
mensaje al modelo, no hay que tocar ninguna regla de estados.

**En la app de materiales no aparece ninguna tarea. ¿Está mal?**
Casi siempre significa que **no hay ninguna tarea *Preparar materiales* abierta**: o ya se
completó, o el curso se canceló y el motor borró sus tareas pendientes. La app mira solo esa
tarea, que es lo que pide §4.2.1. Se verifica con:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/tasks?taskDefinitionKey=prepararmateriales"
```

Y si otro administrativo la reclamó, al resto del grupo ya no le aparece: eso es el
comportamiento correcto según §2, no un error.

**¿Por qué tienen interfaz gráfica si el enunciado permitía línea de comandos?**
Porque esa opción es solo para grupos de hasta tres personas. Igual, las dos están armadas
con la lógica separada de la ventana: los 42 tests prueban la capa de servicio, no la
interfaz.

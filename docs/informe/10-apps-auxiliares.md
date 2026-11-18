# 10 — Aplicaciones auxiliares de escritorio

> **Explica:** Damián · **Cubre:** §4.2.1 (preparación de materiales con la API Java), §4.2.2 (simulación de comunicaciones con la API REST)
>
> READMEs de cada módulo: [`materiales/`](../../materiales/README.md) · [`comunicaciones/`](../../comunicaciones/README.md)

---

## 1. Qué pide el enunciado

**§4.2.1.** Una app **con interfaz gráfica**, local a la intranet y con alcance a la BD del motor, que **debe usar la API Java de Flowable** (no la REST) y permitir a un administrativo: identificarse y consultar las tareas *Preparar materiales* asignadas o reclamables, reclamar una disponible para su grupo y completarla.

**§4.2.2.** Una app que permita seleccionar una solicitud y simular las comunicaciones de la empresa: aceptación (**con la fecha acordada**), rechazo, solicitud de modificaciones (**con las observaciones**), envío de la **nómina** de participantes y **cancelación** durante la preparación. Cada comunicación se envía **mediante la API REST** y debe mostrar **solo las comunicaciones habilitadas en el estado actual**.

> **Sobre la interfaz de la segunda.** El enunciado permite hacerla por línea de comandos **solo a los grupos de hasta 3 personas**. Somos más, así que se hizo **con interfaz gráfica**, igual que la primera.

Las dos son deliberadamente **dos mecanismos de integración distintos**: una por la API Java embebida, la otra por HTTP. Si las dos hablaran igual, una estaría mal.

---

## 2. App de materiales (§4.2.1) — API Java

Módulo [`materiales/`](../../materiales/). Java + Swing, **sin Spring**.

### Cómo se conecta

Levanta un `ProcessEngine` **embebido** contra el **mismo PostgreSQL** del motor y trabaja con `IdentityService` y `TaskService`. No pasa por la API REST ni por el BFF: es la API Java que pide el enunciado.

Que "tiene alcance a la BD del motor" **no** significa que escriba SQL a mano: la API Java *es* la API del motor, el mismo código que corre en el servidor de 8080 pero dentro del proceso de la app, con sus transacciones y sus validaciones.

Tres recaudos para no pisar al motor ([`EngineFactory`](../../materiales/src/main/java/com/materiales/app/engine/EngineFactory.java)):

| Recaudo | Por qué |
|---|---|
| `databaseSchemaUpdate = false` | La app **no crea ni migra tablas**: el dueño del esquema es la app del motor. Si las versiones no coinciden, falla al arrancar en vez de tocar la base |
| Async executor apagado | No compite por los jobs ni por los temporizadores del motor |
| Event registry deshabilitado | No lo usa, y arrancarlo dispara un chequeo de Liquibase sobre sus tablas |

Compartir una base entre varios motores no es una rareza: el contenedor `flowable-ui` hace exactamente lo mismo.

### Las tres funciones

1. **Identificarse.** Usuario y contraseña **de Flowable** (§3.3), validados con `identityService.checkPassword`. Además exige pertenecer al grupo `administracion`, porque *Preparar materiales* es de ese grupo (§2): un docente queda afuera con un mensaje claro.
2. **Consultar la bandeja.** Las tareas *Preparar materiales* **asignadas** al usuario más las **reclamables** por su grupo, con el curso y la empresa de cada instancia (salen de las variables que dejó el alta). Son **dos consultas** —`taskAssignee` y `taskCandidateGroupIn` con los grupos que resolvió el login— en vez de `taskCandidateOrAssigned`, para que los grupos candidatos sean explícitos y no dependan de la resolución implícita del motor.
3. **Reclamar y completar.** El reclamo lo rechaza el motor si otro se la llevó primero, y la app lo muestra. Al completar deja `materialesPreparados = true` y, opcionalmente, un `detalleMateriales`. La app valida lo que el motor **no** verifica: una tarea sin reclamar no se completa, y una asignada solo la completa su responsable (§2).

### Además: el análisis de §4.3

El enunciado permite incorporar las consultas del análisis de ejecuciones a esta app, y así está: botón **Análisis de ejecuciones** en la ventana, y también por consola. Lo explica [11](11-analisis-y-mineria.md).

### Correrla

```bash
cd materiales && ./build.sh
java -jar materiales/target/app.jar
```

Entrar con `administrativo1` / `password`. La conexión se cambia sin recompilar con `-Ddb.url=...`, `DB_URL` o editando `materiales.properties`.

**26 tests** sobre un motor embebido en H2: corren sin PostgreSQL y sin pantalla.

---

## 3. App de comunicaciones (§4.2.2) — API REST

Módulo [`comunicaciones/`](../../comunicaciones/). Java + Swing, sin Spring, con una sola dependencia de runtime (Jackson, para los JSON); el cliente HTTP es el del JDK. El jar queda en poco más de 2 MB.

### Las cinco comunicaciones

| Comunicación | Dato que aporta | Variable |
|---|---|---|
| Aceptación de la propuesta | fecha acordada de inicio | `fechaCurso` |
| Rechazo de la propuesta | — | — |
| Solicitud de modificaciones | observaciones | `observaciones` |
| Nómina de participantes | participantes | `participantes` |
| Cancelación | — | — |

Cada una se envía con `PUT /runtime/executions/{executionId}` y `action: messageEventReceived`, con el nombre del mensaje tal como está en el BPMN.

No pide login: la empresa **no es usuario del motor** —§3.3 solo pide usuarios para el personal— así que la app usa la cuenta técnica.

### Solo las habilitadas, y por qué eso importa

Esta es la parte interesante. La app **no deduce** el estado con reglas propias: por cada mensaje le pregunta al motor qué ejecución está suscripta,

```http
GET /runtime/executions?processInstanceId=...&messageEventSubscriptionName=Aceptación
```

que es la **única fuente de verdad** y además devuelve el `executionId` al que hay que enviarlo. Así la app no puede quedar desincronizada del proceso:

| Estado de la instancia | Qué ofrece la app |
|---|---|
| Esperando respuesta a la propuesta | Aceptar, rechazar, pedir modificaciones |
| En preparación | Enviar la nómina, cancelar |
| Durante el dictado o cerrada | Ninguna |

La fecha de la aceptación se **valida antes** de llamar al motor, para que un formato mal escrito se vea en el momento y no como una instancia trabada en el temporizador.

### Correrla

```bash
cd comunicaciones && ./build.sh
java -jar comunicaciones/target/app.jar
```

**16 tests** que usan el servidor HTTP del JDK como stub de la API REST: corren sin el motor levantado. Los nombres de los parámetros REST se verificaron contra `flowable-rest-6.8.0.jar`, no de memoria.

---

## 4. Cómo se demuestran

Es el **escenario 11**, y se muestra mejor encadenando las dos:

1. Abrir la app de comunicaciones, elegir una solicitud que esté esperando respuesta: aparecen las tres comunicaciones de la negociación.
2. Aceptar con una fecha cercana. Refrescar: esa misma instancia ahora ofrece **otras dos** —nómina y cancelación—, porque pasó a preparación.
3. Abrir la app de materiales con `administrativo1`: aparece la tarea *Preparar materiales* de esa instancia, reclamable por el grupo.
4. Reclamarla (el otro administrativo deja de verla), completarla con un detalle.
5. Volver a comunicaciones, enviar la nómina, y completar *Confeccionar lista definitiva*: cierra la preparación.

Las dos operan sobre **las mismas instancias y tareas** que el sitio web y que Flowable UI, que es lo que pide §4: todo lo que se hace en una se ve en las otras.

---

## 5. Preguntas probables

**¿Por qué las apps no ocupan un puerto como el resto del stack?**
Porque son **clientes**, no servidores: nadie las llama, ellas llaman. Como `psql` o el navegador. La de materiales habla con PostgreSQL por la API Java y la de comunicaciones con la API REST del motor en 8080.

**¿Por qué sin Spring?**
Son apps de escritorio de diez clases con una dependencia; un contenedor de inyección no resolvería nada. Y en el caso de materiales, usar el *starter* de Flowable la habría convertido en una **segunda aplicación Flowable completa** sobre la misma base, con autodespliegue de procesos y su propio async executor compitiendo por los temporizadores.

**¿Dos motores sobre la misma base no es peligroso?**
Flowable lo soporta y el stack ya lo hacía con `flowable-ui`. Igual la app es el participante más conservador de los tres: no toca el esquema y no toma jobs.

**¿Por qué la app de materiales valida cosas que el motor no?**
Porque el motor permite completar una tarea asignada a otro si se lo pide por API. Las reglas de §2 —reclamar antes de completar, y que solo el responsable complete— son de negocio, y las aplican tanto esta app como el BFF.

**¿Cómo se prueba una app de escritorio sin pantalla?**
Separando la capa de servicio de la interfaz. Los 42 tests de las dos apps prueban servicios: la de materiales contra un motor embebido en H2, la de comunicaciones contra un stub HTTP. Las ventanas Swing se prueban a mano, y eso queda dicho en sus READMEs.

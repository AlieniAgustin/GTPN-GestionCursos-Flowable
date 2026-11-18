# App de simulación de las comunicaciones de la empresa (§4.2.2)

App de escritorio que hace de empresa: se elige una solicitud y se le envían las comunicaciones. Todo por la **API REST de Flowable** (`/process-api`), que es lo que pide el enunciado; no usa la API Java ni el BFF.

> **Por qué tiene interfaz gráfica.** El enunciado permite hacer estas acciones por línea de comandos **solo a los grupos de hasta 3 personas**, y somos más. Así que se hizo con ventana, igual que la app de materiales.
>
> Informe de la defensa: [`docs/informe/10-apps-auxiliares.md`](../docs/informe/10-apps-auxiliares.md)

## Cómo correrla

Necesita el motor levantado (ver el [README principal](../README.md#1-levantar)) y entorno gráfico.

```bash
./build.sh
```

```bash
java -jar target/app.jar
```

`./build.sh` hace `mvn clean package -DskipTests` y deja `target/app.jar`. Hay que repetirlo después de cualquier `mvn clean`: `mvn test` compila pero no empaqueta, y sin el jar sale `Unable to access jarfile`.

No pide login: la empresa no es usuario del motor —§3.3 solo pide usuarios para el personal de la institución— así que la app usa la cuenta técnica.

## Qué hace

La tabla de arriba lista las instancias en curso con su curso, empresa y el id del curso en la BD de la institución. Al seleccionar una, abajo aparecen **solo las comunicaciones habilitadas en ese estado**:

| Comunicación               | Dato que aporta                     | Variable        |
| -------------------------- | ----------------------------------- | --------------- |
| Aceptación de la propuesta | fecha acordada de inicio del curso  | `fechaCurso`    |
| Rechazo de la propuesta    | —                                   | —               |
| Solicitud de modificaciones| observaciones                       | `observaciones` |
| Nómina de participantes    | participantes, uno por línea o separados por comas | `participantes` |
| Cancelación                | —                                   | —               |

Esperando respuesta a la propuesta salen aceptar, rechazar y pedir modificaciones; en preparación, la nómina y la cancelación; mientras corre el curso, ninguna.

Eso **no lo deduce la app con reglas propias**: por cada mensaje le pregunta al motor qué ejecución está suscripta (`GET /runtime/executions?processInstanceId=...&messageEventSubscriptionName=...`), que es la única fuente de verdad y además da el `executionId` al que hay que enviarlo. Así la app no puede quedar desincronizada del proceso.

Cada comunicación se envía con `PUT /runtime/executions/{executionId}` y `action: messageEventReceived`. La fecha se valida antes de llamar al motor, para que un formato mal escrito se vea en el momento y no como una instancia trabada en el timer.

Para tener solicitudes que mostrar: `./scripts/seed-demo.sh` desde la raíz del repositorio.

> Esta app es la forma práctica de generar instancias terminadas con recorridos variados, que es lo que necesitan el [análisis de §4.3](../materiales/README.md#análisis-de-las-ejecuciones-43) y la minería de §4.4 para dar algo interesante.

## Configuración

Precedencia: propiedad de sistema, variable de entorno y por último [`comunicaciones.properties`](src/main/resources/comunicaciones.properties).

| Propiedad             | Variable de entorno  | Default                                  |
| --------------------- | -------------------- | ---------------------------------------- |
| `flowable.base-url`   | `FLOWABLE_BASE_URL`  | `http://localhost:8080/process-api`      |
| `flowable.username`   | `FLOWABLE_USERNAME`  | `admin`                                  |
| `flowable.password`   | `FLOWABLE_PASSWORD`  | `test`                                   |

```bash
java -Dflowable.base-url=http://otra-maquina:8080/process-api -jar target/app.jar
```

## Decisiones de implementación

- **Sin Spring.** Es una app de escritorio con una sola dependencia de runtime: Jackson, para leer y armar los JSON. El cliente HTTP es el `HttpClient` del JDK. El jar completo queda en poco más de 2 MB.
- **HTTP Basic armado a mano** en la cabecera, en vez de un `Authenticator`, para que la credencial viaje en el primer pedido y no haga falta el desafío 401.
- Los errores se traducen a mensajes útiles: credenciales rechazadas, motor caído con la URL que se intentó, y el cuerpo que devolvió el motor en los 4xx/5xx.

## Tests

```bash
mvn test
```

**16 tests, 0 fallos.** Usan el servidor HTTP del JDK como stub de la API REST, así que **no necesitan el motor levantado**. Cubren el cliente (HTTP Basic, escapado de los nombres de mensaje con acento, 204 sin cuerpo, 401, 500, motor caído) y el servicio (parseo de solicitudes, comunicaciones habilitadas en cada estado, cuerpo exacto de cada mensaje y las validaciones locales).

Los nombres de los parámetros de la API REST se verificaron contra `flowable-rest-6.8.0.jar`, no de memoria.

La interfaz Swing no tiene tests automáticos: se prueba a mano.

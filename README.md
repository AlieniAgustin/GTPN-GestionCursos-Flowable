# Gestión de Cursos de Capacitación — GTPN

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Flowable](https://img.shields.io/badge/Flowable-6.8.0-009688?style=flat-square)](https://www.flowable.com/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.8+-C71A36?style=flat-square&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

Proyecto final de **Gestión y Tecnologías de Procesos de Negocio (GTPN)**: implementación en Flowable del proceso de organización de cursos de capacitación de una institución especializada.

Enunciado oficial: [`docs/ProyectoCursos_V3.pdf`](docs/ProyectoCursos_V3.pdf).

---

## El problema en dos párrafos

Las empresas solicitan cursos para capacitar a sus empleados. La institución evalúa la **viabilidad** (si ya tiene cinco o más cursos en ejecución, rechaza y finaliza), elabora una propuesta y negocia con la empresa, que puede aceptarla, rechazarla o pedir modificaciones. Si pasan cinco días sin respuesta se envía un recordatorio (máximo dos); si tampoco hay respuesta, se archiva la solicitud.

Aceptada la propuesta, se **prepara el curso** en tres ramas paralelas (inscripciones, materiales y recursos). Durante la preparación la empresa puede **cancelar**, lo que compensa los recursos ya gestionados. Si todo avanza, el jefe docente define quién dicta el curso, el docente lo desarrolla, se elabora el informe de resultados y el jefe administrativo lo envía y archiva el expediente, liberando los recursos.

---

## Contenido del repositorio

| Carpeta                  | Qué contiene                                                                                                                                                                                               |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [`model/`](model/)       | Modelos BPMN: `cursosNuestraVersion.bpmn` (refinado, 7PMG) y `cursosOriginal.bpmn` (base de cátedra), más el [informe de justificación](model/informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md) |
| [`flowable/`](flowable/) | Motor de procesos: Spring Boot 2.7.18 + Flowable 6.8.0, BPMN desplegable, DMN de viabilidad, listeners, delegates y API REST                                                                               |
| [`backend/`](backend/)   | Backend-for-Frontend (Spring Boot 3) con la BD de la institución. Ver su [README](backend/README.md)                                                                                                       |
| [`materiales/`](materiales/) | App de escritorio (Swing) para *Preparar materiales* por la **API Java** de Flowable (§4.2.1)                                                                                                          |
| [`docs/`](docs/)         | Enunciado y documentación funcional por sección del proceso                                                                                                                                                |
| [`adr/`](adr/)           | Decisiones de arquitectura (compensación, cancelación no interruptiva, aulas simuladas, [qué va a la BD y qué queda como variable](adr/datos-proceso-vs-bd.md))                                            |

---

## Mapa de servicios: qué hace cada URL

| URL                                                      | Qué es / para qué sirve                                                                                                                              | Cómo se levanta                                     |
| -------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| `http://localhost:8080`                                  | **App Flowable**: motor de procesos embebido. Es el corazón del sistema                                                                              | Docker (perfil `prod`) o Maven (perfil `dev`)       |
| `http://localhost:8080/health`                           | Healthcheck público, sin autenticación. Devuelve `UP`                                                                                                | —                                                   |
| `http://localhost:8080/groups`                           | Lista los grupos del motor. Público. Sirve para verificar que el seed corrió                                                                         | —                                                   |
| `http://localhost:8080/me`                               | Devuelve el usuario autenticado, sus grupos y roles. **Requiere HTTP Basic**                                                                         | —                                                   |
| `http://localhost:8080/process-api/**`                   | **API REST oficial de Flowable**: desplegar procesos, iniciar instancias, consultar y completar tareas. Es la que consumen las apps externas         | —                                                   |
| `http://localhost:8088/flowable-ui/`                     | **Flowable UI**: Task App (bandeja de tareas), Modeler, Admin App e IDM App (usuarios y grupos). Sirve para operar el proceso a mano durante la demo | Docker                                              |
| `localhost:5432`                                         | **PostgreSQL**: base compartida por el motor y la UI                                                                                                 | Docker                                              |
| `http://localhost:8081`                                  | **Backend BFF**: intermediario entre las apps externas y Flowable; contiene la BD de la institución (empresas y cursos)                              | Docker                                              |
| `http://localhost:8081/api/flowable/**`                  | API interna que usa el motor para volcar a la BD de la institución el estado del curso, sus datos (programa, presupuesto, fecha, informe) y el acceso de la empresa. Protegida por API key | —                           |

> [!NOTE]
> La app Flowable y Flowable UI comparten la **misma base de datos**. Por eso un usuario creado desde la UI sirve para autenticarse contra la API REST, y viceversa.

---

## Credenciales: dónde va cada contraseña

| URL / recurso                                       | Usuario              | Contraseña            | Para qué                                                                                                                                                      |
| --------------------------------------------------- | -------------------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `http://localhost:8088/flowable-ui/`                | `admin`              | `test`                | Entrar a Flowable UI (Task, Modeler, Admin, IDM)                                                                                                              |
| `http://localhost:8080/process-api/**` (HTTP Basic) | `admin`              | `test`                | Administrar el motor por API REST                                                                                                                             |
| `http://localhost:8080/**` (HTTP Basic)             | `jefeadministrativo` | `password`            | Rol: registrar/elaborar/revisar propuesta, archivar solicitud, enviar informe                                                                                 |
| idem                                                | `administrativo1`    | `password`            | Rol: lista definitiva, materiales, recursos, compensaciones                                                                                                   |
| idem                                                | `jefedocente`        | `password`            | Rol: definir docente, elaborar informe de resultados                                                                                                          |
| idem                                                | `docente1`           | `password`            | Rol: desarrollar curso                                                                                                                                        |
| idem                                                | `docente2`           | `password`            | idem                                                                                                                                                          |
| `localhost:5432` (PostgreSQL)                       | `flowable`           | `flowable`            | Base `flowable`. Definido en [`docker-compose.yml`](docker-compose.yml)                                                                                       |
| Backend → Flowable (cuenta técnica)                 | `admin`              | `test`                | `flowable.username` / `flowable.password` en [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties)         |
| `http://localhost:8081/api/flowable/**`             | header `X-API-KEY`   | `flowable-secret-key` | Autentica al motor contra el backend. Definido en [`flowable/.../application.properties`](flowable/src/main/resources/application.properties) y en el backend |
| `http://localhost:8081/api/auth/login`              | los mismos de arriba | los mismos de arriba  | El BFF valida las credenciales contra el `/me` de Flowable y abre sesión (`JSESSIONID`)                                                                       |

**De dónde sale cada uno:**

- `admin` / `test` lo crea el bootstrap del contenedor `flowable-ui` la primera vez que arranca contra una base vacía.
- Los cinco usuarios de negocio con contraseña `password` los crea el seed de [`IdentitySeeder.java`](flowable/src/main/java/com/flowable/app/config/IdentitySeeder.java), que **corre en todos los perfiles, `prod` incluido**: recién levantado el stack con Docker ya están disponibles. Para saltearlo, `institucion.seed.enabled=false`.
- Son cinco porque el enunciado (§3.3) define **Administración con 2 personas** (una es el jefe administrativo) y **Docentes con 3** (uno es el jefe del equipo).

> [!IMPORTANT]
> El seed **crea pero nunca borra**: solo da de alta lo que falta (usuarios, grupos y membresías), así que es idempotente y se puede volver a correr sin riesgo. Pero si tu base viene de antes del ajuste a cinco usuarios, va a seguir teniendo un `administrativo2` huérfano, y el listener de asignación lo va a considerar como candidato. Para limpiarla, `docker compose down -v` y volver a levantar.

---

## Comandos, en orden

### Requisitos previos

JDK 17+, Apache Maven 3.8+, Git, Docker y Docker Compose.

```bash
git clone https://github.com/AlieniAgustin/GTPN-GestionCursos-Flowable.git
```

---

### 1. Levantar

#### 1.1 Compilar el JAR (obligatorio antes de levantar)

Los `Dockerfile` **no compilan**: solo copian el fat JAR ya generado. Si salteás este paso, `docker compose up --build` falla porque `target/app.jar` no existe (está en `.gitignore`, no se versiona). Hay que hacerlo en los **dos** módulos:

```bash
cd flowable && ./build.sh
```

```bash
cd backend && ./build.sh
```

Cada script corre `mvn clean package -DskipTests` y deja el artefacto en `target/app.jar` de su módulo.

#### 1.2 Levantar los contenedores

```bash
docker compose up --build -d
```

Levanta cuatro servicios en orden de dependencia: `flowable-postgres`, `flowable-app` (espera a que Postgres esté sano), `flowable-backend` y `flowable-ui` (ambos esperan a que la app esté sana).

```bash
docker compose ps
```

#### 1.3 Verificar que el seed corrió

El seed de usuarios y grupos corre **en cada arranque de la app, en cualquier perfil**, así que no hay ningún paso manual: en cuanto `flowable-app` queda sano, los cinco usuarios de negocio, los dos grupos y la cuenta técnica `admin`/`test` ya están en la base.

```bash
curl -s http://localhost:8080/groups
```

Debe devolver `{"groups":["Administracion","Docentes"],"quantityGroups":2}`.

Y un usuario de negocio tiene que poder autenticarse:

```bash
curl -s -u administrativo1:password http://localhost:8080/me
```

En los logs del contenedor queda el detalle de lo que creó:

```bash
docker compose logs flowable-app | grep -i "Seeded\|Identity seed"
```

#### 1.4 Ver logs

```bash
docker compose logs -f flowable-app
```

---

### 2. Bajar

Detener los contenedores conservando los datos:

```bash
docker compose down
```

Detener **y borrar el volumen** de PostgreSQL (deja la base vacía: se pierden despliegues e instancias; los usuarios y grupos los vuelve a crear el seed en el próximo arranque):

```bash
docker compose down -v
```

---

### 3. Compilar

```bash
cd flowable && mvn clean compile
```

---

### 4. Testear

```bash
cd flowable && mvn test -DforkCount=0
```

Resultado esperado: **20 tests, 0 fallos** — `HappyPathTest`, `CompensationTest` (3), `ClassroomUnavailableTest`, `NoResponseTest`, `TaskAssignmentTest`, `CourseLimitExceededTest`, `ProposalRejectionTest`, `ProposalModificationsTest`, `ClosingPathsTest` (2), `CompanyAccessTest` (3), `CourseDateTest` (3), `IdentitySeederTest` y `AppTest`.

> [!WARNING]
> **Problema de entorno: el apóstrofe en la ruta.**
> Si la ruta absoluta del proyecto contiene un apóstrofe (por ejemplo `.../Computer's Science/...`), `mvn test` a secas **siempre falla** con:
>
> ```
> The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
> Process Exit Code: 2
> ```
>
> No es un problema del código ni de los tests. Maven Surefire arma el comando del JVM hijo como `/bin/sh -c cd "...Computer\'s Science..."`, y dentro de comillas dobles el `\'` queda literal, así que el `cd` apunta a una ruta inexistente y el fork muere antes de arrancar.
>
> Soluciones, de menor a mayor esfuerzo:
>
> 1. `-DforkCount=0` — corre los tests en la JVM de Maven, sin fork. Es lo que recomendamos.
> 2. Mover el repositorio a una ruta sin apóstrofes ni comillas.
>
> `./build.sh` **no** se ve afectado porque usa `-DskipTests`.

Casi todos los tests sobreescriben el datasource a H2 en memoria, pero `AppTest` levanta el contexto con la configuración real del perfil `dev`, así que **necesita el contenedor de PostgreSQL arriba** y escribe en la misma base que usa Docker: cada corrida redesplega el BPMN y suma una versión nueva del proceso. Para correr solo los tests aislados: `mvn test -DforkCount=0 -Dtest='!AppTest'`.

---

### 5. Correr la app local (perfil `dev`)

Requiere el contenedor de PostgreSQL levantado, porque [`application-dev.properties`](flowable/src/main/resources/application-dev.properties) apunta a `jdbc:postgresql://localhost:5432/flowable`.

Si el puerto 8080 está ocupado por el contenedor `flowable-app`, usá otro puerto:

```bash
cd flowable && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

Si bajaste los contenedores de la app y querés el 8080:

```bash
cd flowable && mvn spring-boot:run
```

Diferencias entre perfiles:

|                           | `dev`                       | `prod`                                                                                                     |
| ------------------------- | --------------------------- | ---------------------------------------------------------------------------------------------------------- |
| Base de datos             | PostgreSQL `localhost:5432` | PostgreSQL `postgres:5432` (sobreescribible por `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) |
| Seed de usuarios y grupos | **Sí**                      | **Sí** (desactivable con `institucion.seed.enabled=false`)                                                 |
| Consola H2                | —                           | deshabilitada                                                                                              |

---

### 6. Correr el backend BFF

```bash
cd backend && mvn spring-boot:run
```

Solo hace falta si lo querés correr fuera de Docker: `docker compose up` ya lo levanta. Arranca en `http://localhost:8081` y espera PostgreSQL en `localhost:5432` y el motor en `http://localhost:8080`.

**Autenticación.** Las rutas de negocio necesitan sesión, que se mantiene con la cookie `JSESSIONID`; sin ella, la API responde `401`.

El mismo endpoint acepta los dos tipos de usuario (§4.1): el personal de la institución, que se valida contra el IDM de Flowable, y las empresas, con la cuenta que les crea *Habilitar acceso de la empresa*. La respuesta trae el `role`, que es lo que decide qué cursos ve cada uno.

```bash
curl -s -c cookies.txt -X POST http://localhost:8081/api/auth/login -H 'Content-Type: application/json' -d '{"username":"jefeadministrativo","password":"password"}'
```

Devuelve `{"username":"jefeadministrativo","role":"INSTITUCION","authenticated":true}`. Para comprobar si la sesión sigue viva:

```bash
curl -s -b cookies.txt http://localhost:8081/api/auth/me
```

El usuario y la contraseña de una empresa salen de las variables `empresaUsuario` y `empresaPasswordTemporal` de su instancia, o del log del motor: el enunciado permite simular esa comunicación.

**Registrar una solicitud e iniciar su instancia** (§4.1.1). Devuelve `201` con el curso y el `processInstanceId` ya vinculado:

```bash
curl -s -b cookies.txt -X POST http://localhost:8081/api/courses -H 'Content-Type: application/json' -d '{"contact":{"name":"ACME SA","email":"contacto@acme.com","phone":"+543581234567"},"name":"Curso de Flowable","description":"Capacitacion en BPMN","participantCount":12,"possibleDates":["2027-03-01"]}'
```

**Consultar los cursos.** Una empresa ve solo los suyos; el personal de la institución, todos (§4.1.2). Pedir el curso de otra empresa devuelve `404`, no `403`, para no revelar que existe.

```bash
curl -s -b cookies.txt http://localhost:8081/api/courses
```

```bash
curl -s -b cookies.txt http://localhost:8081/api/courses/52
```

**Consultar la bandeja de tareas** del usuario autenticado: las que tiene asignadas más las que puede reclamar por su grupo candidato (§4.1.3).

```bash
curl -s -b cookies.txt http://localhost:8081/api/tasks
```

**Consultar el detalle de una tarea.** Devuelve el payload tipado y el `actionUrl` al que hay que postear para completarla. Si la tarea no tiene estrategia asociada, responde `404` con el motivo:

```bash
curl -s -b cookies.txt http://localhost:8081/api/tasks/{taskId}/detail
```

**Completar una tarea** usando el `actionUrl` devuelto. Por ejemplo, *Definir docente*:

```bash
curl -s -b cookies.txt -X POST http://localhost:8081/api/tasks/{taskId}/define-teacher -H 'Content-Type: application/json' -d '{"userId":"docente1"}'
```

Las otras tres acciones son `/deliver-course`, `/prepare-report` y `/send-report`.

El detalle y el completado solo funcionan sobre tareas que estén en la bandeja del usuario: una tarea de otro rol responde `404` con el motivo.

**API interna** que usa el motor para volcar datos a la BD de la institución. No lleva sesión sino API key.

Estado final del curso, al terminar la instancia:

```bash
curl -s -X PUT http://localhost:8081/api/flowable/courses/{processId} -H 'X-API-KEY: flowable-secret-key' -H 'Content-Type: application/json' -d '{"status":"FINALIZADO"}'
```

Valores válidos de `status`: `FINALIZADO`, `CANCELADO`, `RECHAZADO`, `NO_VIABLE`, `SIN_RESPUESTA`.

Datos del curso (§3.2). Todos los campos son opcionales y **solo se pisan los que llegan con valor**, porque el proceso los produce en etapas distintas:

```bash
curl -s -X PUT http://localhost:8081/api/flowable/courses/{processId}/details -H 'X-API-KEY: flowable-secret-key' -H 'Content-Type: application/json' -d '{"program":"4 modulos, 16 horas","budget":180000.00,"agreedDate":"2027-03-01","report":"11 de 12 participantes aprobaron"}'
```

Qué dato vive en la BD y cuál queda como variable del proceso está documentado en [`adr/datos-proceso-vs-bd.md`](adr/datos-proceso-vs-bd.md).

---

### 7. Probar la API REST

Healthcheck, sin autenticación:

```bash
curl -s http://localhost:8080/health
```

Identidad y roles del usuario autenticado:

```bash
curl -s -u admin:test http://localhost:8080/me
```

```bash
curl -s -u docente1:password http://localhost:8080/me
```

Procesos desplegados:

```bash
curl -s -u admin:test http://localhost:8080/process-api/repository/process-definitions
```

**Iniciar una instancia.** El proceso arranca por un evento de mensaje (`Solicitud de curso`), no por `processDefinitionKey`:

```bash
curl -s -u admin:test -X POST http://localhost:8080/process-api/runtime/process-instances -H 'Content-Type: application/json' -d '{"message":"Solicitud de curso"}'
```

La respuesta trae las variables que calculan los listeners y el DMN: `cantidadCursosActivos` y `viable`.

Consultar las tareas activas:

```bash
curl -s -u admin:test http://localhost:8080/process-api/runtime/tasks
```

Tareas de una instancia concreta:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/tasks?processInstanceId={processInstanceId}"
```

**Bandeja de un usuario**: tareas asignadas a él más las que puede reclamar por su grupo candidato. Es la consulta que usan las apps externas para armar la lista de pendientes.

```bash
curl -s -u administrativo1:password "http://localhost:8080/process-api/runtime/tasks?candidateOrAssigned=administrativo1"
```

**Reclamar** una tarea de grupo (las asignadas directamente a un usuario no se reclaman) — responde `200`:

```bash
curl -s -u administrativo1:password -X POST http://localhost:8080/process-api/runtime/tasks/{taskId} -H 'Content-Type: application/json' -d '{"action":"claim","assignee":"administrativo1"}'
```

**Completar** una tarea — responde `200`:

```bash
curl -s -u administrativo1:password -X POST http://localhost:8080/process-api/runtime/tasks/{taskId} -H 'Content-Type: application/json' -d '{"action":"complete"}'
```

**Simular las comunicaciones de la empresa** enviando mensajes a la instancia. Nombres definidos en el BPMN: `Aceptación`, `Rechazo`, `Solicitud de modificaciones`, `Nómina de participantes`, `Cancelación`, `Corrección de datos`, `Consulta de avance`.

Primero hay que averiguar el `executionId` de la actividad que está esperando ese mensaje:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/executions?processInstanceId={processInstanceId}"
```

Mientras la instancia espera respuesta, aparecen las ejecuciones `aceptacion`, `rechazo`, `modificaciones` y el temporizador `plazo5dias`. Con el id de la que corresponda — responde `204`:

```bash
curl -s -u admin:test -X PUT http://localhost:8080/process-api/runtime/executions/{executionId} -H 'Content-Type: application/json' -d '{"action":"messageEventReceived","messageName":"Aceptación"}'
```

Tras la aceptación arranca el subproceso de preparación y aparecen en paralelo las tareas `prepararmateriales` y `evaluarrecursos`, ambas sin asignar y con `administracion` como grupo candidato.

Historial de instancias terminadas (base del análisis de la sección 4.3 del enunciado):

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-process-instances?finished=true"
```

> [!TIP]
> Para probar el escenario 7 (falta de aulas), bajá `aulas.disponibles` a `1` en [`application.properties`](flowable/src/main/resources/application.properties) y recompilá.

---

### 8. Cargar datos de prueba

Da de alta empresas y solicitudes por la API del BFF —igual que lo haría el sitio web— y las deja repartidas en cuatro estados: aceptada y en preparación, rechazada, con modificaciones pendientes y recién ingresada. Sirve para tener algo que mostrar en la demo y recorridos variados para el análisis de §4.3 y la minería de §4.4.

```bash
./scripts/seed-demo.sh 8
```

El **esquema no lo crea el script**: las tablas las genera Hibernate al arrancar el BFF (`spring.jpa.hibernate.ddl-auto=update`). Para ver cómo quedó la base:

```bash
docker exec flowable-postgres psql -U flowable -d flowable -c 'select id, name, budget, agreed_date, status from courses order by id;'
```

---

### 9. Correr la app de materiales (§4.2.1)

App de escritorio, local a la intranet, que usa la **API Java** de Flowable: arma un `ProcessEngine` embebido contra la **misma base** del motor y trabaja con `IdentityService` y `TaskService`. No pasa por la API REST ni por el BFF.

```bash
cd materiales && ./build.sh
```

```bash
java -jar materiales/target/app.jar
```

Necesita el PostgreSQL del stack levantado y entorno gráfico. Si la base está en otra máquina de la intranet, o con otras credenciales:

```bash
java -Ddb.url=jdbc:postgresql://otra-maquina:5432/flowable -Ddb.username=flowable -Ddb.password=flowable -jar materiales/target/app.jar
```

También se puede por variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) o editando [`materiales.properties`](materiales/src/main/resources/materiales.properties).

**Cómo usarla:** entrá con un usuario del grupo Administración (`administrativo1` / `password`, por ejemplo). La ventana lista las tareas *Preparar materiales* asignadas y las reclamables por el grupo, con el curso y la empresa de cada instancia. **Reclamar** toma una disponible; **Completar** la cierra pidiendo un detalle opcional de los materiales, que queda en la variable `detalleMateriales`.

Para tener tareas que mostrar, dejá alguna solicitud aceptada y en preparación: `./scripts/seed-demo.sh` ya genera varias.

Dos cosas que la app **no** hace a propósito: no crea ni migra tablas (`databaseSchemaUpdate = false`, el dueño del esquema es la app del motor, y si las versiones no coinciden falla al arrancar) y no activa el async executor, así que no compite por los jobs ni por los temporizadores del motor.

```bash
cd materiales && mvn test
```

Resultado esperado: **16 tests, 0 fallos**. Levantan un motor embebido sobre H2, así que no necesitan PostgreSQL ni pantalla.

---

## Acortar los plazos para la demo (§3.5)

El enunciado pide cinco días de espera antes de cada recordatorio y una fecha de inicio de curso acordada en ejecución. Ninguno de los dos está fijo en el BPMN: el modelo usa `${plazoRespuesta}` y `${fechaCurso}`, y los valores por defecto salen de dos properties.

| Property                     | Variable de entorno           | Default | Qué controla                                                                              |
| ---------------------------- | ----------------------------- | ------- | ----------------------------------------------------------------------------------------- |
| `proceso.plazo-respuesta`    | `PROCESO_PLAZO_RESPUESTA`     | `P5D`   | Espera del gateway antes de cada recordatorio (son 2 como máximo)                          |
| `proceso.fecha-curso-offset` | `PROCESO_FECHA_CURSO_OFFSET`  | `P7D`   | Desde la aceptación hasta el inicio del curso, **solo** si la empresa no manda fecha       |

Son duraciones ISO-8601 de días, horas, minutos y segundos (`P5D`, `PT1M`, `PT30S`); los meses (`P1M`) no sirven.

Para la demo con Docker, descomentar las dos variables en [`docker-compose.yml`](docker-compose.yml) y `docker compose up -d flowable-app`:

```bash
PROCESO_PLAZO_RESPUESTA=PT1M PROCESO_FECHA_CURSO_OFFSET=PT2M mvn spring-boot:run
```

Los plazos también se pueden fijar por instancia, y eso le gana a la property: `plazoRespuesta` como variable al iniciarla, y `fechaCurso` (o `fechaAcordada`) en el mensaje `Aceptación`, que es de donde la toma la app de comunicaciones del enunciado §4.2.2:

```bash
curl -s -u admin:test -X PUT http://localhost:8080/process-api/runtime/executions/{executionId} -H 'Content-Type: application/json' -d '{"action":"messageEventReceived","messageName":"Aceptación","variables":[{"name":"fechaCurso","type":"string","value":"2026-03-01T09:00"}]}'
```

Si la aceptación no trae fecha, el motor la deriva del offset y lo deja en el log; la instancia nunca queda trabada esperando la variable.

---

## Estado de implementación vs enunciado

### Implementado

- Modelo BPMN ejecutable desplegado (`Process_Curso`), con compensación, cancelación no interruptiva y eventos de mensaje.
- **Decision task DMN** de viabilidad ([`evaluarViabilidad.dmn`](flowable/src/main/resources/dmn/evaluarViabilidad.dmn)) alimentada desde el motor por `CountActiveCoursesListener`.
- **Task Listener** `AssignLeastBusyListener`: asigna *Confeccionar lista definitiva* al administrativo con menos tareas pendientes.
- **Execution Listener** `UpdateCourseStatusListener`: notifica el estado final del curso al backend.
- Delegates `ReserveClassroomDelegate` / `ReleaseClassroomDelegate` con el **web service de aulas simulado** (`aulas.disponibles`).
- Seguridad HTTP Basic contra el IDM de Flowable, con roles derivados de los grupos.
- Seed de los cinco usuarios y los dos grupos de §3.3, idempotente y activo en todos los perfiles (`prod` incluido).
- Actividad automática **Habilitar acceso de la empresa** (`EnableCompanyAccessDelegate`): le pide al BFF la cuenta de la empresa, que se crea la primera vez y se reutiliza en las contrataciones siguientes (§4.1.2).
- **Plazos parametrizables** (§3.5): `proceso.plazo-respuesta` y `proceso.fecha-curso-offset`, sin fechas fijas en el modelo.
- **App de preparación de materiales** (§4.2.1): de escritorio, por la API Java de Flowable, con login contra el IDM, bandeja de tareas asignadas y reclamables, reclamo y completado.
- 20 tests automatizados del motor (escenarios 1 a 8, los cinco caminos de cierre de §3.4 y los plazos de §3.5) y 16 de la app de materiales.

### Pendiente

| Enunciado                                | Qué falta                                                                                                                                            |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| § 4.1 Sitio web institucional            | No desarrollado: sección pública de solicitud + acceso por credenciales para empresas y administración                                               |
| § 4.1.2 Acceso de la empresa             | La cuenta, el login y el listado de cursos propios ya funcionan por API; falta la pantalla                                                           |
| § 4.1.3                                  | Generación del PDF de la propuesta y de sus nuevas versiones                                                                                         |
| § 4.2.2 App de comunicaciones (API REST) | No desarrollada: simulación de aceptación, rechazo, modificaciones, nómina y cancelación                                                             |
| § 4.3 Análisis de ejecuciones            | No implementadas las tres consultas (resultados de solicitudes, duración de ramas paralelas, distribución de tareas)                                 |
| § 4.4 Minería de procesos                | No implementado el análisis con PM4Py (`exportar_history.py` / `analizar.py`) ni el CSV de la etapa de negociación                                   |

---

## Estructura de archivos

```text
├── docker-compose.yml                # PostgreSQL + app Flowable + backend BFF + Flowable UI
├── scripts/
│   └── seed-demo.sh                  # Datos de prueba: empresas y solicitudes variadas
├── docs/
│   └── ProyectoCursos_V3.pdf         # Enunciado vigente de la cátedra
├── model/
│   ├── cursosNuestraVersion.bpmn     # Modelo refinado (7PMG)
│   ├── cursosOriginal.bpmn           # Modelo conceptual de cátedra
│   └── informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md
├── adr/                              # Decisiones de arquitectura
├── flowable/                         # Motor de procesos
│   ├── build.sh                      # mvn clean package -DskipTests → target/app.jar
│   ├── Dockerfile                    # Imagen final: solo copia target/app.jar
│   └── src/main/
│       ├── java/com/flowable/app/
│       │   ├── App.java              # Entrypoint
│       │   ├── config/              # IdentitySeeder: usuarios y grupos (§3.3)
│       │   ├── delegates/            # Reservar / liberar aula, habilitar acceso de la empresa
│       │   ├── listeners/            # Viabilidad, asignación, estado, cierre, plazos
│       │   └── security/             # HTTP Basic contra el IDM de Flowable
│       └── resources/
│           ├── processes/Institución_de_capacitación.bpmn20.xml
│           ├── dmn/evaluarViabilidad.dmn
│           └── application*.properties
├── materiales/                       # App de escritorio de materiales (§4.2.1)
│   ├── build.sh                      # mvn clean package -DskipTests → target/app.jar
│   └── src/main/java/com/materiales/app/
│       ├── MaterialsApp.java         # Entrypoint: motor embebido + login + ventana
│       ├── engine/                   # Conexión a la BD del motor y armado del ProcessEngine
│       ├── service/                  # Login contra el IDM, bandeja, reclamo y completado
│       └── ui/                       # Swing: login, tabla de tareas y acciones
└── backend/                          # Backend-for-Frontend + BD de la institución
    ├── build.sh                      # mvn clean package -DskipTests → target/app.jar
    ├── Dockerfile                    # Imagen final: solo copia target/app.jar
    └── src/main/java/com/backend/proxy/
        ├── controller/               # Auth, cursos, tareas y la API interna del motor
        ├── entity/                   # Company y Course (§3.2)
        ├── service/                  # Alta de cursos, acceso de empresas, tareas
        └── strategy/task/            # Payload y actionUrl por tipo de tarea
```

---

## Equipo de trabajo

- **Materia:** Gestión y Tecnologías de Procesos de Negocio (GTPN)
- **Profesor:** Dr. Ariel Gonzalez
- **Integrantes:**
  - Agustin Alieni
  - Agustin Cesari
  - Damian Dalio
  - Hernan Jara
  - Francisco Natale

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
| [`docs/`](docs/)         | Enunciado y documentación funcional por sección del proceso                                                                                                                                                |
| [`adr/`](adr/)           | Decisiones de arquitectura (compensación, cancelación no interruptiva, aulas simuladas)                                                                                                                    |

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
| `http://localhost:8081/api/flowable/courses/{processId}` | Endpoint interno que el motor usa para actualizar el estado del curso. Protegido por API key                                                         | —                                                   |

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

Resultado esperado: **12 tests, 0 fallos** — `HappyPathTest`, `CompensationTest` (3), `ClassroomUnavailableTest`, `NoResponseTest`, `TaskAssignmentTest`, `CourseLimitExceededTest`, `ProposalRejectionTest`, `ProposalModificationsTest`, `IdentitySeederTest` y `AppTest`.

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

**Autenticación.** Las rutas de negocio necesitan sesión. Se abre con el usuario y contraseña de Flowable y se mantiene con la cookie `JSESSIONID`; sin ella, la API responde `401`.

```bash
curl -s -c cookies.txt -X POST http://localhost:8081/api/auth/login -H 'Content-Type: application/json' -d '{"username":"jefeadministrativo","password":"password"}'
```

```bash
curl -s -b cookies.txt http://localhost:8081/api/auth/me
```

**Registrar una solicitud e iniciar su instancia** (§4.1.1). Devuelve `201` con el curso y el `processInstanceId` ya vinculado:

```bash
curl -s -b cookies.txt -X POST http://localhost:8081/api/courses -H 'Content-Type: application/json' -d '{"contact":{"name":"ACME SA","email":"contacto@acme.com","phone":"+543581234567"},"name":"Curso de Flowable","description":"Capacitacion en BPMN","participantCount":12,"possibleDates":["2027-03-01"]}'
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

**Endpoint interno** que usa el motor para avisar el estado final del curso. No lleva sesión sino API key:

```bash
curl -s -X PUT http://localhost:8081/api/flowable/courses/{processId} -H 'X-API-KEY: flowable-secret-key' -H 'Content-Type: application/json' -d '{"status":"FINALIZADO"}'
```

Valores válidos de `status`: `FINALIZADO`, `CANCELADO`, `RECHAZADO`, `NO_VIABLE`, `SIN_RESPUESTA`.

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

## Estado de implementación vs enunciado

### Implementado

- Modelo BPMN ejecutable desplegado (`Process_Curso`), con compensación, cancelación no interruptiva y eventos de mensaje.
- **Decision task DMN** de viabilidad ([`evaluarViabilidad.dmn`](flowable/src/main/resources/dmn/evaluarViabilidad.dmn)) alimentada desde el motor por `CountActiveCoursesListener`.
- **Task Listener** `AssignLeastBusyListener`: asigna *Confeccionar lista definitiva* al administrativo con menos tareas pendientes.
- **Execution Listener** `UpdateCourseStatusListener`: notifica el estado final del curso al backend.
- Delegates `ReserveClassroomDelegate` / `ReleaseClassroomDelegate` con el **web service de aulas simulado** (`aulas.disponibles`).
- Seguridad HTTP Basic contra el IDM de Flowable, con roles derivados de los grupos.
- Seed de los cinco usuarios y los dos grupos de §3.3, idempotente y activo en todos los perfiles (`prod` incluido).
- 12 tests automatizados que cubren los escenarios 1 a 8 de la defensa.

### Pendiente

| Enunciado                                | Qué falta                                                                                                                                            |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| § 3.2 BD de la institución               | Existen las entidades `Company` y `Course` en el backend, falta cablear el alta de solicitudes y el vínculo con la instancia de proceso              |
| § 4.1 Sitio web institucional            | No desarrollado: sección pública de solicitud + acceso por credenciales para empresas y administración                                               |
| § 4.1.2 Acceso de la empresa             | Falta la actividad automática *Habilitar acceso de la empresa* que crea/reutiliza la cuenta                                                          |
| § 4.1.3                                  | Generación del PDF de la propuesta y de sus nuevas versiones                                                                                         |
| § 4.2.1 App Java (API Java)              | No desarrollada: app de escritorio para *Preparar materiales* usando la API Java de Flowable                                                         |
| § 4.2.2 App de comunicaciones (API REST) | No desarrollada: simulación de aceptación, rechazo, modificaciones, nómina y cancelación                                                             |
| § 4.3 Análisis de ejecuciones            | No implementadas las tres consultas (resultados de solicitudes, duración de ramas paralelas, distribución de tareas)                                 |
| § 4.4 Minería de procesos                | No implementado el análisis con PM4Py (`exportar_history.py` / `analizar.py`) ni el CSV de la etapa de negociación                                   |
| Backend BFF                              | Faltan tests (el módulo no tiene ninguno) y los endpoints de consulta para empresas y administrativos                                                |
| § 3.5 Temporizadores                     | Nadie setea `fechaCurso` en el flujo real: completar *Definir docente* traba la instancia. Ver detalle en [`TODO.md`](TODO.md)                       |

---

## Estructura de archivos

```text
├── docker-compose.yml                # PostgreSQL + app Flowable + Flowable UI
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
│       │   ├── delegates/            # Reservar / liberar aula
│       │   ├── listeners/            # Viabilidad, asignación, estado, cierre
│       │   └── security/             # HTTP Basic contra el IDM de Flowable
│       └── resources/
│           ├── processes/Institución_de_capacitación.bpmn20.xml
│           ├── dmn/evaluarViabilidad.dmn
│           └── application*.properties
└── backend/                          # Backend-for-Frontend + BD de la institución
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

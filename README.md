# Gestión de Cursos de Capacitación

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Flowable](https://img.shields.io/badge/Flowable-6.8.0-009688?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)

Proceso de negocio de una institución de capacitación, implementado en [Flowable](https://www.flowable.com/): desde la solicitud de una empresa hasta el archivo del expediente. Proyecto final de **Gestión y Tecnologías de Procesos de Negocio (GTPN)**.

[Proceso](#el-proceso) · [Inicio rápido](#inicio-rápido) · [Apps de escritorio](#apps-de-escritorio) · [Configuración](#configuración) · [Estructura](#estructura)

Enunciado: [`docs/ProyectoCursos_V3.pdf`](docs/ProyectoCursos_V3.pdf)

## El proceso

1. **Solicitud y viabilidad.** Una empresa pide un curso. Un DMN evalúa la viabilidad: con 5 o más cursos en ejecución se rechaza.
2. **Negociación.** La institución elabora una propuesta y la empresa la acepta, rechaza o pide modificaciones. Sin respuesta en 5 días se envía un recordatorio (máx. 2) y luego se archiva.
3. **Preparación.** Tres ramas en paralelo: inscripciones, materiales y recursos. Si la empresa cancela, se compensan los recursos ya gestionados.
4. **Ejecución y cierre.** Se define el docente, se dicta el curso, se elabora el informe y se archiva el expediente liberando recursos.

## Arquitectura y Dónde Vive Cada Servicio

| Servicio / Componente | Directorio / Origen | Contenedor Docker | Puerto Host / URL | Red Interna Compose | Descripción |
| --- | --- | --- | --- | --- | --- |
| **Backend Proxy (BFF)** | [`backend/`](backend/) | `flowable-backend` | `localhost:8081` | `backend:8081` | BFF en Spring Boot 3 con la BD de la institución, API REST, sitio web y Swagger en `/swagger-ui.html`. |
| **Flowable Engine** | [`flowable/`](flowable/) | `flowable-app` | `localhost:8080` | `flowable-app:8080` | Motor de procesos (Spring Boot + Flowable): BPMN, DMN, listeners, delegates y API REST. |
| **Flowable UI** | Imagen `flowable/flowable-ui` | `flowable-ui` | `localhost:8088/flowable-ui/` | `flowable-ui:8080` | Task App, Modeler, Admin e IDM. |
| **PostgreSQL** | Imagen `postgres:15-alpine` | `flowable-postgres` | `localhost:5432` | `postgres:5432` | Base de datos relacional compartida (`flowable` / `flowable`). |
| **App de Materiales** | [`materiales/`](materiales/) | Proceso Java Swing | — | — | App Swing para preparar materiales, por la API Java. |
| **App de Comunicaciones** | [`comunicaciones/`](comunicaciones/) | Proceso Java Swing | — | — | App Swing que simula a la empresa, por la API REST. |
| **Minería de Procesos** | [`mineria/`](mineria/) | Scripts Python / PM4Py | — | — | Minería de procesos con PM4Py sobre la negociación. |
| **Documentación Técnica** | [`backend/docs/`](backend/docs/) | MkDocs local | `127.0.0.1:8000` | — | Documentación técnica y arquitectura construida con MkDocs Material (`cd backend && mkdocs serve`). |

Más detalle en [`docs/`](docs/), [`adr/`](adr/), [`model/`](model/) y la [documentación del backend](backend/docs/). Los endpoints del BFF están en `localhost:8081/swagger-ui.html`.

## Inicio rápido

**Requisitos:** JDK 17+, Maven 3.8+, Docker y Docker Compose.

```bash
git clone https://github.com/AlieniAgustin/GTPN-GestionCursos-Flowable.git
cd GTPN-GestionCursos-Flowable

# Build padre: compila ambos módulos y levanta todo el stack con Docker Compose
./build.sh
```

> [!TIP]
> **Build Padre (`./build.sh`):**
> Se encarga de compilar en orden `flowable` y `backend`, verificar que existan sus artefactos `target/app.jar` y levantar el stack con `docker compose up --build -d`.
> - Solo compilar sin levantar Docker: `./build.sh -b`
> - Compilar, levantar y cargar datos de prueba: `./build.sh -s`
> - Detener el stack: `./build.sh -d`

Verificar que el motor y el seed están listos:

```bash
curl -s http://localhost:8080/groups
curl -s -u administrativo1:password http://localhost:8080/me
```

Cargar datos de prueba (empresas y solicitudes en distintos estados):

```bash
./scripts/seed-demo.sh 8
```

Para bajar el stack: `docker compose down` (conserva datos) o `docker compose down -v` (borra la base).

### Credenciales

| Recurso | Usuario | Contraseña |
| --- | --- | --- |
| Flowable UI y `/process-api` | `admin` | `test` |
| Administración: `jefeadministrativo`, `administrativo1` | — | `password` |
| Docentes: `jefedocente`, `docente1`, `docente2` | — | `password` |
| PostgreSQL (base `flowable`) | `flowable` | `flowable` |
| API interna `/api/flowable/**` | header `X-API-KEY` | `flowable-secret-key` |

> [!NOTE]
> `IdentitySeeder` crea usuarios y grupos en cada arranque, en todos los perfiles, pero nunca borra. Si tu base es anterior y tiene un `administrativo2` huérfano, ejecutá `docker compose down -v`.

## Uso

Iniciar una instancia (el proceso arranca por mensaje, no por `processDefinitionKey`):

```bash
curl -s -u admin:test -X POST http://localhost:8080/process-api/runtime/process-instances \
  -H 'Content-Type: application/json' -d '{"message":"Solicitud de curso"}'
```

Simular a la empresa enviando un mensaje a la ejecución que lo espera (`GET /process-api/runtime/executions?processInstanceId=...` para obtener el id):

```bash
curl -s -u admin:test -X PUT http://localhost:8080/process-api/runtime/executions/{executionId} \
  -H 'Content-Type: application/json' -d '{"action":"messageEventReceived","messageName":"Aceptación"}'
```

Mensajes del BPMN: `Aceptación`, `Rechazo`, `Solicitud de modificaciones`, `Nómina de participantes`, `Cancelación`, `Corrección de datos`, `Consulta de avance`.

En la práctica es más cómodo usar el sitio (`localhost:8081`), Flowable UI o la app de comunicaciones.

## Apps de escritorio

Necesitan el stack levantado y entorno gráfico.

```bash
# Preparar materiales (API Java): entrar con administrativo1 / password
(cd materiales && ./build.sh) && java -jar materiales/target/app.jar

# Comunicaciones de la empresa (API REST)
(cd comunicaciones && ./build.sh) && java -jar comunicaciones/target/app.jar

# Análisis de ejecuciones (§4.3); también es un botón en la app de materiales
java -cp materiales/target/app.jar com.materiales.app.analysis.AnalysisApp
```

> [!TIP]
> El análisis sale casi vacío con datos recién sembrados. Aceptá, rechazá y cancelá solicitudes desde la app de comunicaciones para generar instancias terminadas.

## Configuración

### Plazos

| Property | Variable de entorno | Default | Controla |
| --- | --- | --- | --- |
| `proceso.plazo-respuesta` | `PROCESO_PLAZO_RESPUESTA` | `P5D` | Espera antes de cada recordatorio |
| `proceso.fecha-curso-offset` | `PROCESO_FECHA_CURSO_OFFSET` | `P7D` | Aceptación → inicio del curso, si la empresa no manda fecha |

Duraciones ISO-8601 (`P5D`, `PT1M`); los meses no sirven. Para acortarlas en la demo, descomentá las variables en [`docker-compose.yml`](docker-compose.yml) y ejecutá `docker compose up -d flowable-app`. También se pueden fijar por instancia con las variables `plazoRespuesta` y `fechaCurso`.

Para simular falta de aulas, bajá `aulas.disponibles` a `1` (`AULAS_DISPONIBLES` en Docker).

### Desarrollo

```bash
cd flowable && mvn spring-boot:run                       # perfil dev, requiere PostgreSQL
cd flowable && mvn test -DforkCount=0                    # AppTest necesita PostgreSQL
cd flowable && mvn test -DforkCount=0 -Dtest='!AppTest'  # solo tests con H2
cd backend  && mvn spring-boot:run
```

## Estructura

```text
├── flowable/          Motor de procesos
├── backend/           BFF y BD de la institución
├── materiales/        App de escritorio (API Java)
├── comunicaciones/    App de escritorio (API REST)
├── mineria/           Minería de procesos
├── model/             Modelos BPMN
├── adr/               Decisiones de arquitectura
├── docs/              Enunciado e informe
└── scripts/           Datos y preparación de la demo
```

> [!NOTE]
> Pendiente (opcional, §1.1): el límite de dos recordatorios está en un script y no en el flujo del modelo. Ver [`mineria/conclusion.md`](mineria/conclusion.md).

## Equipo

Materia GTPN, profesor Dr. Ariel Gonzalez.

Agustin Alieni, Agustin Cesari, Damian Dalio, Hernan Jara y Francisco Natale.
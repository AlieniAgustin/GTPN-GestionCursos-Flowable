# Setup y Ejecución del Backend

Esta guía detalla los pasos para configurar el entorno local, compilar y ejecutar el módulo `backend`.

---

## Requisitos Previos

- **Java JDK 21** o superior.
- **Apache Maven 3.9+** (o el wrapper `./mvnw` si está disponible).
- **Docker y Docker Compose** (para levantar PostgreSQL y Flowable).
- **Python 3.10+ y MkDocs** (para consultar o editar esta documentación).

---

## Dónde Vive Cada Servicio (Topología del Ecosistema)

El sistema se compone de varios servicios distribuidos entre contenedores Docker, aplicaciones de escritorio y herramientas de soporte:

| Servicio | Directorio en el Repositorio | Contenedor Docker | URL / Puerto en Host | Resolución Interna (Compose) | Responsabilidad |
|---|---|---|---|---|---|
| **Backend Proxy (BFF)** | [`backend/`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/) | `flowable-backend` | [http://localhost:8081](http://localhost:8081) | `http://backend:8081` | Spring Boot 3 BFF, API REST, portal web Thymeleaf y Swagger UI en `/swagger-ui.html`. |
| **Flowable Engine** | [`flowable/`](file:///home/francisco/GTPN-GestionCursos-Flowable/flowable/) | `flowable-app` | [http://localhost:8080](http://localhost:8080) | `http://flowable-app:8080` | Motor BPMN 2.0 y DMN (Spring Boot 2.7 + Flowable 6.8), listeners y API REST. |
| **Flowable UI** | Imagen oficial `flowable/flowable-ui` | `flowable-ui` | [http://localhost:8088/flowable-ui/](http://localhost:8088/flowable-ui/) | `http://flowable-ui:8080` | Panel web para administración, modelador BPMN y consulta de IDM / tareas. |
| **PostgreSQL** | Imagen oficial `postgres:15-alpine` | `flowable-postgres` | `localhost:5432` | `postgres:5432` | Base relacional compartida (`db: flowable`, `user: flowable`). Persistida en volumen `postgres_data`. |
| **Documentación MkDocs** | [`backend/docs/`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/docs/) | Servidor local MkDocs | [http://127.0.0.1:8000](http://127.0.0.1:8000) | — | Sitio de documentación técnica y arquitectura del backend. |
| **App de Materiales** | [`materiales/`](file:///home/francisco/GTPN-GestionCursos-Flowable/materiales/) | Proceso Java Swing | — (acceso directo a Java/BD) | — | Aplicación de escritorio para la preparación y registro de materiales. |
| **App de Comunicaciones**| [`comunicaciones/`](file:///home/francisco/GTPN-GestionCursos-Flowable/comunicaciones/) | Proceso Java Swing | — (consume HTTP `:8080` y `:8081`) | — | Simulador de interacción de la empresa para responder propuestas y enviar nóminas. |
| **Minería de Procesos** | [`mineria/`](file:///home/francisco/GTPN-GestionCursos-Flowable/mineria/) | Scripts Python / PM4Py | — | — | Análisis de trazas de eventos del proceso de negociación (§4.4). |

---

## Paso 1: Infraestructura de Base de Datos y Flowable

Desde la raíz del repositorio, inicia los servicios de soporte:

```bash
# Levantar PostgreSQL y Flowable Engine
docker compose up -d postgres flowable
```

Verifica que PostgreSQL esté accesible en el puerto `5432` y Flowable en el puerto `8080`.

---

## Paso 2: Variables de Entorno y Configuración

El archivo `application.properties` viene preconfigurado con valores por defecto para desarrollo local:

| Variable | Valor por Defecto | Descripción |
|---|---|---|
| `server.port` | `8081` | Puerto HTTP del backend. |
| `DB_HOST` | `localhost` | Host de PostgreSQL. |
| `DB_PORT` | `5432` | Puerto de PostgreSQL. |
| `DB_NAME` | `flowable` | Nombre de la base de datos. |
| `DB_USER` | `flowable` | Usuario de la base de datos. |
| `DB_PASSWORD` | `flowable` | Contraseña de la base de datos. |
| `flowable.base-url` | `http://localhost:8080` | URL base del motor Flowable REST. |
| `flowable.username` | `admin` | Usuario de la cuenta técnica. |
| `flowable.password` | `test` | Contraseña de la cuenta técnica. |
| `flowable.api-key` | `flowable-secret-key` | Secreto para proteger `/api/flowable/**`. |

---

## Paso 3: Compilación y Ejecución

### Opción A: Orquestación Total con Build Padre (Recomendado)
Desde la raíz del proyecto, el script `./build.sh` compila ambos módulos (`flowable` y `backend`) y levanta todo el stack con Docker Compose:

```bash
# Compilar todo y levantar los contenedores:
./build.sh

# O compilar, levantar y poblar datos de prueba:
./build.sh -s
```

### Opción B: Modo Desarrollo con Maven (Solo Backend)
```bash
cd backend
mvn spring-boot:run
```

### Opción C: Compilación Local de Backend a Jar
```bash
cd backend
./build.sh
# El script compila y genera target/app.jar
java -jar target/app.jar
```

---

## Paso 4: Acceso a la Interfaz de Usuario (UI) y Verificación

Con el backend iniciado (puerto `8081`):

### 1. Portal Público de Solicitudes (Sin Login)
- **URL:** [http://localhost:8081/](http://localhost:8081/)
- **Uso:** Cualquier empresa o cliente puede solicitar una capacitación completando el formulario público (§4.1.1). Al enviarlo, se guarda el curso en la base de datos y se dispara el proceso BPMN en Flowable.

### 2. Login Web Institucional y Empresas
- **URL de Acceso:** [http://localhost:8081/login](http://localhost:8081/login)
- **Credenciales para ingresar:**

| Tipo de Usuario | Usuarios de Prueba | Contraseña | Destino post-login |
|---|---|---|---|
| **Administración** | `jefeadministrativo`, `administrativo1` | `password` | Redirige automáticamente a la bandeja de tareas ([`/tareas`](http://localhost:8081/tareas)) para elaborar/revisar propuestas y recursos. |
| **Cuerpo Docente** | `jefedocente`, `docente1`, `docente2` | `password` | Redirige a la bandeja ([`/tareas`](http://localhost:8081/tareas)) para definir docentes y dictar cursos. |
| **Empresas Clientes** | Cuentas generadas automáticamente (ej. con `./build.sh -s` o `./scripts/seed-demo.sh 8`) | Contraseña de empresa | Redirige automáticamente a su portal de seguimiento ([`/mis-cursos`](http://localhost:8081/mis-cursos)). |

### 3. Exploración Interactiva de la API
- **Swagger UI:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Health check:** [http://localhost:8081/health](http://localhost:8081/health) (Respuesta esperada: `{"status":"UP"}`)


---

## Próximo Paso

- Consulta cómo ejecutar y estructurar pruebas en [Pruebas y Testing](testing.md).

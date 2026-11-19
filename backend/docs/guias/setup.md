# Setup y Ejecución del Backend

Esta guía detalla los pasos para configurar el entorno local, compilar y ejecutar el módulo `backend`.

---

## Requisitos Previos

- **Java JDK 21** o superior.
- **Apache Maven 3.9+** (o el wrapper `./mvnw` si está disponible).
- **Docker y Docker Compose** (para levantar PostgreSQL y Flowable).
- **Python 3.10+ y MkDocs** (para consultar o editar esta documentación).

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

### Opción A: Modo Desarrollo con Maven
```bash
cd backend
mvn spring-boot:run
```

### Opción B: Script de Compilación a Jar
```bash
cd backend
./build.sh
# El script compila y genera target/app.jar
java -jar target/app.jar
```

---

## Paso 4: Verificación

Una vez que el servicio esté arriba:

1. **Health Check:**
   ```bash
   curl http://localhost:8081/health
   # Respuesta esperada: {"status":"UP"}
   ```

2. **Swagger UI:**
   Abre en el navegador [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) para explorar los endpoints disponibles.

3. **Portal Web:**
   Ingresa a [http://localhost:8081/](http://localhost:8081/) para ver el sitio web Thymeleaf.

---

## Próximo Paso

- Consulta cómo ejecutar y estructurar pruebas en [Pruebas y Testing](testing.md).

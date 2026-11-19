# Swagger y OpenAPI

El backend integra **SpringDoc OpenAPI 3** (`springdoc-openapi-starter-webmvc-ui` versión 2.5.0), lo que permite explorar, probar y validar interactivamente todos los endpoints de la API REST directamente desde el navegador.

---

## Acceso Rápido

Con la aplicación en ejecución en el puerto 8081:

| Recurso | URL | Acceso |
|---|---|---|
| **Swagger UI (Interfaz Web)** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | Pública (sin login) |
| **Especificación OpenAPI (JSON)** | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) | Pública (sin login) |

---

## Configuración Técnica

La configuración reside en [`OpenApiConfig`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/src/main/java/com/backend/proxy/config/OpenApiConfig.java) y en `application.properties`:

```properties
springdoc.packages-to-scan=com.backend.proxy.controller
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=alpha
springdoc.swagger-ui.tags-sorter=alpha
```

### Exclusión en Spring Security
En [`SecurityConfig`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/src/main/java/com/backend/proxy/security/SecurityConfig.java), las rutas de Swagger están declaradas en `PUBLIC_URLS`:
```java
"/v3/api-docs/**",
"/swagger-ui/**",
"/swagger-ui.html"
```
Esto permite a desarrolladores y herramientas de CI/CD inspeccionar los esquemas sin requerir autenticación previa.

---

## Tags y Agrupación de Operaciones

Los endpoints se organizan por controladores mediante anotaciones `@Tag` y `@Operation`:

- **Cursos:** Operaciones sobre solicitudes de cursos (`POST /api/courses`, `GET /api/courses`, `GET /api/courses/{id}`).
- **Tareas:** Bandeja de entrada, consulta de detalles HATEOAS y completitud de acciones tipadas (`/api/tasks/**`).
- **Flowable Interno:** Callbacks y webhooks para sincronización desde tareas de servicio del motor BPMN (`/api/flowable/**`).

---

## Próximo Paso

- Conoce el esquema de datos y persistencia en [Entidades y Persistencia](../modelo/entidades.md).

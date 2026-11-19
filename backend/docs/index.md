# Backend Proxy: Documentación Técnica

El módulo **`backend`** es el servicio intermediario (*Backend-for-Frontend* / BFF) del sistema de Gestión de Cursos con Flowable BPM. Centraliza la autenticación, gestiona la persistencia de cursos y empresas, y orquesta las interacciones con el motor de procesos Flowable mediante una cuenta técnica unificada y un patrón desacoplado de resolución de tareas con HATEOAS.

---

## Inicio Rápido (Quick Path)

Para ejecutar el backend y explorar la documentación interactiva:

1. **Asegurar base de datos activa:**
   ```bash
   docker compose up -d postgres
   ```

2. **Compilar y levantar la aplicación (puerto 8081):**
   ```bash
   ./mvnw spring-boot:run
   # O alternativamente con Maven del sistema:
   mvn spring-boot:run
   ```

3. **Verificar el servicio:**
   - Health check: [http://localhost:8081/health](http://localhost:8081/health)
   - Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
   - Portal web / Thymeleaf: [http://localhost:8081/](http://localhost:8081/)

---

## Mapa de la Documentación

| Sección | Descripción | Contenido Clave |
|---|---|---|
| [**Arquitectura**](arquitectura/overview.md) | Principios de diseño y topología del backend | Rol BFF, cuenta técnica única, modelo de autenticación dual y patrón Strategy para tareas. |
| [**API y Endpoints**](api/controladores.md) | Catálogo de APIs REST expuestas | Endpoints de cursos, tareas y callbacks de Flowable (`/api/courses`, `/api/tasks`, `/api/flowable`). |
| [**Manejo de Errores**](api/errores.md) | Contrato unificado de respuestas de error | `@RestControllerAdvice`, `ApiErrorResponse` y mapeo de excepciones de integración. |
| [**Modelo de Datos**](modelo/entidades.md) | Persistencia relacional en PostgreSQL | Entidades JPA (`Course`, `Company`, `CompanyAccess`, `ProposalDocument`) y delimitación contra variables de proceso BPMN. |
| [**Guías de Desarrollo**](guias/setup.md) | Instrucciones paso a paso para desarrolladores | Variables de entorno, ejecución de tests reales con `@DataJpaTest` y creación de nuevas tareas BPMN. |

---

## Cómo Servir Esta Documentación

Para ver y editar esta documentación en tiempo real con recarga automática:

```bash
# Desde la carpeta backend:
mkdocs serve

# O desde la raíz del repositorio:
mkdocs serve -f backend/mkdocs.yml
```

Luego abre tu navegador en [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

---

## Referencias de Arquitectura

- [Microsoft Learn: Patrón Backends for Frontends (BFF)](https://learn.microsoft.com/es-es/azure/architecture/patterns/backends-for-frontends)
- [Mentores Tech: ¿Qué es HATEOAS y cuándo aplicarlo?](https://www.mentorestech.com/resource-blog-content/que-es-hateoas-y-cuando-aplicarlo)


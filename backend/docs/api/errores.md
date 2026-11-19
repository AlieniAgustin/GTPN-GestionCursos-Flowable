# Manejo Centralizado de Errores

El backend implementa un mecanismo unificado de captura y normalización de excepciones mediante `@RestControllerAdvice`, garantizando que tanto los errores de negocio como los fallos de comunicación con Flowable produzcan respuestas estructuradas y predecibles.

---

## Contrato de Error: `ApiErrorResponse`

Todas las respuestas de error en la API REST siguen el contrato inmutable del record [`ApiErrorResponse`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/src/main/java/com/backend/proxy/exception/ApiErrorResponse.java):

```json
{
  "timestamp": "2026-11-18T20:30:00.123456Z",
  "error": "FLOWABLE_INTEGRATION_ERROR",
  "message": "Error in communication with flowable: Not Found",
  "path": "/api/tasks/8020/detail"
}
```

### Campos del Payload
- **`timestamp`**: Marca de tiempo UTC (`Instant`) en que ocurrió el error.
- **`error`**: Código semántico de error en mayúsculas (ej. `FLOWABLE_INTEGRATION_ERROR`, `RESOURCE_NOT_FOUND`).
- **`message`**: Explicación legible del motivo del fallo.
- **`path`**: URI de la petición que generó la excepción.

---

## Manejador Global: `GlobalExceptionHandler`

La clase [`GlobalExceptionHandler`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/src/main/java/com/backend/proxy/exception/GlobalExceptionHandler.java) intercepta excepciones lanzadas durante el procesamiento:

### 1. Errores de Integración con Flowable (`RestClientResponseException`)
Cuando el motor Flowable responde con un código de error HTTP (4xx o 5xx):
- El interceptor preserva el código HTTP original devuelto por Flowable (ej. 404 si la tarea no existe, 400 si faltan variables).
- Transforma el cuerpo de respuesta en un `ApiErrorResponse` con código `FLOWABLE_INTEGRATION_ERROR`.
- Evita que detalles internos del stacktrace de Flowable se filtren al frontend.

### 2. Recursos no Encontrados (`ResourceNotFoundException`)
Lanzada cuando se consulta una entidad inexistente en la base de datos (por ejemplo, un curso por ID que no existe):
- Retorna código HTTP `404 Not Found`.

### 3. Excepciones de Estado y Concurrencia
- `TaskAlreadyClaimedException`: Lanzada cuando dos usuarios intentan tomar la misma tarea simultáneamente.
- `TaskNotClaimedException`: Cuando se intenta completar una tarea sin antes haberla reclamado.
- `InvalidCourseStateException`: Si se intenta transicionar un curso en un estado incompatible.

---

## Próximo Paso

- Explora la interfaz interactiva de la API en [Swagger y OpenAPI](swagger.md).

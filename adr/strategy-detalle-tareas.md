# ADR: Patrón Strategy y Enlaces HATEOAS para Consulta y Resolución de Tareas (BFF)

- **Estado:** Aceptado
- **Fecha:** 2026-11-09
- **Contexto:** `backend/` (Backend Proxy / BFF), aplicaciones cliente (Web / Mobile) y motor Flowable
- **Relacionado:** 
  - [ADR: Arquitectura de Seguridad Stateless y Backend Proxy con Cuenta Técnica](arquitecture.md)
  - [Docs: Desarrollo y Finalización](../docs/desarrollo-finalizacion.md)

---

## 1. Contexto y Problema

Las tareas de usuario (`userTask`) en Flowable representan pantallas interactivas para distintos roles (jefe docente, administrativos, docentes). Cada tarea posee requerimientos asimétricos:
* **Lectura:** Contexto de negocio y catálogos institucionales vivos para que el usuario tome una decisión (ej. lista de docentes disponibles, versiones de propuestas).
* **Escritura:** Respuestas con contratos específicos y validaciones estrictas antes de avanzar el proceso.

### Problemas Detectados
1. **Chatty I/O (N+1 de red):** Si la UI consulta la tarea y luego efectúa peticiones adicionales para obtener catálogos o decodificar variables genéricas (`List<FlowableVariable>`), degrada la performance en clientes web y móviles.
2. **Acoplamiento de URLs en el Frontend:** Si el cliente debe mantener lógica condicional (`if task == 'X' POST urlA else POST urlB`), cualquier cambio de flujo rompe la UI.
3. **Pérdida de Type-Safety:** Un único endpoint genérico de completitud (`POST /api/tasks/{taskId}/complete`) que reciba JSON arbitrario impide la validación automática de Spring (`@Valid`), oculta la documentación en OpenAPI/Swagger y traslada errores de tipos al runtime de Flowable.

---

## 2. Decisión

Se adopta una arquitectura combinada de **Strategy** para la agregación de datos con **Hipermedios (HATEOAS)** para el desacople de rutas, complementada por **Endpoints de Acción Tipados** para la resolución.

```text
[Frontend / Mobile]
         │
         │  1. GET /api/tasks/{taskId}/detail
         ▼
[Backend Proxy (BFF)]
   TaskService ──► Busca TaskStrategy según taskDefinitionKey
                   ├── Consulta variables a Flowable
                   ├── Consulta catálogo/repositorio institucional
                   └── Construye TaskDetailResponse con 'actionUrl' dinámica
         │
         │  2. Payload: { taskId, actionUrl, data: { ... } }
         ▼
[Frontend / Mobile]
   Renderiza formulario con 'data'
   Envía submit directo a 'actionUrl'
         │
         │  3. POST {actionUrl}  (ej. /api/tasks/{id}/definir-docente)
         ▼
[Backend Controller (Tipado)]
   Valida con @Valid Bean Validation nativo (Fail-Fast: 400 Bad Request si es inválido)
   Mapea DTO tipado a variables de Flowable
   Ejecuta complete() en el motor
         │
         ▼
[Flowable Engine]
   Avanza el token de forma consistente
```

### Componentes de la Solución

#### 1. Consulta Agregada e Hipermedios (Query)
El cliente invoca `GET /api/tasks/{taskId}/detail`. El backend despacha a la estrategia correspondiente:

```java
public record TaskDetailResponse<T>(
    String taskId,
    String taskName,
    String actionUrl,
    T data
) {}

public interface TaskStrategy {
    boolean supports(String taskDefinitionKey);
    TaskDetailResponse<?> buildDetail(FlowableTask task, Map<String, Object> processVariables);
}
```

Implementación de ejemplo (`DefinirDocenteStrategy`):
```java
@Component
public class DefinirDocenteStrategy implements TaskStrategy {

    private final DocenteRepository docenteRepository;

    public DefinirDocenteStrategy(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    @Override
    public boolean supports(String taskKey) {
        return "definirdocentequedesarrollaelcurso".equals(taskKey);
    }

    @Override
    public TaskDetailResponse<DefinirDocenteDataDTO> buildDetail(FlowableTask task, Map<String, Object> vars) {
        var docentes = docenteRepository.findDocentesHabilitados();
        return new TaskDetailResponse<>(
            task.getId(),
            task.getName(),
            "/api/tasks/" + task.getId() + "/definir-docente",
            new DefinirDocenteDataDTO(vars.get("nombreCurso").toString(), docentes)
        );
    }
}
```

#### 2. Endpoints de Acción Tipados (Command)
En lugar de un endpoint genérico de completitud, cada acción se expone con su DTO explícito y validaciones nativas:

```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{taskId}/detail")
    public ResponseEntity<TaskDetailResponse<?>> getDetail(@PathVariable String taskId) {
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    @PostMapping("/{taskId}/definir-docente")
    public ResponseEntity<Void> completarDefinirDocente(
            @PathVariable String taskId,
            @Valid @RequestBody DefinirDocenteRequestDTO dto) {

        taskService.complete(taskId, Map.of("docenteSeleccionado", dto.userId()));
        return ResponseEntity.noContent().build();
    }
}
```

---

## 3. Consecuencias y Tradeoffs

* **Ventajas (+):**
  * **1 sola petición de lectura:** La UI recibe el contexto completo del curso, catálogos y la URL de respuesta en un único viaje de red.
  * **Desacople en Frontend (HATEOAS):** El frontend no conoce URLs de antemano; envía el formulario a la `actionUrl` informada en el payload.
  * **Validación Nativa y Fail-Fast:** Spring valida con `@Valid` antes de tocar el servicio o el motor. Si falta un campo requerido, devuelve `400 Bad Request` de inmediato, evitando corromper el estado de la instancia en Flowable.
  * **OpenAPI / Swagger Autodocumentado:** Cada acción expone su esquema de entrada fuertemente tipado.
  * **Extensibilidad (OCP):** Agregar una nueva tarea solo requiere crear su `TaskStrategy` y su endpoint de acción en el controller.
* **Tradeoffs (-):**
  * Requiere definir un método de acción por cada tipo de tarea en el controller en lugar de un único endpoint genérico. Se asume con gusto por la seguridad de tipos, claridad semántica y validación declarativa.

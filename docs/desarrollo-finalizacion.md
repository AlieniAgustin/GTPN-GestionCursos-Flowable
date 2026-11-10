# Desarrollo y Finalización

Documento de diseño y flujo operativo para la asignación docente, dictado, cierre administrativo y compensación final de recursos.

> **Decisión de Arquitectura:** La agregación de contexto, enlaces HATEOAS y validación tipada para resolver tareas se documenta en el [ADR: Patrón Strategy y Enlaces HATEOAS para Consulta y Resolución de Tareas (BFF)](../adr/strategy-detalle-tareas.md).

---

## 1. Asignación del Docente

Al concluir la preparación del curso (`enviarconfirmacion`), el jefe del equipo docente recibe la tarea de designar al docente responsable.

### Consulta Agregada y Contratos (BFF)
Mediante `GET /api/tasks/{taskId}/detail`, el BFF despacha a `DefinirDocenteStrategy`, retornando el contexto del curso, la terna docente y la ruta de respuesta en una única llamada:

```java
public record DocenteDTO(String userId, String name) {}

public record DefinirDocenteDetailDTO(
    String taskId,
    String actionUrl, // "/api/tasks/{taskId}/definir-docente"
    String cursoNombre,
    List<DocenteDTO> docentesDisponibles
) {}

public record DefinirDocenteRequestDTO(String userId) {}
```

Al completar `definirdocentequedesarrollaelcurso` (`flowable:assignee="jefedocente"`), se persiste en el proceso la variable `docenteSeleccionado` con el `userId` elegido.

---

## 2. Reglas de Negocio y Restricciones Técnicas

* **Terna y autoasignación:** El jefe docente debe seleccionar obligatoriamente a uno de los 3 integrantes del equipo docente, pudiendo elegirse a sí mismo.
* **Bloqueo hasta fecha acordada:** El curso no puede dictarse hasta alcanzar la fecha programada. El proceso pausa el avance mediante el timer intermedio `fechaprogramada` (`${fechaCurso}`).
* **Asignación dinámica en Flowable:** La tarea `desarrollarcurso` resuelve su responsable en tiempo de ejecución mediante la expresión `flowable:assignee="${docenteSeleccionado}"`.

---

## 3. Flujo de Ejecución y Cierre

```text
[Enviar Confirmación]
         │
         ▼
[userTask: Definir Docente]          ──► Asignada a 'jefedocente' (guarda docenteSeleccionado)
         │
         ▼
[Timer: Fecha Programada]            ──► Pausa hasta ${fechaCurso}
         │
         ▼
[userTask: Desarrollar Curso]        ──► Asignada dinámicamente a ${docenteSeleccionado}
         │
         ▼
[userTask: Elaborar Informe]         ──► Asignada a 'jefedocente' (persiste dataObject 'informe')
         │
         ▼
[userTask: Enviar Informe y Archivar]──► Asignada a 'jefeadministrativo'
         │
         ▼ (ExecutionListener: resultadoCurso = 'FINALIZADO')
[Throw Compensación: Liberar Recurso]──► Compensa 'prepararcurso' (aulaLiberada = true)
         │
         ▼
[End: Curso Finalizado]              ──► Listener global: sincroniza estado con Backend
```

---

## 4. Liberación de Recursos e Integración

1. **Compensación de Recursos (`liberarrecurso`):**  
   Al terminar normalmente, el evento intermedio de compensación (`activityRef="prepararcurso"`) dispara la liberación de los recursos reservados durante la preparación (ej. `ReleaseClassroomDelegate` marca `aulaLiberada = true`). Reutiliza la misma lógica de compensación sin duplicar código.

2. **Sincronización con el Backend:**  
   En la transición final se fija `resultadoCurso = "FINALIZADO"`. Al alcanzar el fin del proceso, el listener `UpdateCourseStatusListener` notifica al backend vía:
   * **Endpoint:** `PUT /api/flowable/courses/{processId}`
   * **Payload:** `{"status": "FINALIZADO"}`

---

## 5. Verificación en Testing (`HappyPathTest.java`)

El ciclo completo se valida en la suite de integración:

* **Asignación dinámica:** Completa `definirdocentequedesarrollaelcurso` con `docenteSeleccionado = "docente1"` y comprueba que `desarrollarcurso` tenga `assignee == "docente1"`.
* **Disparo temporal:** Verifica que el token espere en `fechaprogramada` y avance tras `fixture.fireTimer(...)`.
* **Cierre y compensación:** Completa la elaboración y envío de informe, verificando que la instancia finalice, `resultadoCurso` sea `"FINALIZADO"` y `aulaLiberada` quede en `true`.

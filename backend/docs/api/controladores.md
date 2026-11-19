# Controladores REST

El backend expone APIs REST agrupadas en controladores especializados para cursos, tareas de usuario, servicios internos de Flowable y autenticación.

---

## 1. Cursos (`/api/courses`)

Gestiona la creación, listado y consulta de cursos de capacitación.

| Método | Endpoint | Roles Permitidos | Descripción |
|---|---|---|---|
| `POST` | `/api/courses` | Autenticado | Crea una nueva solicitud de curso e inicia el proceso BPMN en Flowable. |
| `GET` | `/api/courses` | Autenticado | Lista cursos visibles. Filtra automáticamente: las empresas solo ven los suyos, personal institucional ve todos. |
| `GET` | `/api/courses/{courseId}` | Autenticado | Retorna el detalle completo del curso si el usuario tiene permiso para verlo. |

### Ejemplo: Crear Curso (`POST /api/courses`)
```json
{
  "courseName": "Desarrollo Cloud Nativo con Spring Boot",
  "companyName": "Mercado Libre",
  "companyEmail": "capacitaciones@mercadolibre.com",
  "companyPhone": "+54 11 5555-0100",
  "contactName": "Ana Torres",
  "taxId": "30-70963004-9",
  "possibleDates": ["2026-12-01", "2026-12-15"]
}
```

**Respuesta (`201 Created`):**
```json
{
  "id": 42,
  "processInstanceId": "9102",
  "courseName": "Desarrollo Cloud Nativo con Spring Boot",
  "status": "SOLICITADO",
  "stage": "VIABILIDAD",
  "companyName": "Mercado Libre",
  "createdAt": "2026-11-18T10:00:00Z"
}
```

---

## 2. Tareas (`/api/tasks`)

Permite a los operadores inspeccionar y resolver sus tareas asignadas en Flowable.

| Método | Endpoint | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/tasks` | Autenticado | Retorna la bandeja de entrada (*inbox*) con tareas asignadas al usuario o reclamables. |
| `GET` | `/api/tasks/{taskId}/detail` | Autenticado | Devuelve contexto agregado y el objeto `TaskDetailResponse` con `actionUrl`. |
| `POST` | `/api/tasks/{taskId}/define-teacher` | Autenticado | Asigna el docente responsable del curso (`docenteSeleccionado`). |
| `POST` | `/api/tasks/{taskId}/deliver-course` | Autenticado | Marca el curso como desarrollado por el docente asignado. |
| `POST` | `/api/tasks/{taskId}/prepare-report` | Autenticado | Registra el informe de resultados del curso. |
| `POST` | `/api/tasks/{taskId}/send-report` | Autenticado | Envía el informe a la empresa y archiva el expediente. |

---

## 3. Callbacks Internos (`/api/flowable`)

Endpoints protegidos por el encabezado `X-API-KEY`, invocados por las tareas de servicio de Flowable.

| Método | Endpoint | Header Requerido | Descripción |
|---|---|---|---|
| `PUT` | `/api/flowable/courses/{processId}` | `X-API-KEY` | Sincroniza el cambio de estado del curso (`CourseStatusUpdateDTO`). |
| `PUT` | `/api/flowable/courses/{processId}/details` | `X-API-KEY` | Persiste fechas acordadas o propuesta final. |
| `POST` | `/api/flowable/courses/{processId}/company-access` | `X-API-KEY` | Habilita o recupera las credenciales de la empresa asociada. |

---

## 4. Autenticación (`/api/auth/login`)

Procesa la autenticación JSON mediante `JsonAuthenticationFilter`:

**Petición (`POST /api/auth/login`):**
```json
{
  "username": "jefedocente",
  "password": "secretpassword"
}
```

**Respuesta (`200 OK`):**
```json
{
  "username": "jefedocente",
  "roles": ["ROLE_INSTITUCION"]
}
```
*Se emite automáticamente una cookie de sesión HTTP estándar.*

---

## Próximo Paso

- Conoce el formato estándar de errores en [Manejo Centralizado de Errores](errores.md).

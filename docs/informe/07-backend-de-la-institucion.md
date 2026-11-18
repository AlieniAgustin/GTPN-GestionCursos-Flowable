# 07 — Backend de la institución

> **Explica:** Fran · **Cubre:** la infraestructura sobre la que corre §4.1, y el §3.3 del lado del backend
>
> Documento del módulo: [`backend/README.md`](../../backend/README.md) · ADR: [`strategy-detalle-tareas.md`](../../adr/strategy-detalle-tareas.md), [`arquitecture.md`](../../adr/arquitecture.md)

---

## 1. Qué es y por qué existe

El módulo [`backend/`](../../backend/) es un **Backend-for-Frontend (BFF)**: Spring Boot 3 en el puerto `8081`. Cumple tres funciones a la vez:

1. **Dueño de la base de datos de la institución** (§3.2): las tablas `companies`, `courses` y `proposal_documents`.
2. **Intermediario con el motor**: todas las operaciones sobre instancias y tareas pasan por él, hablando con la API REST de Flowable con una **cuenta técnica única**.
3. **Servidor del sitio web** (§4.1): las páginas son server-side con Thymeleaf dentro del mismo módulo.

Las tres cosas en un solo módulo fue una decisión: mismo Maven, mismo contenedor y **misma sesión** para el sitio y la API, así que no hay CORS, ni Node, ni un servicio más que levantar para la defensa.

```text
Navegador ─────► BFF (8081) ─── cuenta técnica admin/test ───► Motor (8080)
                   │                                              │
                   │ JPA                                          │ tablas ACT_*
                   ▼                                              ▼
           companies / courses / proposal_documents  ◄──── PostgreSQL (5432) ────┘
                                        ▲
                   listeners del motor ─┘ (PUT /api/flowable/**, por API key)
```

El motor y la institución comparten el **servidor** de PostgreSQL pero no las tablas: el motor usa las `ACT_*` y la institución las suyas. Un contenedor menos, y el mismo criterio que ya usaba Flowable UI.

## 2. Las dos puertas de entrada y las tres cadenas de seguridad

La seguridad está partida en tres cadenas en [`SecurityConfig`](../../backend/src/main/java/com/backend/proxy/security/SecurityConfig.java), por orden de precedencia:

| Orden | Alcance | Autenticación | Si falla |
|---|---|---|---|
| 1 | `/api/flowable/**` | **API key** en el header `X-API-KEY` ([`ApiKeyFilter`](../../backend/src/main/java/com/backend/proxy/security/ApiKeyFilter.java)) | 401 |
| 2 | `/api/**` | Sesión (`JSESSIONID`) creada por `POST /api/auth/login` | **401**, que es lo correcto para una API |
| 3 | el resto (el sitio) | Formulario de login en `/login`, con redirección | Redirige al login |

La cadena 1 es la que usan los **listeners del motor** para empujar datos: no hay un usuario detrás, es servicio contra servicio, y la API key evita que cualquiera pueda escribir el estado de un curso.

### Quién puede entrar

Hay **dos orígenes de credenciales**, y los resuelve un solo punto, [`AuthenticationService.resolveRole`](../../backend/src/main/java/com/backend/proxy/service/AuthenticationService.java):

1. **Personal de la institución** → se valida contra el **IDM de Flowable**, pegándole a `/me` del motor con esas credenciales. Es lo que pide §3.3: el personal usa su usuario y contraseña del motor, sin cuentas separadas en la base de la institución.
2. **Empresas** → si lo anterior no valida, se prueba contra `companies.access_username` / `access_password_hash` con **BCrypt**. Son las cuentas que crea *Habilitar acceso de la empresa* (ver [08](08-datos-de-la-empresa-y-acceso.md)).

El resultado es un rol, `INSTITUCION` o `EMPRESA` ([`Roles`](../../backend/src/main/java/com/backend/proxy/security/Roles.java)), y ese rol es lo que decide después qué cursos ve cada uno.

Lo importante del diseño: **la decisión de quién entra está en un solo lugar**. El login de la API (`POST /api/auth/login`) y el del sitio ([`SiteAuthenticationProvider`](../../backend/src/main/java/com/backend/proxy/security/SiteAuthenticationProvider.java)) delegan los dos en el mismo servicio, así que no hay dos reglas que se puedan desincronizar.

## 3. La API

### Pública para el navegador y los clientes REST

| Endpoint | Qué hace |
|---|---|
| `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/auth/me` | Sesión y usuario actual, con su rol |
| `POST /api/courses` | Registra la solicitud e **inicia la instancia** del proceso (§4.1.1) |
| `GET /api/courses` · `GET /api/courses/{id}` | Cursos visibles según el rol (§4.1.2) |
| `GET /api/tasks` | Bandeja del usuario: asignadas + reclamables por su grupo |
| `GET /api/tasks/{id}/detail` | Detalle tipado de la tarea, según su tipo |
| `POST /api/tasks/{id}/define-teacher` · `/deliver-course` · `/prepare-report` · `/send-report` | Completan tareas con datos |

### Interna, solo para el motor (API key)

| Endpoint | Lo llama | Para qué |
|---|---|---|
| `PUT /api/flowable/courses/{processId}` | `UpdateCourseStatusListener` | Estado final del curso (§3.4) |
| `PUT /api/flowable/courses/{processId}/details` | `SyncCourseDetailsListener` | Programa, presupuesto, fecha acordada, informe (§3.2) |
| `POST /api/flowable/courses/{processId}/company-access` | `EnableCompanyAccessDelegate` | Crear o reutilizar la cuenta de la empresa (§4.1.2) |

### Cómo le habla al motor

[`FlowableClient`](../../backend/src/main/java/com/backend/proxy/client/FlowableClient.java) sobre un `RestClient` configurado con la URL base y las credenciales técnicas (`flowable.username` / `flowable.password`, `admin`/`test`). Un interceptor registra cada llamada, que para depurar en la demo vale oro.

El BFF **nunca** consulta las tablas `ACT_*` por SQL: todo lo que necesita del proceso lo pide por la API REST. Así el esquema del motor queda como asunto del motor.

### El detalle de tareas, con Strategy

`GET /api/tasks/{id}/detail` no devuelve un mapa genérico de variables: resuelve una **estrategia por `taskDefinitionKey`** ([`strategy/task/`](../../backend/src/main/java/com/backend/proxy/strategy/task/)) y arma un payload tipado con el contexto que esa tarea necesita y la `actionUrl` para completarla. Si ninguna estrategia aplica, responde 404 con el motivo en vez de devolver algo a medias. Está argumentado en [`adr/strategy-detalle-tareas.md`](../../adr/strategy-detalle-tareas.md).

### Errores

[`GlobalExceptionHandler`](../../backend/src/main/java/com/backend/proxy/exception/GlobalExceptionHandler.java) traduce todo a `ApiErrorResponse` (timestamp, status, error, message, path). `/error` es público a propósito: si estuviera protegido, cualquier excepción terminaría como un 403 opaco que tapa el mensaje real.

## 4. Visibilidad y permisos (lo que no se delega al motor)

Dos reglas que el BFF aplica además de lo que ya controla el motor:

- **Cursos:** una empresa ve **solo los suyos**; el personal ve todos. El curso de otra empresa responde **404 y no 403**, para no revelar que existe.
- **Tareas:** la bandeja se consulta con `candidateOrAssigned={usuario}`, y tanto el detalle como el completado verifican que la tarea esté en esa bandeja. Si no, 404 con el motivo. Además se aplican las dos reglas de §2 que el motor no verifica solo: una tarea de grupo **no se completa sin reclamarla**, y una tarea con responsable **solo la completa esa persona**.

Verificado contra el stack: `docente1` no ve ni puede completar una tarea del grupo `administracion`.

## 5. Persistencia

PostgreSQL, el mismo servidor del stack. Las tablas las genera Hibernate al arrancar (`spring.jpa.hibernate.ddl-auto=update`), así que no hay scripts de esquema que mantener — el enunciado V3 tachó ese requisito. Los datos de prueba los carga [`scripts/seed-demo.sh`](../../scripts/seed-demo.sh), que da de alta empresas y solicitudes **por la API**, igual que lo haría el sitio.

`spring.jpa.open-in-view=false` está explícito: evita que una vista dispare consultas lazy sin querer. Por eso los endpoints devuelven DTOs y no entidades JPA.

## 6. Tests

```bash
cd backend && mvn test
```

**103 tests, 0 fallos.** Cubren el login con sus dos orígenes de credenciales, el alta de cursos con el vínculo a la instancia, la actualización parcial de detalles, la visibilidad por rol, la resolución de estrategias, la bandeja de tareas, los códigos y validaciones de los controladores (`@WebMvcTest`) y las pantallas del sitio.

## 7. Preguntas probables

**¿Por qué un BFF y no que el navegador le pegue directo a la API REST de Flowable?**
Por tres razones: el motor no sabe de empresas (no son usuarios del IDM), habría que exponer el motor al browser con credenciales de cada usuario, y la base de la institución necesita un dueño. El BFF resuelve las tres y deja al motor haciendo lo suyo.

**Si todo pasa por una cuenta técnica, ¿no se pierde quién hizo cada cosa?**
No: el usuario autenticado viaja en cada operación. La bandeja se consulta **por ese usuario** (`candidateOrAssigned`), el reclamo se hace a su nombre y el completado verifica que la tarea sea suya. La cuenta técnica es el transporte, no la identidad.

**¿Por qué 404 y no 403 al pedir el curso de otra empresa?**
Porque un 403 confirma que ese curso existe. Con 404 no se filtra información de otras empresas.

**¿Por qué el sitio vive dentro del backend y no es un proyecto aparte?**
Porque comparte sesión y modelo con la API, y para la defensa es un contenedor menos. Con un frontend separado habría que resolver CORS, duplicar la configuración de sesión y levantar otro servicio.

**¿Qué pasa si el motor está caído?**
Las pantallas que dependen de él avisan y siguen mostrando lo que hay en la base. El alta de solicitudes, en cambio, no puede completarse sin el motor: ahí el error se muestra sin perder el formulario cargado.

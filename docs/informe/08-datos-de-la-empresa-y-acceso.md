# 08 — Datos de la empresa y su acceso: qué se guarda, cuándo se actualiza y qué ve cada uno

> **Explica:** Fran · **Cubre:** §3.2 (base de datos de la institución), §4.1.2 (acceso y seguimiento para las empresas)
>
> Criterio completo: [`adr/datos-proceso-vs-bd.md`](../../adr/datos-proceso-vs-bd.md)

---

## 1. Qué pide el enunciado

**§3.2.** Una base de datos propia de la institución con las empresas solicitantes y la información de los cursos: **nombre, descripción, programa, presupuesto, fechas e informe de resultados**. Cada curso vinculado con su **instancia del proceso** en Flowable. La lista de participantes y los recursos asignados **pueden** quedar como variables del proceso.

**§4.1.2.** Luego de aceptar la propuesta y antes de empezar la preparación, la actividad automática *Habilitar acceso de la empresa* **crea una cuenta** para la empresa si todavía no tiene una; si ya existe por una contratación anterior, **se reutiliza** y se vincula el nuevo curso con ella. La comunicación de las credenciales puede simularse. Una vez identificada, la empresa puede: consultar **sus** cursos, ver el **estado** de un curso (en preparación, confirmado y pendiente de inicio, en desarrollo, en elaboración del informe, finalizado o cancelado) y, durante la preparación, consultar las **actividades pendientes o en ejecución**, contemplando las que corren en paralelo.

---

## 2. Qué dato se guarda

Tres tablas, en el mismo PostgreSQL del stack pero separadas de las `ACT_*` del motor.

### `companies` — la empresa solicitante

| Columna | Qué es | De dónde sale |
|---|---|---|
| `id` | identificador | alta |
| `name` | razón social | formulario de solicitud (§4.1.1) |
| `email`, `phone` | contacto | formulario de solicitud |
| `access_username` | usuario con el que entra al sitio | *Habilitar acceso de la empresa* (§4.1.2) |
| `access_password_hash` | contraseña, con **BCrypt** | *Habilitar acceso de la empresa* |

La empresa se **reutiliza** si ya existe: se busca por razón social antes de crearla, así que la segunda contratación de ACME no crea una ACME nueva.

### `courses` — el curso

| Columna | Qué es | Lo produce |
|---|---|---|
| `id` | identificador; es el **business key** de la instancia | alta de la solicitud |
| `process_instance_id` | **el vínculo con Flowable** que pide §3.2 | respuesta del motor al iniciar la instancia |
| `company_id` | la empresa dueña | alta |
| `name`, `description` | temática y descripción | formulario de solicitud |
| `possible_dates` | fechas preferidas por la empresa (`jsonb`) | formulario de solicitud |
| `program` | programa del curso | *Registrar y elaborar propuesta* |
| `budget` | presupuesto | *Registrar y elaborar propuesta* |
| `agreed_date` | fecha de inicio acordada | la **aceptación** de la empresa (§3.5) |
| `report` | informe de resultados | *Elaborar informe de resultados* |
| `status` | cómo **terminó** el trámite | `UpdateCourseStatusListener` al cerrar (§3.4) |

El vínculo con el proceso es **en los dos sentidos**: el curso guarda el `process_instance_id`, y la instancia arranca con el id del curso como *business key* y con `courseId` como variable.

### `proposal_documents` — las versiones de la propuesta

Una fila por versión del PDF: `version`, `program`, `budget`, `proposed_dates`, `created_by`, `created_at` y el `pdf` en bytes. Así *Revisar propuesta* genera la versión siguiente y el historial de la negociación queda completo.

### Lo que **no** se guarda, y por qué

Todo lo demás vive como **variable del proceso**: los recursos (`requiereAula`, `aulaAsignada`, …), la lista definitiva de participantes, el docente seleccionado, los contadores de recordatorios. El criterio es la **vida útil del dato**:

> Si le sirve a la institución después de que la instancia terminó, va a la BD.
> Si solo existe para que el proceso decida mientras corre, queda como variable.

`listaDefinitiva` y los recursos quedan como variables **por decisión explícita del enunciado**, que lo permite. El único dato duplicado a propósito es el `informe`: variable mientras el proceso lo necesita para la tarea de envío, y columna porque la institución lo conserva. Las dos tablas completas —las 10 columnas que persisten y las ~30 variables que no— están en [`adr/datos-proceso-vs-bd.md`](../../adr/datos-proceso-vs-bd.md).

---

## 3. Cuándo se actualiza

La regla es: **el motor empuja, el BFF persiste**. El motor nunca escribe SQL de la institución y el BFF no sondea al motor.

```text
userTask completada ──take──► SyncCourseDetailsListener
                                   │ PUT /api/flowable/courses/{processId}/details
                                   ▼
                              CourseService.updateCourseDetails  ──► courses
```

[`SyncCourseDetailsListener`](../../flowable/src/main/java/com/flowable/app/listeners/SyncCourseDetailsListener.java) está enganchado en los **cuatro flujos** donde el proceso produce datos:

| Momento | Qué se escribe |
|---|---|
| Al enviar la propuesta | `program`, `budget` |
| Al reenviarla revisada | `program`, `budget` de la nueva versión |
| Al aceptar la propuesta | `agreed_date` (corre después de que `ResolveCourseDateListener` normaliza la fecha) |
| Al elaborar el informe | `report` |

Y dos integraciones más, en otros momentos:

| Momento | Quién | Qué se escribe |
|---|---|---|
| Al dar de alta la solicitud | El sitio / `POST /api/courses` | La empresa, el curso y el `process_instance_id` |
| Después de la aceptación, antes de la preparación | `EnableCompanyAccessDelegate` | `access_username`, `access_password_hash` |
| Al cerrar la instancia, por cualquiera de los cinco caminos | `UpdateCourseStatusListener` | `status` |

Tres propiedades del mecanismo que conviene poder defender:

1. **Idempotente y parcial.** El listener manda solo las variables que ya existen, y la entidad pisa solo los campos que llegan con valor. Reenviar una propuesta revisada no borra la fecha ni el informe.
2. **Va por el motor, no por el BFF.** El enunciado permite completar varias tareas desde Flowable UI, sin pasar por el sitio (§4.1.3, §4.2.1). Enganchado en el modelo, la base queda sincronizada **se complete por donde se complete**.
3. **No bloquea la instancia.** Si el BFF no responde, el listener registra el error y el proceso sigue. La BD de la institución es un registro paralelo, no una condición para avanzar.

El costo asumido: el dato llega **con el desfasaje de la etapa**. El programa aparece en la base cuando la propuesta se envía, no mientras se escribe. Para ver datos en vivo de una tarea abierta hay que leer las variables de la instancia, y es exactamente lo que hacen las pantallas que lo necesitan.

---

## 4. Habilitar el acceso de la empresa (§4.1.2)

Es una actividad **automática** del proceso, entre la aceptación y la preparación: `habilitaraccesodelaempresa`, un service task con [`EnableCompanyAccessDelegate`](../../flowable/src/main/java/com/flowable/app/delegates/EnableCompanyAccessDelegate.java).

```text
Aceptación ─► Habilitar acceso de la empresa ─► [Preparar curso]
                      │ POST /api/flowable/courses/{processId}/company-access
                      ▼
               CompanyAccessService
                      ├─ la empresa ya tiene cuenta  ─► la reutiliza  (created = false)
                      └─ no tiene                    ─► la crea       (created = true)
```

[`CompanyAccessService`](../../backend/src/main/java/com/backend/proxy/service/CompanyAccessService.java) busca el curso por su `process_instance_id`, toma **su** empresa y:

- Si la empresa ya tiene `access_username`, la devuelve tal cual. El curso nuevo queda vinculado a esa cuenta por su `company_id`: no hay nada más que hacer, porque el vínculo curso→empresa ya existe desde el alta.
- Si no tiene, genera el usuario a partir de la razón social (`ACME S.A.` → `acme-sa`, desambiguado con el id si ya estaba tomado) y una contraseña aleatoria de 12 caracteres, que guarda **hasheada con BCrypt**.

Las credenciales se **comunican simuladas**, como permite el enunciado: el usuario queda en la variable `empresaUsuario` y, solo cuando la cuenta se acaba de crear, la contraseña en `empresaPasswordTemporal` y en el log del motor. En la contraseña está la única copia en claro, y es deliberado: en la base solo vive el hash.

Si el BFF no responde, la instancia sigue con `empresaAccesoHabilitado = false` en vez de trabarse, y el log lo avisa.

**Tests:** `CompanyAccessTest` cubre los tres casos —cuenta nueva, cuenta reutilizada y backend caído— verificando además que el curso entre igual en preparación.

---

## 5. Qué ve la institución y qué ve la empresa

El rol que resolvió el login (ver [07](07-backend-de-la-institucion.md)) decide todo.

| | Empresa (`EMPRESA`) | Personal de la institución (`INSTITUCION`) |
|---|---|---|
| Cómo entra | Cuenta de `companies`, creada por *Habilitar acceso* | Usuario y contraseña **de Flowable** (§3.3) |
| Cursos que lista `/mis-cursos` | **Solo los suyos** | **Todos** |
| Curso de otra empresa | **404**, no 403: no se revela que existe | — |
| Estado del curso | Sí, el estado en vivo | Sí |
| Actividades pendientes o en ejecución | Sí, durante la preparación | Sí |
| Programa, presupuesto, fechas, informe | Sí, de su curso | Sí |
| PDF de las propuestas | Sí, los de su curso | Sí |
| Bandeja de tareas `/tareas` | **No**: no tiene tareas en el proceso | Sí, las suyas y las de su grupo |
| Completar tareas | **No** | Sí, según §2 |
| Formulario público de solicitud | Sí, sin estar logueada | — |

La decisión de visibilidad vive en `CourseService`, **un solo lugar**, y la usan tanto la API como las pantallas del sitio: no hay dos reglas que puedan quedar desalineadas.

### El estado del curso, que no estaba en la base

Esto es lo más interesante de §4.1.2. La columna `courses.status` guarda **cómo terminó** el trámite, y recién cuando cerró: no sirve para decirle a la empresa "tu curso está en preparación".

El estado en vivo lo **deduce** [`CourseProgressService`](../../backend/src/main/java/com/backend/proxy/service/CourseProgressService.java) a partir de las actividades que el motor tiene abiertas, y lo expone como [`CourseStage`](../../backend/src/main/java/com/backend/proxy/types/CourseStage.java) con los seis estados que pide el enunciado más los previos a la aceptación y los otros cierres:

| Etapa | Cuándo |
|---|---|
| En evaluación | la institución está evaluando la viabilidad o armando la propuesta |
| Propuesta enviada | esperando la respuesta de la empresa |
| **En preparación** | el subproceso de preparación está abierto |
| **Confirmado, pendiente de inicio** | la preparación terminó y el curso espera la fecha acordada |
| **En desarrollo** | el curso se está dictando |
| **En elaboración del informe** | terminó el dictado |
| **Finalizado** / **Cancelado** | cerró |
| Propuesta rechazada · No viable · Cerrado por falta de respuesta | los otros cierres |

Se evalúa **de la etapa más avanzada a la más temprana**, así una instancia con varias actividades abiertas no reporta una etapa vieja.

### Las actividades en paralelo

Salen de `historic-activity-instances?finished=false` del motor, filtrando el subproceso contenedor y las compuertas —están activos, pero no son algo que la empresa esté esperando—. Un curso en preparación muestra las tres ramas a la vez: *Evaluar Recursos*, *Preparar materiales* y *Recibir nómina*, que es justo lo que pide el enunciado cuando dice "contemplando las actividades que se ejecutan simultáneamente".

El motor se consulta **una sola vez por página** y se agrupa en memoria, para no disparar una llamada por curso. Si el motor no responde, el sitio sigue mostrando lo que hay en la base.

---

## 6. Cómo se demuestra

Es el **escenario 10** de la defensa:

1. Aceptar la propuesta de un curso desde la app de comunicaciones. En el log del motor aparece la cuenta creada: `Cuenta creada para 'ACME S.A.': usuario acme-sa`, y la contraseña temporal en la variable `empresaPasswordTemporal` de la instancia.
2. Entrar al sitio en `/login` con esa cuenta.
3. `/mis-cursos` muestra **solo** los cursos de ACME, con su estado en vivo.
4. Abrir el curso en preparación: se ven las tres actividades paralelas y los PDFs de las propuestas.
5. Pedir el id de un curso de otra empresa en la URL: **404**.
6. Aceptar una **segunda** solicitud de la misma empresa: el log dice `ya tenía cuenta: se reutiliza el usuario acme-sa`, y el curso nuevo aparece en la misma lista.

En la base:

```bash
docker exec flowable-postgres psql -U flowable -d flowable -c \
  'select id, name, program, budget, agreed_date, status, process_instance_id from courses order by id;'
```

---

## 7. Preguntas probables

**¿Por qué la contraseña de la empresa está hasheada y la del personal no la guardan?**
Porque el personal no tiene cuenta en la institución: se valida contra el IDM de Flowable, como pide §3.3. La empresa sí necesita una cuenta propia —no es usuario del motor— y de esa guardamos solo el hash BCrypt.

**¿Qué pasa si se habilita el acceso dos veces para la misma empresa?**
Nada: el servicio es idempotente. Detecta que ya tiene cuenta, la reutiliza y no emite una contraseña nueva. Eso cubre literalmente el "si ya existe una cuenta por una contratación anterior, se reutilizará".

**Si el estado en vivo se deduce del motor, ¿para qué está `courses.status`?**
Para que el registro sobreviva a la instancia. El historial de Flowable se puede purgar, y la institución igual tiene que saber cómo terminó cada curso. `status` es el cierre; `CourseStage` es la foto del momento.

**¿Por qué el programa y el presupuesto se escriben al enviar la propuesta y no al redactarla?**
Porque el dato recién existe cuando la tarea se completa: hasta entonces está en el formulario del navegador. El listener va en el flujo de salida de la tarea, que es el primer momento en que el valor es firme.

**¿Y si el BFF está caído justo cuando el motor quiere sincronizar?**
Ese dato no se reintenta: queda el error en el log y el proceso avanza. Lo asumimos a conciencia, porque preferimos una base con un campo desactualizado antes que una instancia trabada. Si algún día molesta, el arreglo es un reintento en el listener, no cambiar el criterio.

# TODO — Lo que falta implementar

Checklist del equipo, derivada del enunciado [`docs/ProyectoCursos_V3.pdf`](docs/ProyectoCursos_V3.pdf) contrastada con lo que hoy está en el repositorio.

**Cómo usarla:** marcá el `[x]` cuando el ítem esté hecho **y probado**, y poné tu nombre entre paréntesis al final de la línea. Si un ítem te queda grande, partilo en sub-ítems antes de empezar. Las referencias `§` son secciones del PDF.

**Integrantes:** Agustin Alieni · Agustin Cesari · Damian Dalio · Hernan Jara · Francisco Natale

---

## Resumen

| Bloque | Estado |
|---|---|
| 1. Motor y modelo BPMN | Completo |
| 2. BD de la institución | Completo |
| 3. Backend BFF | Completo |
| 4. Sitio web institucional | Completo |
| 5. App Java (API Java) | Completo |
| 6. App de comunicaciones (API REST) | Completo |
| 7. Análisis de ejecuciones | Completo |
| 8. Minería de procesos | Completo — 🔴 detectó un defecto en los recordatorios |
| 9. Entrega y demo | Escenarios 1–8 cubiertos por tests; 9, 10 y 11 ya se pueden mostrar con el sitio y las dos apps |

---

## 1. Motor y modelo BPMN

Completo: el BPMN ejecutable, el DMN de viabilidad, los listeners de §3.4, los delegates de aulas y de acceso de la empresa, los plazos parametrizables de §3.5 y la seguridad por grupos.

- [x] **Hacer que el seed corra en `prod`** (§3.3). El seed salió de `App.java` y ahora vive en [`IdentitySeeder.java`](flowable/src/main/java/com/flowable/app/config/IdentitySeeder.java), **sin `@Profile`**: corre en cada arranque y en cualquier perfil, así que el stack Docker ya levanta con los cinco usuarios, los dos grupos y la cuenta técnica `admin`/`test`. Se puede desactivar con `institucion.seed.enabled=false`. Es idempotente y además repara membresías faltantes (antes, si el usuario existía pero no estaba en el grupo, quedaba así), y loguea cada alta para verificarlo con `docker compose logs flowable-app`. Se eliminó el workaround de arrancar la app local (paso 1.3 del README). `IdentitySeederTest` lo verifica con perfil `prod`; 12 tests pasando. ( )
- [x] **Implementar la actividad automática *Habilitar acceso de la empresa*** (§4.1.2). En el BPMN pasó de un `scriptTask` con un `println` a un service task con [`EnableCompanyAccessDelegate`](flowable/src/main/java/com/flowable/app/delegates/EnableCompanyAccessDelegate.java), que llama al BFF (`POST /api/flowable/courses/{processId}/company-access`). Del otro lado, [`CompanyAccessService`](backend/src/main/java/com/backend/proxy/service/CompanyAccessService.java) busca el curso por `processInstanceId`, toma su empresa y, si todavía no tiene cuenta, le crea una: usuario derivado de la razón social (desambiguado con el id si ya estaba tomado) y contraseña random guardada con BCrypt en las columnas nuevas `access_username` / `access_password_hash` de `companies`. Si la empresa ya contrató antes, se reutiliza la cuenta y el curso nuevo queda vinculado a ella por su `company_id`. El usuario queda en la variable `empresaUsuario` y, solo cuando la cuenta se acaba de crear, la contraseña en `empresaPasswordTemporal`: el enunciado permite simular la comunicación de las credenciales, así que se leen de la instancia o del log. Si el BFF no responde, la instancia sigue con `empresaAccesoHabilitado = false` en vez de trabarse. `CompanyAccessTest` cubre los tres casos (creación, reutilización, backend caído). **Falta** la pantalla de login de la empresa, que es del bloque 4. ( )
- [x] **Verificar que todos los caminos de cierre dejan seteado el resultado** (§3.4). Verificado: los cinco caminos ya marcaban `resultadoCurso` y no hay ninguno suelto. Para que siga siendo así, [`ClosingPathsTest`](flowable/src/test/java/com/flowable/ClosingPathsTest.java) lee el modelo desplegado y, por cada evento de fin del proceso, camina hacia atrás por los flujos comprobando que **todos** los caminos que llegan ahí pasan por un listener que setea la variable; un segundo test exige que los resultados del modelo sean exactamente los cinco del enunciado. Probado sabotear un flujo: el test falla indicando el evento de fin alcanzable sin resultado. Además se completaron las aserciones que faltaban: `NO_VIABLE` en `CourseLimitExceededTest` (los otros cuatro ya estaban) y, en `HappyPathTest`, que el `UpdateCourseStatusListener` efectivamente le manda el estado al BFF. ( )
- [x] **Revisar los plazos para la demo** (§3.5). Parametrizado y documentado (sección "Acortar los plazos para la demo" del [README](README.md)). Dos properties nuevas con sus variables de entorno: `proceso.plazo-respuesta` (default `P5D`, lo aplica [`ApplyResponseDeadlineListener`](flowable/src/main/java/com/flowable/app/listeners/ApplyResponseDeadlineListener.java) al entrar a *Enviar propuesta*) y `proceso.fecha-curso-offset` (default `P7D`, para cuando la empresa no manda fecha). En [`docker-compose.yml`](docker-compose.yml) quedaron las dos variables comentadas, listas para la demo. El plazo por instancia le gana a la property, así que se puede iniciar una solicitud con `plazoRespuesta` propio. El BPMN no tiene ninguna duración ni fecha fija: sigue usando `${plazoRespuesta}` y `${fechaCurso}`. ( )
- [x] **Definir `fechaCurso` durante la ejecución** (§3.5). Lo resuelve [`ResolveCourseDateListener`](flowable/src/main/java/com/flowable/app/listeners/ResolveCourseDateListener.java), que corre al tomar el flujo que sale de *Aceptación*: toma la fecha que trajo el mensaje en `fechaCurso` o en `fechaAcordada` (el nombre que usa la app de comunicaciones de §4.2.2), la normaliza a ISO-8601 con offset — acepta `Date`, `2026-03-01`, `2026-03-01T09:00` y `2026-03-01T09:00-03:00` — y, si no llegó nada usable, la deriva de `proceso.fecha-curso-offset` y lo avisa en el log. Así la variable **siempre** está definida antes del timer y no vuelve a aparecer `Unknown property used in expression: ${fechaCurso}`. `CourseDateTest` cubre los tres casos, incluido el happy path completo **sin** inyectar la fecha al iniciar la instancia, que es el que fallaba. ( )

---

## 2. Base de datos de la institución (§3.2)

Completo. Las entidades `Company` y `Course` viven en [`backend/src/main/java/com/backend/proxy/entity/`](backend/src/main/java/com/backend/proxy/entity/) y el criterio de qué se persiste está en [`adr/datos-proceso-vs-bd.md`](adr/datos-proceso-vs-bd.md).

- [x] **Vincular cada curso con su instancia de proceso**. `POST /api/courses` da de alta empresa y curso, inicia la instancia y guarda el `processInstanceId` en la fila del curso. El sentido inverso ya andaba: el motor avisa el estado final por `PUT /api/flowable/courses/{processId}`. ( )
- [x] **Reemplazar H2 en memoria por algo persistente.** El BFF pasó a PostgreSQL, compartiendo el servidor del stack: el motor usa las tablas `ACT_*` y la institución `companies` / `courses`. Esto además era un **bug bloqueante**: `Course.possibleDates` se mapea con `columnDefinition = "jsonb"`, que H2 no conoce, así que el `create table courses` fallaba en silencio al arrancar y cualquier alta moría con `Table "COURSES" not found`. ( )
- [x] **Guardar el resto de la información del curso**: programa, presupuesto, fecha acordada e informe de resultados. `Course` sumó las columnas `program`, `agreed_date` y `report` (y ahora sí se escribe `budget`, que estaba declarada pero nunca se llenaba). Los datos viajan por un endpoint interno nuevo, `PUT /api/flowable/courses/{processId}/details`, que actualiza **solo** los campos que llegan con valor. Del lado del motor los empuja [`SyncCourseDetailsListener`](flowable/src/main/java/com/flowable/app/listeners/SyncCourseDetailsListener.java), enganchado en los cuatro flujos donde el proceso produce datos: al enviar la propuesta y al reenviarla revisada (programa y presupuesto), al aceptarla (fecha, después de que `resolveCourseDate` la normaliza) y al elaborar el informe. Va por el motor y no por el BFF a propósito, para que la base quede sincronizada aunque la tarea se complete desde Flowable UI. `CourseDetailsSyncTest` cubre los cuatro casos; 24 tests pasando. ( )
- [x] **Decidir y documentar qué queda como variable del proceso y qué va a la BD.** Escrito en [`adr/datos-proceso-vs-bd.md`](adr/datos-proceso-vs-bd.md). El criterio es la vida útil del dato: si le sirve a la institución después de que la instancia terminó, va a la BD; si solo existe para que el proceso decida mientras corre, queda como variable. El ADR lista las dos tablas completas (las 10 columnas que persisten y las ~30 variables que no), explica el mecanismo de sincronización y deja asentado por qué `listaDefinitiva` y los recursos quedan como variables —el enunciado lo permite— y por qué `informe` es el único dato duplicado a propósito. ( )
- [x] **Datos de prueba**: [`scripts/seed-demo.sh`](scripts/seed-demo.sh) da de alta empresas y solicitudes por la API del BFF, igual que lo haría el sitio web, y las reparte en cuatro estados distintos (aceptada y en preparación, rechazada, con modificaciones pendientes, recién ingresada) para tener recorridos variados para §4.3 y §4.4. Se corre con `./scripts/seed-demo.sh [cantidad]`. **Los scripts de creación del esquema no hacen falta**: el enunciado V3 tachó ese requisito y, además, las tablas las genera Hibernate al arrancar el BFF (`ddl-auto=update`). ( )

---

## 3. Backend BFF

Completo. El BFF autentica a los dos tipos de usuario, opera contra el motor y respeta la visibilidad de cada rol. Detalle en [`backend/README.md`](backend/README.md).

- [x] **Crear el `AuthController`** con `POST /api/auth/login`, `POST /api/auth/logout` y `GET /api/auth/me`. Valida contra el `/me` de Flowable con el `FlowableAuthService` que ya existía y deja el SecurityContext en la `HttpSession`. ( )
- [x] **Cablear un mecanismo de autenticación.** La cadena principal usa `HttpSessionSecurityContextRepository`, así que después del login alcanza con la cookie `JSESSIONID`. Sin sesión la API devuelve **401** (antes 403), que es lo correcto para una API. ( )
- [x] **Corregir `CourseController`**: el `@PathVariable processId` que no existía en la ruta se eliminó; el endpoint quedó como `POST /api/courses` y devuelve `201` con `Location` y un `CourseResponseDTO` (no la entidad JPA, para no serializar la asociación lazy `company`). ( )
- [x] **Implementar `TaskService` y las cuatro `TaskStrategy`.** `GET /api/tasks/{id}/detail` lee la tarea y las variables de la instancia en Flowable, resuelve la estrategia por `taskDefinitionKey` y devuelve el payload tipado con su `actionUrl`. Si ninguna estrategia aplica, 404 con el motivo. `complete()` arma el payload de Flowable y lo postea. También se completó `TeacherRepositoryStub` con los tres docentes de §2. ( )
- [x] **`/error` alcanzable sin autenticación**, así los errores salen con el formato del `GlobalExceptionHandler` en vez de convertirse en un 403 opaco. ( )
- [x] **Backend agregado a [`docker-compose.yml`](docker-compose.yml)** con su `Dockerfile` y `build.sh`, healthcheck sobre un `/health` nuevo, y resolución por nombre de servicio en ambos sentidos (`BACKEND_BASE_URL` en el motor, `FLOWABLE_BASE_URL` en el BFF). ( )
- [x] **Tests del backend**: de 0 a **27**. `AuthenticationServiceTest` (los dos orígenes de credenciales, contraseña incorrecta, empresa sin acceso todavía, cierre de sesión), `CourseServiceTest` (alta con vínculo a la instancia, reutilización de empresa, actualización parcial de detalles, visibilidad por rol, estado terminal), `TaskServiceTest` (resolución de estrategia, tarea sin estrategia, tarea ajena, bandeja) y `CourseControllerTest` (`@WebMvcTest`: códigos, `Location`, validación de email y de fecha pasada). ( )
- [x] **Filtrar las tareas por usuario.** `TaskController` recibe el usuario con `@AuthenticationPrincipal` y lo pasa al servicio. La bandeja consulta `candidateOrAssigned={usuario}`, y tanto el detalle como el completado verifican que la tarea esté en esa bandeja: si no, **404** con el motivo. Verificado contra el stack: `docente1` no ve ni puede completar una tarea del grupo `administracion`. ( )
- [x] **Endpoints de consulta.** `GET /api/tasks` devuelve la bandeja del usuario (asignadas + reclamables por grupo). `GET /api/courses` y `GET /api/courses/{id}` aplican la visibilidad del enunciado 4.1.2: una empresa ve **solo los suyos**, el personal de la institución ve todos; el curso de otra empresa da **404 y no 403**, para no revelar que existe. ( )
- [x] **Login de empresas** (necesario para lo anterior). El mismo `POST /api/auth/login` acepta los dos tipos de usuario: primero prueba contra el IDM de Flowable (personal, §3.3) y si no valida prueba contra las cuentas de `companies` con BCrypt (empresas, §4.1.2). La respuesta incluye el `role` (`INSTITUCION` / `EMPRESA`), que es lo que decide qué cursos ve cada uno. La pantalla de login sigue siendo del bloque 4. ( )

---

## 4. Sitio web de la institución (§4.1)

Completo. Son páginas server-side con **Thymeleaf dentro del módulo `backend`**: mismo Maven, mismo contenedor y misma sesión que la API, así que no hay CORS, ni Node, ni un servicio más que levantar. Los estilos están en [`static/css/sitio.css`](backend/src/main/resources/static/css/sitio.css) y las plantillas en [`templates/`](backend/src/main/resources/templates/).

La cadena de seguridad quedó partida en tres ([`SecurityConfig`](backend/src/main/java/com/backend/proxy/security/SecurityConfig.java)): `/api/flowable/**` por API key, `/api/**` con 401 para los clientes REST, y el sitio con formulario de login y redirección. La API no cambió de comportamiento.

**Sección pública**

- [x] Formulario de **solicitud de capacitación** con datos de la empresa y del contacto, temática, descripción, cantidad de participantes y fechas preferidas. Es un form bean plano ([`SolicitudForm`](backend/src/main/java/com/backend/proxy/web/form/SolicitudForm.java)) porque un formulario HTML no manda objetos anidados ni listas tipadas; las fechas llegan como texto separado por comas y se parsean a mano para poder decir *"No se entendió la fecha '01/03/2027'"* en vez de tirar un error de binding. Los errores se muestran sobre cada campo y no se pierde lo cargado. ( )
- [x] Al enviarlo: **registra la solicitud e inicia la instancia**. Reutiliza `CourseService.processCourse`, así que el curso queda en la BD de la institución con su `process_instance_id` y el proceso arranca por el mensaje `Solicitud de curso`. Verificado de punta a punta: la solicitud 102 de "Textiles Andinas" quedó en la base y dejó la tarea *Registrar y elaborar propuesta* en la bandeja del jefe administrativo. **Es el escenario 9 de la defensa.** Si el motor no responde, se avisa sin perder el formulario. ( )

**Acceso de empresas**

- [x] **Login** unificado en `/login` para los dos tipos de usuario, con [`SiteAuthenticationProvider`](backend/src/main/java/com/backend/proxy/security/SiteAuthenticationProvider.java) delegando en el mismo `AuthenticationService.resolveRole` que usa la API, para no duplicar la decisión de quién entra. Verificado en el navegador. ( )
- [x] **Consultar sus cursos** en `/mis-cursos`, solo los propios. La visibilidad la resuelve `CourseService`, así que no hay lógica duplicada: pedir el curso de otra empresa da **404 y no 403**, para no revelar que existe. El personal de la institución entra a la misma pantalla y ve todos. Verificado en el navegador: `acme-sa` ve sus 3 cursos y recibe 404 en el 53, que es de Molinos del Sur. ( )
- [x] **Ver el estado** de cada curso. Esto **no estaba en la base**: `courses.status` solo guarda cómo terminó el trámite y recién cuando cerró. El estado en vivo lo deduce [`CourseProgressService`](backend/src/main/java/com/backend/proxy/service/CourseProgressService.java) a partir de las actividades que el motor tiene abiertas, y se expone como [`CourseStage`](backend/src/main/java/com/backend/proxy/types/CourseStage.java) con los seis estados del enunciado más los previos a la aceptación (*En evaluación*, *Propuesta enviada*) y los otros cierres. Se evalúa de la etapa más avanzada a la más temprana, así una instancia con varias actividades abiertas no reporta una etapa vieja. ( )
- [x] **Listar las actividades pendientes o en ejecución**, con las ramas paralelas. Salen de `historic-activity-instances?finished=false`, filtrando el subproceso contenedor y las compuertas, que están activos pero no son algo que la empresa esté esperando. Verificado en el navegador: el curso en preparación muestra *Evaluar Recursos*, *Preparar materiales* y *Recibir nómina* a la vez. El motor se consulta **una sola vez por página** y se agrupa en memoria, para no disparar una llamada por curso. Si el motor no responde, el sitio sigue mostrando lo que hay en la base. ( )

**Funciones para Administración** (ingresan con su usuario y contraseña de Flowable, §3.3)

- [x] Consultar **tareas asignadas y disponibles para su grupo** en `/tareas`. La bandeja las muestra en dos listas separadas, porque el enunciado trata los dos casos distinto. Cada tarea indica a qué curso pertenece, resuelto con una sola consulta de cursos por página. Es la misma pantalla para Administración y para el equipo docente: cada uno ve lo suyo según sus grupos. ( )
- [x] **Reclamar y completar** respetando las responsabilidades. `TaskService` suma `claim()` y dos reglas del enunciado 2 que antes no estaban: una tarea de grupo **no se completa sin reclamarla primero**, y una tarea con responsable **solo la completa esa persona**. Las asignadas directamente a un jefe no requieren reclamo y aparecen listas para completar. Verificado contra el stack: completar sin reclamar responde *"Primero tenés que reclamar la tarea 'Preparar materiales'"*, `administrativo1` la reclama y la completa, y `jefeadministrativo` no puede ni reclamarla ni completarla. ( )
- [x] Las tareas que **sí** se completan desde la bandeja son las de confirmación, catalogadas en [`TaskCatalog`](backend/src/main/java/com/backend/proxy/types/TaskCatalog.java) junto con las variables que dejan. Las que necesitan capturar datos aparecen marcadas *"Se completa desde Flowable UI"* hasta que tengan su formulario. ( )
- [x] Completar *Registrar y elaborar propuesta* con formulario y **generar el PDF** de la propuesta. Pantalla `/tareas/{id}/elaborar-propuesta` en [`ProposalSiteController`](backend/src/main/java/com/backend/proxy/web/ProposalSiteController.java): muestra la solicitud, pide programa, presupuesto y fechas posibles (precargadas con las que pidió la empresa) y valida cada campo sin perder lo escrito. Al enviar, [`ProposalService`](backend/src/main/java/com/backend/proxy/service/ProposalService.java) arma el PDF con OpenPDF ([`ProposalPdfBuilder`](backend/src/main/java/com/backend/proxy/service/ProposalPdfBuilder.java)), lo guarda como versión 1 en la tabla nueva `proposal_documents` y completa la tarea con `programa`, `presupuesto`, `fechasPropuestas` y `versionPropuesta`, todo en una transacción. Los PDF se descargan en `/mis-cursos/{id}/propuestas/{version}.pdf` con la misma visibilidad que el curso, y el detalle del curso lista las versiones. Deja lista la base para *Revisar propuesta*, que solo tiene que generar la versión siguiente. `ProposalServiceTest` y `ProposalSiteControllerTest` (12 tests); backend en 87. Verificado contra el stack: la instancia quedó esperando respuesta y el curso con programa y presupuesto en la BD. (Agustin Cesari)
- [x] Completar *Revisar propuesta* **generando una nueva versión del PDF**. Pantalla `/tareas/{id}/revisar-propuesta` en el mismo [`ProposalSiteController`](backend/src/main/java/com/backend/proxy/web/ProposalSiteController.java) y con el mismo formulario que elaborar: muestra las **observaciones** que mandó la empresa al pedir modificaciones (variable `observaciones`, leída del motor) y arranca **precargado con la última versión** para cambiar solo lo pedido. Al enviar genera el PDF de la versión siguiente con el mismo `ProposalService.enviarPropuesta`, y el proceso vuelve a *Enviar propuesta* (recordatorios en 0). El detalle del curso lista todas las versiones con la última como vigente. 4 tests nuevos; backend en 91. Verificado contra el stack: modificaciones desde la API, versión 2 del PDF, presupuesto nuevo en la BD y la instancia otra vez esperando respuesta. (Agustin Cesari)
- [x] Completar *Archivar solicitud* desde la bandeja: es una tarea de confirmación, ya catalogada en `TaskCatalog`, y viene asignada directamente al jefe administrativo así que no requiere reclamo. Verificado contra el stack: al completarla la instancia cerró con `resultadoCurso = RECHAZADO`. ( )
- [x] Completar *Confeccionar lista definitiva* desde la bandeja. Es de grupo, así que hay que reclamarla primero; deja la variable `listaDefinitiva`. Verificado dentro del recorrido completo: cerró la rama de inscripciones y, con las otras dos, el subproceso de preparación. ( )
- [x] Completar *Evaluar recursos* con una pantalla que permita elegir aula física, aula virtual y/o equipamiento especial — **al menos una alternativa**. Pantalla `/tareas/{id}/evaluar-recursos` en [`TaskFormSiteController`](backend/src/main/java/com/backend/proxy/web/TaskFormSiteController.java), a la que se llega desde el botón *Completar* de la bandeja una vez reclamada. Sin ninguna opción marcada vuelve al formulario con el error y no completa nada. Siempre manda las tres variables del gateway inclusivo (`requiereAula`, `requierePlataforma`, `requiereEquipamiento`), aunque sean `false`, para que el motor no falle con *Unknown property*. Verifica que la tarea sea de ese tipo y visible para el usuario. Verificado contra el stack: con *Aula virtual* el proceso abrió *Solicitar Aula Virtual*. (Agustin Cesari)
- [x] Permitir al jefe administrativo **ver el informe** y completar *Enviar informe a empresa y archivar expediente*. Pantalla `/tareas/{id}/enviar-informe` en [`TaskFormSiteController`](backend/src/main/java/com/backend/proxy/web/TaskFormSiteController.java): muestra el informe que dejó el equipo docente y pide una confirmación explícita, porque es el paso que cierra el curso y no tiene vuelta atrás. Sin tildar la casilla vuelve al formulario sin completar nada. El informe se lee de la **variable del proceso**, que es la fuente de verdad mientras la instancia vive, y no de la BD: así la pantalla funciona aunque la sincronización no haya llegado. Verificado contra el stack: al confirmar, la instancia terminó con `resultadoCurso = FINALIZADO`, `aulaLiberada = true` por la compensación y el curso quedó FINALIZADO en la base. **Cierra el escenario 1 de la defensa de punta a punta desde el sitio.** ( )

**Funciones para el Equipo Docente**

- [x] Jefe docente: consultar sus tareas *Definir docente que desarrolla el curso*, **seleccionar uno de los tres docentes (puede elegirse a sí mismo), asignarlo a la tarea *Desarrollar curso*** y completar. Pantalla `/tareas/{id}/definir-docente` con un desplegable de `TeacherRepository.findAvailableTeachers()` (jefedocente, docente1 y docente2). Solo acepta a alguien de esa lista y completa con `docenteSeleccionado`, que el modelo usa como responsable de *Desarrollar curso*. Verificado contra el stack: con docente2 elegido, *Desarrollar curso* le quedó asignada a docente2. `TaskFormSiteControllerTest` cubre las dos pantallas (13 tests) y la bandeja suma un test del enlace; el backend queda en 75 tests pasando. (Agustin Cesari)
- [x] Docente seleccionado: consultar sus tareas *Desarrollar curso* y completarlas. Aparece en su bandeja ya asignada —la asigna el modelo con `docenteSeleccionado`, sin reclamo— y se completa con el botón directo, dejando `cursoDesarrollado`. Verificado: con `docente1` elegido, la tarea le llegó a él y al completarla el proceso pasó a *Elaborar informe*; `docente2` no la ve. ( )

- [x] Completar *Elaborar informe de resultados* desde el sitio. El enunciado lo da como opcional —puede hacerse desde Flowable UI— pero con la infraestructura ya armada salía barato y deja el recorrido completo sin salir del sitio. Pantalla `/tareas/{id}/elaborar-informe`, con la variable `informe` que después lee el jefe administrativo. ( )

> *Solicitar aula virtual*, *Contratar equipamiento* y *Liberar equipamiento* se completan desde la bandeja con el botón directo, o desde Flowable UI: no necesitan pantalla propia. El PDF del informe no es obligatorio (tachado en el enunciado V3).
>
> Con estas dos pantallas **ya no queda ninguna tarea derivada a Flowable UI**. El mecanismo de `TareaVista.necesitaFormulario()` se mantiene igual, como red por si se suma una tarea con datos antes que su pantalla.

---

## 5. App auxiliar: preparación de materiales con la API Java (§4.2.1)

App de escritorio con interfaz gráfica, local a la intranet, con alcance a la BD del motor. **Debe usar la API Java de Flowable**, no la REST.

Vive en [`materiales/`](materiales/), módulo Maven aparte. Sin Spring: arma un `ProcessEngine` embebido contra la misma base del motor ([`EngineFactory`](materiales/src/main/java/com/materiales/app/engine/EngineFactory.java)) y trabaja con `IdentityService` y `TaskService`. Dos recaudos para no pisar al motor: `databaseSchemaUpdate = false` (no crea ni migra tablas; si la versión del esquema no coincide, falla al arrancar) y async executor apagado (no compite por los jobs ni los temporizadores). La conexión se cambia sin recompilar por `-Ddb.url=...`, por `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` o editando `materiales.properties`. Se levanta con `cd materiales && ./build.sh` y `java -jar materiales/target/app.jar`; el paso 9 del [README](README.md) lo explica.

- [x] **Identificarse y consultar las tareas *Preparar materiales*** asignadas o reclamables. Login con usuario y contraseña de Flowable ([`StaffAuthenticator`](materiales/src/main/java/com/materiales/app/service/StaffAuthenticator.java)), que además exige pertenecer a Administración porque la tarea es de ese grupo (§2). La bandeja son dos consultas, `taskAssignee` y `taskCandidateGroupIn` con los grupos que resolvió el login, en vez de `taskCandidateOrAssigned`: así los grupos candidatos son explícitos y no dependen de la resolución implícita del motor. La tabla muestra curso y empresa, que salen de las variables de la instancia (`courseName`, `companyName`, las que escribe el BFF al dar de alta la solicitud). ( )
- [x] **Reclamar** una tarea disponible para su grupo. Si otro integrante la reclamó primero, el motor rechaza el reclamo y la app lo muestra. ( )
- [x] **Completar** la tarea, con un detalle opcional de los materiales que queda en la variable `detalleMateriales` (más `materialesPreparados = true`). La app valida lo que el motor no: una tarea sin reclamar no se puede completar, y una asignada solo la completa su responsable (§2). ( )

**16 tests** ([`materiales/src/test/`](materiales/src/test/java/com/materiales/app/)) sobre un motor embebido en H2, así que corren sin PostgreSQL y sin pantalla: `cd materiales && mvn test`. Cubren login (ok, contraseña mala, usuario inexistente, docente rechazado), bandeja (visible para todo el grupo, y solo para quien la reclamó después del reclamo), reclamo pisado por otro, completado con y sin detalle, y los tres rechazos de validación. La UI Swing no tiene tests automáticos: se prueba a mano.

---

## 6. App auxiliar: simulación de comunicaciones por API REST (§4.2.2)

Permite seleccionar una solicitud y simular las comunicaciones de la empresa.

> **Ojo con la interfaz.** El enunciado permite hacer esto por línea de comandos **solo a los grupos de hasta 3 personas**, y somos 5. Así que se hizo **con interfaz gráfica**, igual que la app de materiales.

Vive en [`comunicaciones/`](comunicaciones/), módulo Maven aparte. Sin Spring y sin la API Java: usa el `HttpClient` del JDK contra `/process-api` y Jackson para los JSON — es la única dependencia de runtime. La empresa no es usuario del motor (§3.3 solo pide usuarios para el personal), así que la app usa la cuenta técnica `admin`/`test`, configurable por `-Dflowable.base-url=...`, por `FLOWABLE_BASE_URL`/`FLOWABLE_USERNAME`/`FLOWABLE_PASSWORD` o editando `comunicaciones.properties`. Se levanta con `cd comunicaciones && ./build.sh` y `java -jar comunicaciones/target/app.jar`; el paso 10 del [README](README.md) lo explica.

- [x] **Seleccionar una solicitud / instancia** sobre la que actuar. La tabla lista las instancias en curso (`GET /runtime/process-instances?processDefinitionKey=Process_Curso&includeProcessVariables=true`, ordenadas por fecha) con curso, empresa y el id del curso en la BD. Una instancia iniciada a mano desde Flowable UI, sin esas variables, se muestra igual por su id. ( )
- [x] **Aceptación** de la propuesta, incluyendo la fecha acordada en `fechaCurso`. Se valida el formato antes de llamar al motor (`AAAA-MM-DD` o `AAAA-MM-DDTHH:MM`), así un error se ve en el momento y no como una instancia trabada en el timer. ( )
- [x] **Rechazo** de la propuesta. Viaja sin variables. ( )
- [x] **Solicitud de modificaciones**, con las observaciones en `observaciones`. ( )
- [x] **Envío de la nómina** de participantes en `participantes`: se escriben uno por línea o separados por comas y se normalizan antes de enviar. ( )
- [x] **Cancelación** durante la preparación. ( )
- [x] Mostrar **solo las comunicaciones habilitadas** en el estado actual. No se deduce con reglas propias: por cada mensaje se le pregunta al motor qué ejecución está suscripta (`GET /runtime/executions?processInstanceId=...&messageEventSubscriptionName=...`), que es la única fuente de verdad y además da el `executionId` al que hay que mandarlo. Esperando respuesta salen aceptar/rechazar/modificaciones; en preparación, nómina y cancelación; durante el dictado, ninguna. ( )

Todas se envían con `PUT /runtime/executions/{executionId}` y `action: messageEventReceived`.

**16 tests** ([`comunicaciones/src/test/`](comunicaciones/src/test/java/com/comunicaciones/app/)) que usan el servidor HTTP del JDK como stub de la API REST, así que corren sin el motor levantado: `cd comunicaciones && mvn test`. Cubren el cliente (HTTP Basic, escapado de los nombres con acento, 204 sin cuerpo, 401, 500, motor caído) y el servicio (parseo de solicitudes, comunicaciones habilitadas en cada estado, cuerpo exacto de cada mensaje y las dos validaciones locales). Los nombres de parámetro de la API REST se verificaron contra `flowable-rest-6.8.0.jar`, no de memoria. La UI Swing no tiene tests automáticos: se prueba a mano.

> Los nombres de mensaje y los `curl` de referencia están en la sección "Probar la API REST" del [README](README.md).

---

## 7. Análisis de las ejecuciones del proceso (§4.3)

Sobre la información histórica de Flowable. El enunciado permite integrarlo a la app del bloque 5 o mostrarlo por consola: están **las dos**. Vive en [`materiales/src/main/java/com/materiales/app/analysis/`](materiales/src/main/java/com/materiales/app/analysis/), con [`ExecutionAnalysisService`](materiales/src/main/java/com/materiales/app/analysis/ExecutionAnalysisService.java) haciendo las consultas por la API Java (`HistoryService`), un formateador de tablas compartido, el botón *Análisis de ejecuciones* en la ventana de la app y [`AnalysisApp`](materiales/src/main/java/com/materiales/app/analysis/AnalysisApp.java) para la terminal:

```bash
java -cp materiales/target/app.jar com.materiales.app.analysis.AnalysisApp
```

- [x] **Resultados de las solicitudes**. Sale de la variable `resultadoCurso` de cada instancia terminada (`finished()` + `includeProcessVariables()`), la que dejan los caminos de cierre de §3.4. Porcentajes sobre el total de terminadas, con las cinco filas siempre presentes aunque estén en cero. Si apareciera una instancia terminada sin resultado se muestra en una fila aparte, pero `ClosingPathsTest` garantiza que no pase. ( )
- [x] **Análisis de la preparación paralela**. La activación es el fin del gateway `preparacionenparalelo`, la misma para las tres ramas; el cierre de cada una es la última finalización de `confeccionarlistadefinitiva`, `prepararmateriales` y —para recursos— la más tardía entre `reservaraula`, `solicitaraulavirtual` y `contratarequipamiento`, que es el "cuando se hayan completado todas las acciones seleccionadas" del enunciado: las no seleccionadas no están en el historial. Se toma la **última** finalización porque `reservaraula` se repite si hubo que gestionar la falta de aulas. Solo se miden las preparaciones que llegaron a `finprepararcurso`; una cancelación interrumpe el subproceso antes, así que queda excluida sola, y el informe aclara cuántas excluyó por cancelación, cuántas están en curso y cuántas instancias nunca entraron en preparación. ( )
- [x] **Distribución de tareas**. `finished()` + `taskWithoutDeleteReason()` agrupado por responsable: eso deja afuera las pendientes y las que el motor borra al interrumpir la preparación. Los cinco integrantes aparecen siempre, aunque tengan cero, y si apareciera un responsable fuera de los dos grupos se lista al final. ( )

**10 tests** ([`materiales/src/test/java/com/materiales/app/analysis/`](materiales/src/test/java/com/materiales/app/analysis/)), 26 en total en el módulo. Los de las consultas corren contra un esqueleto BPMN de test que copia del modelo real los ids de las actividades, el subproceso de preparación con sus tres ramas y el subproceso de evento de cancelación, y **mueven el reloj del motor a mano**, así las duraciones salen exactas (5, 10 y 20 minutos) y no dependen de lo que tarde la máquina. Montan tres instancias: una preparación normal, una cancelada y una solicitud que nunca entró en preparación.

> Probado también contra la base real del stack: el informe corre y lee el historial. **Ojo con dos cosas que se ven ahí:** aparece `administrativo2`, el usuario huérfano de la base vieja (hay que recrearla con `docker compose down -v`), y con solo `seed-demo.sh` no hay ninguna instancia terminada, así que las dos primeras tablas salen vacías. Para llenarlas hay que cerrar instancias con la app del bloque 6.

---

## 8. Introducción a la minería de procesos (§4.4)

Completo. Todo vive en [`mineria/`](mineria/) y la conclusión está en
[`mineria/conclusion.md`](mineria/conclusion.md).

```bash
sudo apt install python3-venv graphviz
python3 -m venv .venv && .venv/bin/pip install pm4py pandas requests psycopg2-binary
```

- [x] **Generar el CSV** a partir de las actividades históricas. Los scripts que menciona el enunciado no estaban en el repo, así que se escribieron: [`exportar_history.py`](mineria/exportar_history.py) arma el event log con los nombres de columna que espera pm4py y recorta la **etapa de negociación**, que es lo que pide el análisis. **Lee de la base, no de la API REST**, y la razón importa: el modelo usa una compuerta basada en eventos, así que cada instancia que espera respuesta deja registradas las cuatro ramas (aceptación, rechazo, modificaciones y el plazo) aunque solo una haya ocurrido; las canceladas se distinguen solo por `DELETE_REASON_`, un campo que el REST no expone. Sin filtrarlas el log pasa de 6 a 19 recorridos y deja de significar algo. Queda `--fuente rest` como alternativa, avisando de la limitación. Se descartan además las negociaciones todavía abiertas, que no son un camino sino una foto a mitad de camino. ( )
- [x] **Datos para el análisis.** [`generar_casos.py`](mineria/generar_casos.py) crea instancias recorriendo a propósito los cinco caminos de la negociación, dando de alta por el sitio y disparando los mensajes y temporizadores por la API REST. Aparecieron dos cosas al usarlo: el límite de cinco cursos (§3.1) hace que las solicitudes nuevas salgan no viables si no hay cupo —correcto, pero hay que tenerlo en cuenta—, y los temporizadores encadenados necesitan reintento, porque el motor tarda un instante en crear el siguiente job y sin esperar solo se dispara el primer recordatorio. ( )
- [x] **Representación gráfica y los tres recorridos más frecuentes.** [`analizar.py`](mineria/analizar.py) genera el DFG y la red heurística en `mineria/resultados/`, e imprime los recorridos con cantidad y porcentaje. Sobre 44 instancias salieron **6 recorridos distintos**; los tres más frecuentes cubren el **77,3 %**: no viable (34,1 %), rechazo de la propuesta (29,5 %) y cierre por falta de respuesta (13,6 %). ( )
- [x] **Conclusión** comparando con el modelo BPMN ([`conclusion.md`](mineria/conclusion.md)): coincidencias, diferencias y limitaciones. ( )

### 🔴 Lo que encontró la minería

El análisis detectó que **el proceso envía tres recordatorios en vez de dos**. El enunciado (§1.1) dice *"se enviarán como máximo dos recordatorios"*, pero el recorrido observado muestra tres envíos en el 100 % de los casos sin respuesta. La causa está en la compuerta `seenviaron2recordatorios`, que evalúa `${recordatorios <= 2}` después de incrementar el contador; debería ser `${recordatorios < 2}` / `${recordatorios >= 2}`.

**Ningún test lo detectaba**: `NoResponseTest` verifica que la solicitud termine archivada, que es lo que pasa, pero no cuenta las vueltas del ciclo.

- [ ] Corregir las condiciones de la compuerta y agregar a `NoResponseTest` una aserción sobre la cantidad de recordatorios. Después conviene regenerar el log para que el entregable refleje el modelo corregido. ( )

---

## 9. Entrega y demostración (§5)

**Material a entregar**

- [ ] Modelo BPMN ejecutable. *(listo)*
- [ ] Código del servicio REST y de los componentes incorporados al motor. *(listo)*
- [ ] Reglas de negocio. *(listo — DMN de viabilidad)*
- [ ] Instrucciones para configurar usuarios y grupos y ejecutar la solución. *(listo — [README.md](README.md))*
- [ ] **Informe breve** explicando variables, listeners y decisiones de implementación. Hay material en [`model/`](model/) y [`adr/`](adr/) para reusar. ( )
- [ ] **Código de las tres aplicaciones externas** más las instrucciones para configurarlas y ejecutarlas. Dos de tres listas: [`materiales/`](materiales/) y [`comunicaciones/`](comunicaciones/), con sus pasos 9 y 10 en el [README](README.md). Falta el sitio web del bloque 4. ( )

**Escenarios de la defensa**

Los 8 primeros ya tienen test automatizado en [`flowable/src/test/java/com/flowable/`](flowable/src/test/java/com/flowable/); falta poder demostrarlos **en vivo desde las aplicaciones**.

- [x] 1. Aceptación y finalización normal del curso, con liberación de recursos. — `HappyPathTest`
- [x] 2. Solicitud de modificaciones y envío de una nueva propuesta. — `ProposalModificationsTest`
- [x] 3. Rechazo de la propuesta. — `ProposalRejectionTest`
- [x] 4. Cierre por falta de respuesta, luego de los recordatorios. — `NoResponseTest`
- [x] 5. Imposibilidad de continuar por el límite de cursos en ejecución. — `CourseLimitExceededTest`
- [x] 6. Cancelación durante la preparación, con compensación de los recursos gestionados. — `CompensationTest`
- [x] 7. Falta de aulas disponibles y generación de la excepción. — `ClassroomUnavailableTest`
- [x] 8. Asignación, reclamo y finalización de tareas según los responsables. — `TaskAssignmentTest`
- [ ] 9. **Registro de una solicitud desde el sitio web e inicio de su instancia.** → bloque 4 ( )
- [ ] 10. **Habilitación del acceso de la empresa y consulta exclusiva de sus cursos.** → bloques 1 y 4 ( )
- [x] 11. **Preparación de materiales por API Java y simulación de comunicaciones por API REST.** Las dos apps están listas (bloques 5 y 6), con 32 tests entre ambas. **Falta probarlo en vivo**: las interfaces Swing no se ejecutaron contra el stack levantado, solo sus capas de servicio. Es parte del ensayo de la demo. ( )

- [ ] **Ensayo completo de la demo** con el stack levantado desde cero, siguiendo el README paso a paso. ( )

---

## 10. Entorno y calidad de vida

- [ ] **Mover el repositorio a una ruta sin apóstrofes.** Si la ruta absoluta contiene un `'` (por ejemplo `.../Computer's Science/...`), `mvn test` falla siempre con `The forked VM terminated without properly saying goodbye`, porque Maven Surefire arma mal el comando del JVM hijo. Workaround actual: `mvn test -DforkCount=0`. ( )
- [ ] Revisar el `pom.xml`: el bloque `<profiles>` está vacío, así que el `mvn compile exec:java -Pdev` que figuraba en documentación vieja no existe. Decidir si se agrega el perfil o se descarta la idea. ( )
- [ ] Script o comando único que levante **todo** el stack (motor + UI + BFF + sitio web) para la defensa. ( )
- [ ] Definir si el BPMN de referencia es [`model/cursosNuestraVersion.bpmn`](model/cursosNuestraVersion.bpmn) o el desplegado en [`flowable/src/main/resources/processes/`](flowable/src/main/resources/processes/), y mantenerlos sincronizados. ( )

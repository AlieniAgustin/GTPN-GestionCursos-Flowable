# TODO — Lo que falta implementar

Checklist del equipo, derivada del enunciado [`docs/ProyectoCursos_V3.pdf`](docs/ProyectoCursos_V3.pdf) contrastada con lo que hoy está en el repositorio.

**Cómo usarla:** marcá el `[x]` cuando el ítem esté hecho **y probado**, y poné tu nombre entre paréntesis al final de la línea. Si un ítem te queda grande, partilo en sub-ítems antes de empezar. Las referencias `§` son secciones del PDF.

**Integrantes:** Agustin Alieni · Agustin Cesari · Damian Dalio · Hernan Jara · Francisco Natale

---

## Resumen

| Bloque | Estado |
|---|---|
| 1. Motor y modelo BPMN | Completo |
| 2. BD de la institución | Persistente y vinculada al proceso — falta completar campos y datos de prueba |
| 3. Backend BFF | Operativo — faltan tests y endpoints de consulta |
| 4. Sitio web institucional | No empezado |
| 5. App Java (API Java) | No empezado |
| 6. App de comunicaciones (API REST) | No empezado |
| 7. Análisis de ejecuciones | No empezado |
| 8. Minería de procesos | No empezado |
| 9. Entrega y demo | Escenarios 1–8 cubiertos por tests; 9–11 dependen de las apps |

---

## 1. Motor y modelo BPMN

Completo: el BPMN ejecutable, el DMN de viabilidad, los listeners de §3.4, los delegates de aulas y de acceso de la empresa, los plazos parametrizables de §3.5 y la seguridad por grupos.

- [x] **Ajustar la cantidad de usuarios del seed** (§3.3). El seed (hoy en [`IdentitySeeder.java`](flowable/src/main/java/com/flowable/app/config/IdentitySeeder.java)) creaba 6 usuarios; ahora crea los **5** que pide el enunciado: `jefeadministrativo` y `administrativo1` en Administración, `jefedocente`, `docente1` y `docente2` en Docentes. Se eliminó `administrativo2`. El BPMN no lo referenciaba (las tareas usan `candidateGroups="administracion"` o asignación directa al jefe), pero `TaskAssignmentTest` sí: se adaptó para que el menos ocupado sea `jefeadministrativo`. Los 11 tests siguen pasando. **Ojo:** el seed no borra usuarios, así que una base creada antes de este cambio conserva el `administrativo2` huérfano — hay que recrearla con `docker compose down -v`. ( )
- [x] **Hacer que el seed corra en `prod`** (§3.3). El seed salió de `App.java` y ahora vive en [`IdentitySeeder.java`](flowable/src/main/java/com/flowable/app/config/IdentitySeeder.java), **sin `@Profile`**: corre en cada arranque y en cualquier perfil, así que el stack Docker ya levanta con los cinco usuarios, los dos grupos y la cuenta técnica `admin`/`test`. Se puede desactivar con `institucion.seed.enabled=false`. Es idempotente y además repara membresías faltantes (antes, si el usuario existía pero no estaba en el grupo, quedaba así), y loguea cada alta para verificarlo con `docker compose logs flowable-app`. Se eliminó el workaround de arrancar la app local (paso 1.3 del README). `IdentitySeederTest` lo verifica con perfil `prod`; 12 tests pasando. ( )
- [x] **Implementar la actividad automática *Habilitar acceso de la empresa*** (§4.1.2). En el BPMN pasó de un `scriptTask` con un `println` a un service task con [`EnableCompanyAccessDelegate`](flowable/src/main/java/com/flowable/app/delegates/EnableCompanyAccessDelegate.java), que llama al BFF (`POST /api/flowable/courses/{processId}/company-access`). Del otro lado, [`CompanyAccessService`](backend/src/main/java/com/backend/proxy/service/CompanyAccessService.java) busca el curso por `processInstanceId`, toma su empresa y, si todavía no tiene cuenta, le crea una: usuario derivado de la razón social (desambiguado con el id si ya estaba tomado) y contraseña random guardada con BCrypt en las columnas nuevas `access_username` / `access_password_hash` de `companies`. Si la empresa ya contrató antes, se reutiliza la cuenta y el curso nuevo queda vinculado a ella por su `company_id`. El usuario queda en la variable `empresaUsuario` y, solo cuando la cuenta se acaba de crear, la contraseña en `empresaPasswordTemporal`: el enunciado permite simular la comunicación de las credenciales, así que se leen de la instancia o del log. Si el BFF no responde, la instancia sigue con `empresaAccesoHabilitado = false` en vez de trabarse. `CompanyAccessTest` cubre los tres casos (creación, reutilización, backend caído). **Falta** la pantalla de login de la empresa, que es del bloque 4. ( )
- [x] **Verificar que todos los caminos de cierre dejan seteado el resultado** (§3.4). Verificado: los cinco caminos ya marcaban `resultadoCurso` y no hay ninguno suelto. Para que siga siendo así, [`ClosingPathsTest`](flowable/src/test/java/com/flowable/ClosingPathsTest.java) lee el modelo desplegado y, por cada evento de fin del proceso, camina hacia atrás por los flujos comprobando que **todos** los caminos que llegan ahí pasan por un listener que setea la variable; un segundo test exige que los resultados del modelo sean exactamente los cinco del enunciado. Probado sabotear un flujo: el test falla indicando el evento de fin alcanzable sin resultado. Además se completaron las aserciones que faltaban: `NO_VIABLE` en `CourseLimitExceededTest` (los otros cuatro ya estaban) y, en `HappyPathTest`, que el `UpdateCourseStatusListener` efectivamente le manda el estado al BFF. ( )
- [x] **Revisar los plazos para la demo** (§3.5). Parametrizado y documentado (sección "Acortar los plazos para la demo" del [README](README.md)). Dos properties nuevas con sus variables de entorno: `proceso.plazo-respuesta` (default `P5D`, lo aplica [`ApplyResponseDeadlineListener`](flowable/src/main/java/com/flowable/app/listeners/ApplyResponseDeadlineListener.java) al entrar a *Enviar propuesta*) y `proceso.fecha-curso-offset` (default `P7D`, para cuando la empresa no manda fecha). En [`docker-compose.yml`](docker-compose.yml) quedaron las dos variables comentadas, listas para la demo. El plazo por instancia le gana a la property, así que se puede iniciar una solicitud con `plazoRespuesta` propio. El BPMN no tiene ninguna duración ni fecha fija: sigue usando `${plazoRespuesta}` y `${fechaCurso}`. ( )
- [x] **Definir `fechaCurso` durante la ejecución** (§3.5). Lo resuelve [`ResolveCourseDateListener`](flowable/src/main/java/com/flowable/app/listeners/ResolveCourseDateListener.java), que corre al tomar el flujo que sale de *Aceptación*: toma la fecha que trajo el mensaje en `fechaCurso` o en `fechaAcordada` (el nombre que usa la app de comunicaciones de §4.2.2), la normaliza a ISO-8601 con offset — acepta `Date`, `2026-03-01`, `2026-03-01T09:00` y `2026-03-01T09:00-03:00` — y, si no llegó nada usable, la deriva de `proceso.fecha-curso-offset` y lo avisa en el log. Así la variable **siempre** está definida antes del timer y no vuelve a aparecer `Unknown property used in expression: ${fechaCurso}`. `CourseDateTest` cubre los tres casos, incluido el happy path completo **sin** inyectar la fecha al iniciar la instancia, que es el que fallaba. ( )

---

## 2. Base de datos de la institución (§3.2)

Las entidades `Company` y `Course` viven en [`backend/src/main/java/com/backend/proxy/entity/`](backend/src/main/java/com/backend/proxy/entity/).

- [x] **Vincular cada curso con su instancia de proceso**. `POST /api/courses` da de alta empresa y curso, inicia la instancia y guarda el `processInstanceId` en la fila del curso. El sentido inverso ya andaba: el motor avisa el estado final por `PUT /api/flowable/courses/{processId}`. ( )
- [x] **Reemplazar H2 en memoria por algo persistente.** El BFF pasó a PostgreSQL, compartiendo el servidor del stack: el motor usa las tablas `ACT_*` y la institución `companies` / `courses`. Esto además era un **bug bloqueante**: `Course.possibleDates` se mapea con `columnDefinition = "jsonb"`, que H2 no conoce, así que el `create table courses` fallaba en silencio al arrancar y cualquier alta moría con `Table "COURSES" not found`. ( )
- [ ] Guardar el resto de la **información del curso**: programa, presupuesto, fechas acordadas e informe de resultados. Hoy se persisten nombre, descripción, fechas posibles y empresa. ( )
- [ ] Decidir y documentar qué queda como **variable del proceso** y qué va a la BD. El enunciado permite mantener la lista de participantes y los recursos asignados como variables. ( )
- [ ] Scripts de **creación y datos de prueba**. ( )

---

## 3. Backend BFF

El BFF ya autentica y opera contra el motor. Detalle en [`backend/README.md`](backend/README.md).

- [x] **Crear el `AuthController`** con `POST /api/auth/login`, `POST /api/auth/logout` y `GET /api/auth/me`. Valida contra el `/me` de Flowable con el `FlowableAuthService` que ya existía y deja el SecurityContext en la `HttpSession`. ( )
- [x] **Cablear un mecanismo de autenticación.** La cadena principal usa `HttpSessionSecurityContextRepository`, así que después del login alcanza con la cookie `JSESSIONID`. Sin sesión la API devuelve **401** (antes 403), que es lo correcto para una API. ( )
- [x] **Corregir `CourseController`**: el `@PathVariable processId` que no existía en la ruta se eliminó; el endpoint quedó como `POST /api/courses` y devuelve `201` con `Location` y un `CourseResponseDTO` (no la entidad JPA, para no serializar la asociación lazy `company`). ( )
- [x] **Implementar `TaskService` y las cuatro `TaskStrategy`.** `GET /api/tasks/{id}/detail` lee la tarea y las variables de la instancia en Flowable, resuelve la estrategia por `taskDefinitionKey` y devuelve el payload tipado con su `actionUrl`. Si ninguna estrategia aplica, 404 con el motivo. `complete()` arma el payload de Flowable y lo postea. También se completó `TeacherRepositoryStub` con los tres docentes de §2. ( )
- [x] **`/error` alcanzable sin autenticación**, así los errores salen con el formato del `GlobalExceptionHandler` en vez de convertirse en un 403 opaco. ( )
- [x] **Backend agregado a [`docker-compose.yml`](docker-compose.yml)** con su `Dockerfile` y `build.sh`, healthcheck sobre un `/health` nuevo, y resolución por nombre de servicio en ambos sentidos (`BACKEND_BASE_URL` en el motor, `FLOWABLE_BASE_URL` en el BFF). ( )
- [ ] **Tests del backend**: hoy el módulo no tiene ninguno (`No tests to run`). Faltan al menos los del login, el alta de curso y la resolución de estrategias. ( )
- [ ] Usar `@AuthenticationPrincipal` en `TaskController` para filtrar las tareas por usuario, como describe el checklist del [`backend/README.md`](backend/README.md). Hoy las consultas no pasan el usuario a Flowable. ( )
- [ ] Endpoints de **consulta** para empresas y administrativos (`GET /api/courses`, bandeja de tareas). Solo existe el alta. ( )

---

## 4. Sitio web de la institución (§4.1)

No empezado. Interactúa con Flowable vía API REST.

**Sección pública**

- [ ] Formulario de **solicitud de capacitación** con: datos de la empresa y del contacto, temática y descripción, cantidad estimada de participantes y fechas preferidas. Los DTOs de validación ya existen (`CourseRequestDTO`, `CompanyContactDTO`). ( )
- [ ] Al enviarlo: **registrar la información e iniciar la instancia** del proceso (mensaje `Solicitud de curso`). ( )

**Acceso de empresas**

- [ ] Login de la empresa con las credenciales creadas por *Habilitar acceso* (bloque 1). ( )
- [ ] **Consultar sus cursos** — solo los propios. ( )
- [ ] **Ver el estado** de un curso: en preparación, confirmado y pendiente de inicio, en desarrollo, en elaboración del informe, finalizado o cancelado. ( )
- [ ] Durante la preparación, **listar las actividades pendientes o en ejecución**, contemplando que varias corren en paralelo. ( )

**Funciones para Administración** (ingresan con su usuario y contraseña de Flowable, §3.3)

- [ ] Consultar **tareas asignadas y disponibles para su grupo**. ( )
- [ ] **Reclamar y completar** tareas respetando las responsabilidades. Las asignadas directamente a un usuario no requieren reclamo. ( )
- [ ] Completar *Registrar y elaborar propuesta* con formulario y **generar el PDF** de la propuesta. ( )
- [ ] Completar *Revisar propuesta* **generando una nueva versión del PDF**. ( )
- [ ] Completar *Archivar solicitud*. ( )
- [ ] Completar *Confeccionar lista definitiva*. ( )
- [ ] Completar *Evaluar recursos* con una pantalla que permita elegir aula física, aula virtual y/o equipamiento especial — **al menos una alternativa**. ( )
- [ ] Permitir al jefe administrativo ver el informe y completar *Enviar informe a empresa y archivar expediente*. ( )

**Funciones para el Equipo Docente**

- [ ] Jefe docente: consultar sus tareas *Definir docente que desarrolla el curso*, **seleccionar uno de los tres docentes (puede elegirse a sí mismo), asignarlo a la tarea *Desarrollar curso*** y completar. ( )
- [ ] Docente seleccionado: consultar sus tareas *Desarrollar curso* y completarlas. ( )

> *Solicitar aula virtual*, *Contratar equipamiento* y *Liberar equipamiento* pueden hacerse desde Flowable UI, no hace falta pantalla propia. *Elaborar informe de resultados* también puede hacerse desde Flowable UI (el PDF del informe ya no es obligatorio).

---

## 5. App auxiliar: preparación de materiales con la API Java (§4.2.1)

App de escritorio con interfaz gráfica, local a la intranet, con alcance a la BD del motor. **Debe usar la API Java de Flowable**, no la REST.

- [ ] Identificarse y **consultar las tareas *Preparar materiales*** asignadas o reclamables. ( )
- [ ] **Reclamar** una tarea disponible para su grupo. ( )
- [ ] **Completar** la tarea. ( )

---

## 6. App auxiliar: simulación de comunicaciones por API REST (§4.2.2)

Permite seleccionar una solicitud y simular las comunicaciones de la empresa. **Puede ser por línea de comandos**, no requiere interfaz gráfica.

- [ ] Seleccionar una solicitud / instancia sobre la que actuar. ( )
- [ ] **Aceptación** de la propuesta, incluyendo la fecha acordada. ( )
- [ ] **Rechazo** de la propuesta. ( )
- [ ] **Solicitud de modificaciones**, incluyendo las observaciones. ( )
- [ ] **Envío de la nómina** de participantes. ( )
- [ ] **Cancelación** durante la preparación. ( )
- [ ] Mostrar **solo las comunicaciones habilitadas** en el estado actual de la instancia. ( )

> Los nombres de mensaje y los `curl` de referencia están en la sección "Probar la API REST" del [README](README.md).

---

## 7. Análisis de las ejecuciones del proceso (§4.3)

Sobre la información histórica de Flowable. Puede integrarse a la app del bloque 5 o **mostrarse por consola**.

- [ ] **Resultados de las solicitudes**: tabla con cantidad y porcentaje de instancias terminadas en cada resultado (curso finalizado, cancelación, rechazo, no viable, cierre por falta de respuesta). Porcentajes sobre el total de instancias **terminadas**. ( )
- [ ] **Análisis de la preparación paralela**: duración promedio de las ramas de inscripciones, materiales y recursos, medida desde su activación hasta su finalización. La rama de recursos termina cuando se completaron todas las acciones seleccionadas. **Excluir las preparaciones interrumpidas por cancelación.** ( )
- [ ] **Distribución de tareas**: tabla con la cantidad de tareas de usuario completadas por cada administrativo y docente. **Excluir las pendientes y las interrumpidas o canceladas.** ( )

---

## 8. Introducción a la minería de procesos (§4.4)

Análisis con PM4Py de la **etapa de negociación** (todo lo previo a la preparación del curso).

```bash
sudo apt install python3-venv graphviz
python3 -m venv .venv
source .venv/bin/activate
pip install pm4py pandas requests
```

- [ ] **Generar el CSV** a partir de las actividades históricas de Flowable, adaptando el script provisto `exportar_history.py` al proceso implementado. ( )
- [ ] **Obtener la representación gráfica** del comportamiento observado e identificar los **tres recorridos más frecuentes**, con cantidad y porcentaje de instancias de cada uno. Si hay menos de tres recorridos distintos, mostrar todos. Se puede usar el script provisto `analizar.py`. ( )
- [ ] **Redactar la conclusión**: comparar el resultado con la etapa de negociación del modelo BPMN — coincidencias, diferencias y limitaciones del análisis. ( )
- [ ] Entregar el CSV, el código o las instrucciones para reproducir el análisis, y una **captura de los resultados**. ( )

> Hace falta generar bastantes instancias terminadas con recorridos variados antes de que el análisis sea interesante. La app del bloque 6 sirve para eso.

---

## 9. Entrega y demostración (§5)

**Material a entregar**

- [ ] Modelo BPMN ejecutable. *(listo)*
- [ ] Código del servicio REST y de los componentes incorporados al motor. *(listo)*
- [ ] Reglas de negocio. *(listo — DMN de viabilidad)*
- [ ] Instrucciones para configurar usuarios y grupos y ejecutar la solución. *(listo — [README.md](README.md))*
- [ ] **Informe breve** explicando variables, listeners y decisiones de implementación. Hay material en [`model/`](model/) y [`adr/`](adr/) para reusar. ( )
- [ ] **Código de las tres aplicaciones externas** más las instrucciones para configurarlas y ejecutarlas. ( )

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
- [ ] 11. **Preparación de materiales por API Java y simulación de comunicaciones por API REST.** → bloques 5 y 6 ( )

- [ ] **Ensayo completo de la demo** con el stack levantado desde cero, siguiendo el README paso a paso. ( )

---

## 10. Entorno y calidad de vida

- [ ] **Mover el repositorio a una ruta sin apóstrofes.** Si la ruta absoluta contiene un `'` (por ejemplo `.../Computer's Science/...`), `mvn test` falla siempre con `The forked VM terminated without properly saying goodbye`, porque Maven Surefire arma mal el comando del JVM hijo. Workaround actual: `mvn test -DforkCount=0`. ( )
- [ ] Revisar el `pom.xml`: el bloque `<profiles>` está vacío, así que el `mvn compile exec:java -Pdev` que figuraba en documentación vieja no existe. Decidir si se agrega el perfil o se descarta la idea. ( )
- [ ] Script o comando único que levante **todo** el stack (motor + UI + BFF + sitio web) para la defensa. ( )
- [ ] Definir si el BPMN de referencia es [`model/cursosNuestraVersion.bpmn`](model/cursosNuestraVersion.bpmn) o el desplegado en [`flowable/src/main/resources/processes/`](flowable/src/main/resources/processes/), y mantenerlos sincronizados. ( )

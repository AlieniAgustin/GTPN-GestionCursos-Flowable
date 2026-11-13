# TODO — Lo que falta implementar

Checklist del equipo, derivada del enunciado [`docs/ProyectoCursos_V3.pdf`](docs/ProyectoCursos_V3.pdf) contrastada con lo que hoy está en el repositorio.

**Cómo usarla:** marcá el `[x]` cuando el ítem esté hecho **y probado**, y poné tu nombre entre paréntesis al final de la línea. Si un ítem te queda grande, partilo en sub-ítems antes de empezar. Las referencias `§` son secciones del PDF.

**Integrantes:** Agustin Alieni · Agustin Cesari · Damian Dalio · Hernan Jara · Francisco Natale

---

## Resumen

| Bloque | Estado |
|---|---|
| 1. Motor y modelo BPMN | Casi completo — ajustes puntuales |
| 2. BD de la institución | A medias — entidades sí, cableado no |
| 3. Backend BFF | Bloqueante — no se puede autenticar |
| 4. Sitio web institucional | No empezado |
| 5. App Java (API Java) | No empezado |
| 6. App de comunicaciones (API REST) | No empezado |
| 7. Análisis de ejecuciones | No empezado |
| 8. Minería de procesos | No empezado |
| 9. Entrega y demo | Escenarios 1–8 cubiertos por tests; 9–11 dependen de las apps |

---

## 1. Motor y modelo BPMN

Lo grueso ya está: el BPMN ejecutable, el DMN de viabilidad, los dos listeners que pide §3.4, los delegates de aulas y la seguridad por grupos. Queda esto:

- [ ] **Ajustar la cantidad de usuarios del seed** (§3.3). El enunciado pide **5** personas: 2 de Administración (una es el jefe administrativo) y 3 de Docentes (uno es el jefe del equipo). El seed de [`App.java`](flowable/src/main/java/com/flowable/app/App.java) crea **6**: sobra `administrativo2`. Revisar también que ninguna tarea del BPMN lo referencie. ( )
- [ ] **Hacer que el seed corra en `prod`** o moverlo a un script de inicialización. Hoy está anotado `@Profile("dev")`, así que el despliegue Docker arranca sin usuarios ni grupos y hay que levantar la app local una vez como workaround (paso 1.3 del README). Para la defensa esto tiene que funcionar de una. ( )
- [ ] **Implementar la actividad automática *Habilitar acceso de la empresa*** (§4.1.2): crea una cuenta para la empresa si no tiene, o reutiliza la existente si ya contrató antes, y vincula el nuevo curso a esa cuenta. ( )
- [ ] **Verificar que todos los caminos de cierre dejan seteado el resultado** del proceso (§3.4), para que el `UpdateCourseStatusListener` registre el estado correcto: `FINALIZADO`, `CANCELADO`, `RECHAZADO`, `SIN_RESPUESTA`, `NO_VIABLE`. ( )
- [ ] **Revisar los plazos para la demo** (§3.5): dejar documentado (o parametrizado) cómo acortar los 5 días de los recordatorios y la fecha de inicio del curso para poder mostrar los escenarios en vivo. ( )

---

## 2. Base de datos de la institución (§3.2)

Las entidades `Company` y `Course` ya existen en [`backend/src/main/java/com/backend/proxy/entity/`](backend/src/main/java/com/backend/proxy/entity/), pero no están cableadas al flujo.

- [ ] Guardar la **empresa solicitante** y la **información del curso**: nombre, descripción, programa, presupuesto, fechas e informe de resultados. ( )
- [ ] **Vincular cada curso con su instancia de proceso** en Flowable (guardar el `processInstanceId` en la BD de la institución, como sugiere el enunciado). ( )
- [ ] Decidir y documentar qué queda como **variable del proceso** y qué va a la BD. El enunciado permite mantener la lista de participantes y los recursos asignados como variables. ( )
- [ ] Reemplazar H2 en memoria por algo **persistente** para la demo (hoy la BD del BFF se pierde en cada reinicio). ( )
- [ ] Scripts de **creación y datos de prueba**. ( )

---

## 3. Backend BFF

Tres defectos verificados que bloquean el resto. Detalle en [`backend/README.md`](backend/README.md).

- [ ] **Crear el `AuthController`** con `POST /api/auth/login` y `POST /api/auth/logout`. La ruta ya está en la lista de URLs públicas de `SecurityConfig` pero **el endpoint no existe**. El `FlowableAuthService` que valida contra `/me` ya está escrito. ( )
- [ ] **Cablear un mecanismo de autenticación** en la cadena de seguridad principal de [`SecurityConfig.java`](backend/src/main/java/com/backend/proxy/security/SecurityConfig.java). Hoy exige `.anyRequest().authenticated()` sin `httpBasic` ni `formLogin`, así que `/api/courses` y `/api/tasks` devuelven **403 siempre**, para cualquier usuario. ( )
- [ ] **Corregir `CourseController.updateStatus`**: declara `@PathVariable String processId` pero el `@PostMapping` no tiene esa variable en la ruta. Falla en runtime. ( )
- [ ] **Implementar los métodos de `TaskService` y `CourseService`** — los controladores tienen los pasos escritos como comentarios `IMPLEMENTATION STEPS`. ( )
- [ ] Permitir que el `/error` sea alcanzable sin autenticación, para que los errores salgan con el formato `ApiErrorResponse` en vez de convertirse en un 403 opaco. ( )
- [ ] **Agregar el backend a [`docker-compose.yml`](docker-compose.yml)** para que todo el stack levante con un solo comando. ( )

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

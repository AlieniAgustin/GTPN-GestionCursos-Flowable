# Informe del proyecto — índice y reparto de la defensa

Informe que pide §5 del enunciado ("un informe breve que explique las variables, los listeners y las decisiones de implementación"), partido por etapas. Cada archivo cubre una etapa, dice **quién la explica** y termina con las preguntas que puede traer.

- **Enunciado:** [`docs/ProyectoCursos_V3.pdf`](../ProyectoCursos_V3.pdf)
- **Estado del proyecto por bloque:** [`TODO.md`](../../TODO.md)
- **Cómo levantar y probar todo:** [`README.md`](../../README.md)

Los documentos no repiten lo que ya está escrito: cuando una decisión tiene su ADR o su documento de diseño, lo enlazan.

---

## Reparto de la exposición

Somos cuatro: **Fran**, **Cesari**, **Alieni** y **Damián**.

| Quién | Documentos | Tema en una línea |
|---|---|---|
| **Fran** | [07](07-backend-de-la-institucion.md), [08](08-datos-de-la-empresa-y-acceso.md) | El backend de la institución, y qué dato se guarda, cuándo se actualiza y qué ve cada uno |
| **Damián** | [01](01-modelo-y-motor.md), [06](06-variables-listeners-y-decisiones.md), [10](10-apps-auxiliares.md) | El modelo y el motor, las variables y los listeners, las dos apps de escritorio |
| **Alieni** | [02](02-viabilidad-y-acuerdo.md), [04](04-cancelacion-y-compensacion.md), [05](05-desarrollo-y-finalizacion.md) | Viabilidad y negociación, cancelación con compensación, dictado y cierre |
| **Cesari** | [03](03-preparacion-del-curso.md), [09](09-sitio-web.md), [11](11-analisis-y-mineria.md) | La preparación en paralelo, el sitio web, el análisis y la minería |

Si alguno quiere cambiar, lo único fijo es que Fran lleva 07 y 08.

---

## Los documentos

| # | Documento | Secciones del enunciado | Explica |
|---|---|---|---|
| 01 | [Modelo BPMN y motor](01-modelo-y-motor.md) | §1.5, §2, §3.3 | Damián |
| 02 | [Viabilidad y acuerdo](02-viabilidad-y-acuerdo.md) | §1.1, §3.1 | Alieni |
| 03 | [Preparación del curso](03-preparacion-del-curso.md) | §1.2 | Cesari |
| 04 | [Cancelación y compensación](04-cancelacion-y-compensacion.md) | §1.3 | Alieni |
| 05 | [Desarrollo y finalización](05-desarrollo-y-finalizacion.md) | §1.4 | Alieni |
| 06 | [Variables, listeners y decisiones](06-variables-listeners-y-decisiones.md) | §3.4, §3.5, §5 | Damián |
| 07 | [Backend de la institución](07-backend-de-la-institucion.md) | §4.1 (infraestructura), §3.3 | Fran |
| 08 | [Datos de la empresa y su acceso](08-datos-de-la-empresa-y-acceso.md) | §3.2, §4.1.2 | Fran |
| 09 | [Sitio web de la institución](09-sitio-web.md) | §4.1.1, §4.1.3, §4.1.4 | Cesari |
| 10 | [Apps auxiliares](10-apps-auxiliares.md) | §4.2.1, §4.2.2 | Damián |
| 11 | [Análisis de ejecuciones y minería](11-analisis-y-mineria.md) | §4.3, §4.4 | Cesari |

---

## Cobertura del enunciado

Para que no quede ninguna sección sin dueño.

| § | Qué pide | Dónde está explicado | Explica |
|---|---|---|---|
| 1.1 | Viabilidad y acuerdo | [02](02-viabilidad-y-acuerdo.md) | Alieni |
| 1.2 | Preparación del curso | [03](03-preparacion-del-curso.md) | Cesari |
| 1.3 | Cancelación durante la preparación | [04](04-cancelacion-y-compensacion.md) | Alieni |
| 1.4 | Desarrollo y finalización | [05](05-desarrollo-y-finalizacion.md) | Alieni |
| 1.5 | Diagrama BPMN y sus modificaciones | [01](01-modelo-y-motor.md) | Damián |
| 2 | Personal y responsabilidades | [01](01-modelo-y-motor.md) | Damián |
| 3.1 | Regla de negocio de viabilidad (DMN) | [02](02-viabilidad-y-acuerdo.md) | Alieni |
| 3.2 | Base de datos de la institución | [08](08-datos-de-la-empresa-y-acceso.md) | Fran |
| 3.3 | Usuarios y grupos | [01](01-modelo-y-motor.md) y [07](07-backend-de-la-institucion.md) | Damián / Fran |
| 3.4 | Los dos listeners | [06](06-variables-listeners-y-decisiones.md) | Damián |
| 3.5 | Datos y temporizadores | [06](06-variables-listeners-y-decisiones.md), [05](05-desarrollo-y-finalizacion.md) | Damián / Alieni |
| 4.1.1 | Solicitud de capacitación | [09](09-sitio-web.md) | Cesari |
| 4.1.2 | Acceso y seguimiento de la empresa | [08](08-datos-de-la-empresa-y-acceso.md) | Fran |
| 4.1.3 | Funciones para Administración | [09](09-sitio-web.md) | Cesari |
| 4.1.4 | Funciones para el equipo docente | [09](09-sitio-web.md) | Cesari |
| 4.2.1 | App de materiales (API Java) | [10](10-apps-auxiliares.md) | Damián |
| 4.2.2 | App de comunicaciones (API REST) | [10](10-apps-auxiliares.md) | Damián |
| 4.3 | Análisis de las ejecuciones | [11](11-analisis-y-mineria.md) | Cesari |
| 4.4 | Minería de procesos | [11](11-analisis-y-mineria.md) | Cesari |
| 5 | Entrega y escenarios de la defensa | Este índice | Los cuatro |

---

## Las tareas de la institución, repartidas

El enunciado §2 define 16 tareas de usuario más las automáticas. Para la demo en vivo, cada uno muestra las suyas. La columna *Responsable* es la del enunciado; la columna *Explica* es nuestra.

| Tarea del proceso | Tipo | Responsable (§2) | Explica |
|---|---|---|---|
| Registrar y elaborar propuesta | userTask | Jefe administrativo | Cesari |
| Enviar propuesta | scriptTask | — (automática) | Alieni |
| Revisar propuesta | userTask | Jefe administrativo | Cesari |
| Archivar solicitud | userTask | Jefe administrativo | Alieni |
| Habilitar acceso de la empresa | serviceTask | — (automática) | Fran |
| Recibir nómina | mensaje | — (la envía la empresa) | Fran |
| Confeccionar lista definitiva | userTask | Administración (lo asigna un listener) | Damián |
| Preparar materiales | userTask | Administración | Damián |
| Evaluar recursos | userTask | Administración | Cesari |
| Reservar aula | serviceTask | — (web service simulado) | Damián |
| Gestionar falta de aula | userTask | Administración | Damián |
| Solicitar aula virtual | userTask | Administración | Cesari |
| Contratar equipamiento | userTask | Administración | Cesari |
| Liberar aula / aula virtual | compensación automática | — | Alieni |
| Liberar equipamiento | userTask de compensación | Administración | Alieni |
| Confirmar liberación de recursos | userTask | Administración | Alieni |
| Definir docente que desarrolla el curso | userTask | Jefe docente | Cesari |
| Desarrollar curso | userTask | Docente seleccionado | Cesari |
| Elaborar informe de resultados | userTask | Jefe docente | Fran |
| Enviar informe a empresa y archivar expediente | userTask | Jefe administrativo | Fran |

---

## Los once escenarios de la defensa (§5)

Los ocho primeros tienen test automatizado; todos se pueden mostrar en vivo.

| # | Escenario | Dónde mostrarlo | Test | Explica |
|---|---|---|---|---|
| 1 | Aceptación y finalización normal, con liberación de recursos | Sitio web + app de comunicaciones | `HappyPathTest` | Alieni |
| 2 | Solicitud de modificaciones y nueva propuesta | Sitio web (*Revisar propuesta*, PDF v2) | `ProposalModificationsTest` | Cesari |
| 3 | Rechazo de la propuesta | App de comunicaciones + bandeja | `ProposalRejectionTest` | Alieni |
| 4 | Cierre por falta de respuesta | Plazos cortos + bandeja | `NoResponseTest` | Alieni |
| 5 | Límite de cursos en ejecución | Alta de una sexta solicitud | `CourseLimitExceededTest` | Alieni |
| 6 | Cancelación con compensación | App de comunicaciones | `CompensationTest` | Alieni |
| 7 | Falta de aulas y excepción | `aulas.disponibles=1` | `ClassroomUnavailableTest` | Damián |
| 8 | Asignación, reclamo y finalización según responsables | Bandeja del sitio | `TaskAssignmentTest` | Damián |
| 9 | Registro de una solicitud desde el sitio e inicio de la instancia | Formulario público | — | Cesari |
| 10 | Habilitación del acceso de la empresa y consulta exclusiva | Login de empresa + *Mis cursos* | — | Fran |
| 11 | Materiales por API Java y comunicaciones por API REST | Las dos apps de escritorio | — | Damián |

Para dejar el stack con una instancia parada en cada estado:

```bash
.venv/bin/python scripts/preparar_demo.py --limpiar
```

El script imprime, para cada escenario, dónde verlo y con qué usuario.

---

## Material a entregar (§5)

| Qué pide el enunciado | Dónde está |
|---|---|
| Modelo BPMN ejecutable | [`flowable/src/main/resources/processes/`](../../flowable/src/main/resources/processes/) |
| Código del servicio REST y de los componentes del motor | [`flowable/`](../../flowable/), [`backend/`](../../backend/) |
| Reglas de negocio | [`evaluarViabilidad.dmn`](../../flowable/src/main/resources/dmn/evaluarViabilidad.dmn) |
| Instrucciones de configuración y ejecución | [`README.md`](../../README.md) |
| Informe de variables, listeners y decisiones | Esta carpeta, en especial [06](06-variables-listeners-y-decisiones.md) |
| Código de las tres aplicaciones externas | [`backend/`](../../backend/) (sitio), [`materiales/`](../../materiales/), [`comunicaciones/`](../../comunicaciones/) |
| CSV, código y captura de la minería | [`mineria/`](../../mineria/) |

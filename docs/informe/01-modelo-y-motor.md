# 01 — Modelo BPMN, motor, usuarios y responsabilidades

> **Explica:** Damián · **Cubre:** §1.5 (diagrama y modificaciones), §2 (personal y responsabilidades), §3.3 (usuarios y grupos)

---

## 1. Qué pide el enunciado

- Implementar el proceso en Flowable tomando como referencia el diagrama de la Figura 1, con libertad para extenderlo o hacer modificaciones menores **justificadas** (§1.5).
- Administración con 2 personas (una es el jefe) y Docentes con 3 (uno es el jefe), con las tareas asignadas según las responsabilidades de §2.
- Crear en Flowable los **cinco usuarios** y los dos grupos, identificar a los dos jefes y configurar asignaciones y grupos candidatos (§3.3).
- Las tareas abiertas a varios integrantes llevan grupo candidato y hay que **reclamarlas** antes de completarlas; las exclusivas de un jefe van asignadas directamente y **no** requieren reclamo.
- El personal entra a las apps externas con su **ID de usuario y contraseña de Flowable**, sin cuentas separadas en la BD de la institución.

## 2. Cómo se resolvió

### El proceso desplegado

Un solo proceso ejecutable, `Process_Curso`, en [`Institución_de_capacitación.bpmn20.xml`](../../flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml). Lo despliega la app del motor al arrancar (Spring Boot 2.7 + Flowable 6.8, módulo [`flowable/`](../../flowable/)), así que no hay que subirlo a mano.

Arranca por **mensaje** (`Solicitud de curso`), no por un start event vacío: así la instancia solo nace cuando llega una solicitud real, y el sitio web la inicia por nombre de mensaje sin conocer el id del proceso.

Estructura, de arriba hacia abajo:

```text
Solicitud de curso (mensaje)
   └─ Evaluar Viabilidad (decision task DMN)  ──► ¿Es viable?
         ├─ no ─► Notificar decisión ─► FIN (NO_VIABLE)
         └─ sí ─► Registrar y elaborar propuesta ─► Enviar propuesta
                      └─ Esperar respuesta (compuerta basada en eventos)
                            ├─ Aceptación ─► Habilitar acceso ─► [Preparar curso] ─► ...
                            ├─ Rechazo ─► Archivar solicitud ─► FIN (RECHAZADO)
                            ├─ Modificaciones ─► Revisar propuesta ─► Enviar propuesta
                            └─ 5 días ─► Enviar recordatorio ─► ¿dos ya? ─► FIN (SIN_RESPUESTA)
```

El detalle de cada etapa está en los documentos [02](02-viabilidad-y-acuerdo.md) a [05](05-desarrollo-y-finalizacion.md).

### Modificaciones respecto del diagrama de cátedra

Están justificadas una por una en [`model/informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md`](../../model/informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md) y en [`model/cambiosBpmnEjecutableYListeners.md`](../../model/cambiosBpmnEjecutableYListeners.md). Las que conviene saber de memoria:

| Cambio | Por qué |
|---|---|
| La cancelación es un **subproceso de evento interrumpiendo** `Preparar curso`, no un evento de borde | Permite compensar solo lo gestionado y cerrar el subproceso sin matar la instancia. Ver [`adr/cancelacion-no-interruptiva.md`](../../adr/cancelacion-no-interruptiva.md) |
| Compensación con eventos de borde `cancelActivity="false"` y actividades `isForCompensation` | Es la forma BPMN de deshacer solo lo que terminó bien. Ver [`adr/compensacion-bpmn.md`](../../adr/compensacion-bpmn.md) |
| *Reservar aula* es un `serviceTask` con delegate, con evento de borde de error | El enunciado pide simular un web service y generar la excepción por falta de aulas. Ver [`adr/aulas-simuladas.md`](../../adr/aulas-simuladas.md) |
| Compuerta extra *¿Cancelado?* a la salida de la preparación | Un subproceso interrumpido y uno completado salen por el mismo flujo; hace falta distinguirlos para no enviar la confirmación de un curso cancelado |
| Un `ExecutionListener` de fin de proceso y un `TaskListener` de creación | Los pide §3.4. Ver [06](06-variables-listeners-y-decisiones.md) |

El modelo de referencia conceptual queda en [`model/cursosNuestraVersion.bpmn`](../../model/cursosNuestraVersion.bpmn); el que se ejecuta es el de `flowable/src/main/resources/processes/`.

### Usuarios, grupos y responsabilidades

Los crea [`IdentitySeeder`](../../flowable/src/main/java/com/flowable/app/config/IdentitySeeder.java) en cada arranque, en cualquier perfil, directamente en las tablas IDM del motor:

| Usuario | Grupo | Rol en §2 |
|---|---|---|
| `jefeadministrativo` | `administracion` | Jefe administrativo |
| `administrativo1` | `administracion` | Administrativo |
| `jefedocente` | `docentes` | Jefe del equipo docente |
| `docente1` | `docentes` | Docente |
| `docente2` | `docentes` | Docente |

Contraseña `password` para los cinco. Además se crea `admin`/`test`, la cuenta técnica con la que el BFF le habla al motor.

El seeder es **idempotente** y además repara membresías faltantes, así que se puede volver a correr sin riesgo: solo da de alta lo que no está. Se desactiva con `institucion.seed.enabled=false`. Lo verifica `IdentitySeederTest` con el perfil `prod`, que es el que usa Docker.

Las responsabilidades de §2 están en el propio modelo, no en código:

- **Tareas de un jefe:** `flowable:assignee="jefeadministrativo"` o `"jefedocente"`. Vienen asignadas, no se reclaman y solo las completa esa persona.
- **Tareas abiertas al grupo:** `flowable:candidateGroups="administracion"`. Hay que reclamarlas antes de completarlas.
- **Tarea del docente elegido:** `flowable:assignee="${docenteSeleccionado}"`, que se resuelve en ejecución con lo que eligió el jefe docente.
- **Caso especial:** *Confeccionar lista definitiva* tiene grupo candidato **y** un task listener que la asigna sola al administrativo menos ocupado (§3.4).

### Autenticación

El motor expone HTTP Basic contra su propio IDM ([`FlowableAuthenticationProvider`](../../flowable/src/main/java/com/flowable/app/security/FlowableAuthenticationProvider.java)): valida con `identityService.checkPassword` y convierte los grupos en roles `ROLE_ADMINISTRACION` / `ROLE_DOCENTES`. Por eso el mismo usuario de Flowable sirve para la API REST, para el sitio y para la app de materiales, que es lo que pide §3.3.

`/health` y `/groups` son públicos a propósito: sirven para verificar que el stack levantó y que el seed corrió.

## 3. Cómo se demuestra

```bash
curl -s http://localhost:8080/groups
```

Devuelve los dos grupos. Y un usuario de negocio autenticándose:

```bash
curl -s -u administrativo1:password http://localhost:8080/me
```

Devuelve su usuario, sus grupos y sus roles. En Flowable UI (`http://localhost:8088/flowable-ui/`, IDM App) se ven los cinco usuarios y las dos membresías.

## 4. Preguntas probables

**¿Por qué el proceso arranca por mensaje y no con un start event normal?**
Porque la solicitud entra desde afuera (sitio web o API) y el mensaje da un punto de entrada con nombre estable. El sitio inicia la instancia con `startProcessInstanceByMessage("Solicitud de curso")` sin acoplarse a la clave ni a la versión del proceso.

**¿Qué pasa si se despliega una versión nueva del proceso?**
Las instancias viejas siguen con su versión. Las consultas que cuentan instancias lo hacen **por `processDefinitionKey`**, no por id de definición, así que consideran todas las versiones, como pide §3.1.

**¿Dónde está la seguridad: en el modelo o en el código?**
El *quién puede* está en el modelo (assignee y candidateGroups). El *quién es* está en el IDM del motor. El sitio y las apps no reimplementan permisos: le preguntan al motor por la bandeja del usuario y, además, el BFF verifica que la tarea esté en esa bandeja antes de dejar completarla.

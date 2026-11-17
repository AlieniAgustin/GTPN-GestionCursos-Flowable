# Simulación y Control de Comunicaciones de la Empresa (§4.2.2)

Aplicación auxiliar para simular las respuestas de empresas externas hacia el motor de procesos Flowable mediante su API REST. Permite a evaluadores y desarrolladores controlar y verificar todos los caminos del flujo BPMN (aceptación, rechazo, modificaciones, nómina y cancelación) sin requerir interacción por correo real ni habilitar acciones de mutación en el portal web de clientes.

---

## Quick Path (Paso a Paso Rápido)

1. **Iniciar la aplicación:**
   ```bash
   cd comunicaciones && ./build.sh
   java -jar target/app.jar
   ```
2. **Seleccionar la solicitud:**  
   Hacer clic en la fila del curso en la tabla superior. La app consulta en tiempo real a Flowable (`/runtime/executions`) y habilita únicamente los botones correspondientes al estado actual.
3. **Enviar la comunicación:**  
   Presionar el botón habilitado e ingresar los datos requeridos (por ejemplo, fecha acordada `2027-03-01` en *Aceptar la propuesta*).
4. **Verificar el avance:**  
   Comprobar en el portal web (`http://localhost:8081/mis-cursos`) o en Flowable UI que el token avanzó a la siguiente actividad.

---

## Details (Decisiones y Reglas Técnicas)

### Separación de Responsabilidades

| Entorno | Enunciado | Rol y Alcance | Autenticación |
|---|---|---|---|
| **Portal Web (`/mis-cursos`)** | §4.1.2 | **Solo lectura:** La empresa visualiza sus propios cursos, estados y avance de tareas paralelas. No envía mensajes. | Usuario de empresa (`ROLE_EMPRESA`) contra tabla `companies`. |
| **App Swing (`comunicaciones/`)** | §4.2.2 | **Simulador global:** El operador selecciona cualquier solicitud activa del sistema y envía eventos externos hacia Flowable. | Cuenta técnica (`admin:test`) contra `/process-api`. |

### Tareas de Usuario vs. Eventos de Espera en Flowable

| Tipo de Elemento | Tipo BPMN | Ubicación en Motor | Interacción Técnica |
|---|---|---|---|
| **Tarea de Usuario** | `userTask` | `ACT_RU_TASK` | El personal institucional la reclama y completa mediante formulario (`action: complete`). |
| **Punto de Espera** | `intermediateCatchEvent` / Event Subprocess | `ACT_RU_EVENT_SUBSCR` | El token aguarda una señal externa. Se despierta informando la recepción del mensaje (`action: messageEventReceived`). |

> **Principio de Diseño:** La aplicación `comunicaciones` **no tiene base de datos propia ni deduce estados**. En cada selección, consulta activamente a `GET /runtime/executions?messageEventSubscriptionName=...`. Si Flowable no tiene una suscripción abierta para ese mensaje, el botón no se dibuja.

### Catálogo de las 5 Comunicaciones

| Mensaje BPMN | Etapa del Proceso | Variable Inyectada | Validación / Formato | Efecto Posterior |
|---|---|---|---|---|
| `Aceptación` | Negociación | `fechaCurso` | ISO-8601 (`AAAA-MM-DD` o `AAAA-MM-DDTHH:MM`) | `SyncCourseDetailsListener` persiste la fecha en `courses.agreed_date` e inicia la preparación. |
| `Rechazo` | Negociación | *(Ninguna)* | — | Avanza a *Archivar solicitud* y cierra el curso como `RECHAZADO`. |
| `Solicitud de modificaciones` | Negociación | `observaciones` | Texto obligatorio | Vuelve a la tarea *Revisar propuesta* para el administrativo. |
| `Nómina de participantes` | Preparación | `participantes` | Lista normalizada por comas | Almacena la lista definitiva como variable del proceso. |
| `Cancelación` | Preparación | *(Ninguna)* | — | Interrumpe la preparación en paralelo, compensa recursos y finaliza como `CANCELADO`. |

### Flujo de Datos y Persistencia

```text
[App Comunicaciones] ──PUT /runtime/executions/{id}──► [Flowable Motor]
                                                             │
                                   ┌─────────────────────────┴─────────────────────────┐
                                   ▼                                                   ▼
                    Consumo de evento en runtime                        Listener en flujo saliente
                    ACT_RU_EVENT_SUBSCR (eliminado)                                    │
                    ACT_HI_ACTINST (completado)                                        ▼
                    ACT_HI_VARINST (variables guardadas)                PUT /api/flowable/courses/...
                                                                                       │
                                                                                       ▼
                                                                           Backend (PostgreSQL)
                                                                           courses (agreed_date / status)
```

---

## Checklist de Verificación (Para Testing y Defensa)

- [ ] **Aislamiento web:** Al ingresar con cuenta de empresa en `/mis-cursos`, no existen botones para aceptar, rechazar o cancelar; la vista es estrictamente informativa.
- [ ] **Detección dinámica en simulador:** Si una solicitud está en *Registrar propuesta*, la app muestra: *"en este estado la instancia no espera ninguna comunicación"*.
- [ ] **Habilitación en negociación:** Una vez enviada la propuesta, aparecen únicamente los botones *Aceptar*, *Rechazar* y *Pedir modificaciones*.
- [ ] **Validación de entrada:** Si se envía una fecha inválida en *Aceptar*, la app lo rechaza localmente antes de llamar a Flowable para evitar tokens trabados.
- [ ] **Habilitación en preparación:** Tras la aceptación, los botones de negociación desaparecen y se activan únicamente *Nómina de participantes* y *Cancelar el curso*.
- [ ] **Sincronización con BD:** Al aceptar, el campo `Fecha acordada` se actualiza inmediatamente en el detalle del curso en la base de datos de la institución.

---

## Next Steps

* **Probar sin interfaz gráfica:** Si se ejecuta en un servidor headless o terminal remota, utilizar los comandos `curl` detallados en la sección de pruebas de `README.md`.
* **Continuar con la etapa siguiente:** Proceder con la asignación de recursos y preparación de materiales mediante la app auxiliar de materiales (`materiales/`).

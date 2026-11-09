# Resolución sobre Cancelación durante Preparación

Documento de diseño y flujo operativo para la cancelación de cursos durante la etapa de preparación y la gestión de compensaciones.

---

## 1. Contexto Operativo

Durante la fase de preparación de un curso (`prepararcurso`), conviven tareas administrativas y asignaciones de recursos:
* **Aula física:** Reserva automática vía `serviceTask`.
* **Aula virtual:** Reserva vía `serviceTask` / `scriptTask`.
* **Equipamiento especial:** Contratación manual y posterior desestimación o liberación obligatoria mediante intervención humana (`userTask` por el grupo `administracion`), conforme a los requerimientos de negocio.

Si la empresa cliente cancela el curso mientras se encuentra en preparación, el sistema debe interrumpir inmediatamente las tareas activas y compensar los recursos previamente comprometidos.

---

## 2. Limitación Técnica del Motor (Flowable 6.8)

El estándar BPMN 2.0 define el atributo `waitForCompletion` en los eventos intermedios de compensación para pausar el avance del token hasta que todos los manejadores de compensación finalicen.

Sin embargo, en **Flowable 6.8.0**:
1. El parser XML reconoce el atributo `waitForCompletion="true"`, pero la clase interna `IntermediateThrowCompensationEventActivityBehavior` **lo ignora por completo**.
2. Al disparar el evento de compensación, el motor despacha las suscripciones y acto seguido invoca `leave(execution)` sin validar si los handlers terminaron.
3. Para tareas automáticas (`serviceTask` o `scriptTask`), esto no supone un problema porque se ejecutan en la misma transacción sincrónica.
4. Para tareas humanas (`userTask` con `isForCompensation="true"` como `liberarequipamiento`), la tarea se crea en la base de datos pero requiere tiempo de resolución externa.

### El problema resultante sin sincronización
Si el flujo de cancelación avanza inmediatamente hacia un fin de subproceso o hacia una propagación de error:
* El boundary o el cierre del subproceso destruye el *scope* de ejecución de `prepararcurso`.
* Al destruirse el *scope*, **la `userTask` pendiente se elimina en la misma transacción**.
* El administrativo nunca llega a ver la tarea en su bandeja y el equipamiento queda reservado sin poder liberarse

---

## 3. Decisión y Patrón de Sincronización

Para garantizar que ninguna tarea de compensación humana sea destruida prematuramente, se adoptó una arquitectura de **punto de pausa y barrera de sincronización**:

```
[Start: Cancelación (Interruptivo)]
               │
               ▼
   [Throw Compensación Equipamiento]  ──► (Crea userTask 'liberarequipamiento')
               │
               ▼
     [Throw Compensación Aula]         ──► (serviceTask automática)
               │
               ▼
 [Throw Compensación Aula Virtual]    ──► (scriptTask automática)
               │
               ▼
 [Script: Registrar Cancelación]      ──► (variable cancelado = true)
               │
               ▼
[userTask: Confirmar Liberación]      ──► (PUNTO DE PAUSA: mantiene vivo el scope)
               │
               ▼
     [Fin Event Subprocess]
               │
               ▼
   [Gateway Exterior: ¿Cancelado?]    ──► Notificar Cierre y Fin del Proceso
```

### Reglas del diseño adoptado

1. **Event Subprocess Interruptivo:**
   El inicio de la cancelación (`recibircancelacion`) tiene `isInterrupting="true"`. Al recibir el mensaje `Cancelación`, detiene inmediatamente cualquier otra tarea concurrente de preparación (como carga de nómina o materiales).

2. **Punto de Pausa (`confirmarliberacionrecursos`):**
   Se incorpora una `userTask` explícita asignada al grupo `administracion`. Esta tarea retiene el token dentro del subproceso de evento y mantiene vivo el *scope*.

3. **Garantía de Orden por el Motor:**
   Flowable no permite cerrar un *scope* si existen ejecuciones hijas activas. Si un operador intenta completar `confirmarliberacionrecursos` mientras `liberarequipamiento` sigue pendiente, el motor rechaza la operación y efectúa un rollback de la transacción. La confirmación solo puede completarse una vez que el equipamiento fue efectivamente liberado (`equipoLiberado = true`).

4. **Transición sin Error Boundaries:**
   Al evitar un evento de fin de error (`errorEndEvent`), la cancelación termina en un fin normal dentro del subproceso. La decisión de desvío se traslada al gateway exclusivo exterior `¿Cancelado?`, que al evaluar `${cancelado == true}` conduce al cierre ordenado del curso (`resultadoCurso = CANCELADO`).

---

## 4. Comparativa de Alternativas

| Enfoque | Viabilidad | Motivo de Descarte o Adopción |
|---|---|---|
| `waitForCompletion="true"` nativo | Inviable | Ignorado por `IntermediateThrowCompensationEventActivityBehavior` en Flowable 6.8. |
| `liberarequipamiento` como `scriptTask` | Descartado | Automatiza la liberación pero incumple la consigna de validación humana administrativa. |
| Bucle con Gateway y Timer de espera | Descartado | Introduce polling innecesario y complejidad en el diagrama BPMN. |
| **User Task de confirmación (Punto de pausa)** | **Adoptado** | Respeta la intervención humana requerida, asegura consistencia transaccional y aprovecha las validaciones nativas de ciclo de vida de Flowable. |

---

## 5. Casos de Verificación y Testing

El comportamiento está cubierto por la suite de integración en `CompensationTest.java`:

* **Cancelación con equipamiento contratado:** Verifica la creación sucesiva de `liberarequipamiento` y `confirmarliberacionrecursos`, comprobando el estado final `CANCELADO` y `equipoLiberado = true`.
* **Rechazo por inversión de orden:** Verifica que completar la confirmación antes de la liberación dispare una excepción y mantenga vivas ambas tareas.
* **Cancelación sin equipamiento previo:** Verifica que si solo hubo reservas automáticas (aulas), la confirmación se complete de forma directa sin requerir intervención sobre equipamiento inexistente.

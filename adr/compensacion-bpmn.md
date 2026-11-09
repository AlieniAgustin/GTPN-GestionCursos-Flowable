# ADR: Espera de handlers de compensación en Flowable 6.8

- **Estado:** Aceptado
- **Fecha:** 2026-11-08 (actualizado 2026-11-09)
- **Contexto:** `flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml`
- **Versión del motor:** Flowable 6.8.0 / Spring Boot 2.7.18
- **Relacionado:** [ADR: Event subprocess de cancelación interruptivo con gateway ¿Cancelado?](cancelacion-no-interruptiva.md)

## Contexto

La cancelación del curso usa compensación BPMN. Según la consigna, el handler `liberarequipamiento` debía ser una tarea de usuario (`isForCompensation="true"`): un administrativo confirma la liberación del equipamiento desde la UI de Flowable.

Con una tarea de usuario el proceso no puede esperar a que el handler termine, por cómo funciona el motor (ver abajo). Este ADR documenta ese comportamiento y la decisión tomada.

## Comportamiento del motor: el throw event no espera

`IntermediateThrowCompensationEventActivityBehavior.execute` (Flowable 6.8.0) termina así:

```
409: aload 7
411: invokeinterface List.isEmpty()
416: ifne 426
419: aload 7
421: aload_1
422: iconst_0
423: invokestatic ScopeUtil.throwCompensationEvent(List, DelegateExecution, boolean)
426: aload_0
427: aload_1
428: invokevirtual leave(DelegateExecution)     // el token principal SIEMPRE avanza
431: return
```

El throw event lanza la compensación (`async=false`) y acto seguido hace `leave()`, sin comprobar si el handler terminó. No hay ninguna llamada a `isWaitForCompletion()` en el comportamiento.

El atributo existe en el modelo (`CompensateEventDefinition.waitForCompletion`) y el parser XML lo lee (`CompensateEventDefinitionParser` contiene `waitForCompletion` / `setWaitForCompletion`), pero en 6.8 el motor lo ignora. Poner `waitForCompletion="true"` no cambia el comportamiento.

## Qué pasaba con `liberarequipamiento` como userTask

1. `compensarrecursoscancelacion*` busca las suscripciones de compensación y las ejecuta (`async=false`).
2. El handler `liberarequipamiento` es una `userTask`: se crea la tarea y su execution, pero no puede completarse dentro de la transacción.
3. El throw event hace `leave()` y el token principal sigue hasta `errorcancelacion`.
4. El error se propaga al boundary `capturarcancelacion`, que destruye el scope de `prepararcurso` y con él la tarea pendiente.

La tarea se crea y se borra en la misma transacción: nadie llega a verla y el curso se cancela con el equipamiento reservado.

Para evitarlo hay que mantener vivo el scope de cancelación hasta que el humano complete la tarea.

## Decisión

- **Fecha:** 2026-11-08, consultado con el profesor.

1. **`liberarequipamiento` pasa a ser un `scriptTask`**, igual que las otras compens## Decisión

- **Fecha:** 2026-11-09, siguiendo el ejemplo de compensación pasado por el profesor.

1. **`liberarequipamiento` sigue siendo una `userTask`** (`isForCompensation="true"`, grupo `administracion`), como pide la consigna. Al completarla pone `equipoLiberado = true`.
2. **El event subprocess `cancelacion` es interruptivo** y hace: compensaciones → `Registrar Cancelacion` (script, `cancelado = true`) → `confirmarliberacionrecursos` (userTask, grupo `administracion`) → fin.
3. **`confirmarliberacionrecursos` es el punto de pausa**: mantiene vivo el scope mientras la compensación humana está pendiente. Si se intenta completar antes que `liberarequipamiento`, el motor rechaza la operación y deshace la transacción; Confirmar sigue pendiente hasta que se libere el equipamiento.
4. **Después de `prepararcurso`, el gateway `¿Cancelado?`** decide: `${cancelado == true}` → `notificarcierre` → `fincierre` (`resultadoCurso = CANCELADO`); por defecto → `enviarconfirmacion`.
5. **No confiar en `waitForCompletion`** en esta versión del motor.

## Consecuencias

- La liberación del equipamiento es humana, como pide la consigna (secciones 2 y 4.1.3), sin bucles de polling.
- El orden lo controla el motor: primero Liberar, después Confirmar.
- Si no se contrató equipamiento, no se crea `liberarequipamiento` y Confirmar se completa directo.
- `CompensationTest` cubre los tres casos: cancelación con equipamiento, Confirmar antes que Liberar (rechazado) y cancelación sin equipamiento.

## Alternativas consideradas

| Alternativa | Por qué no se eligió |
|---|---|
| `waitForCompletion="true"` en el throw event | El atributo se parsea pero el comportamiento de 6.8 no lo lee; no tiene efecto. |
| `liberarequipamiento` como `scriptTask` | Fue la versión del 2026-11-08 (aceptada por el profesor "por ahora"). Funciona, pero la liberación deja de ser humana y no cumple la consigna. |
| `userTask` + gateway `equipoliberado` + timer `esperarliberacion` | Funciona (commit `aa0fd61`), pero agrega un bucle de espera por polling que el ejemplo del profesor no usa. |
| Dejar la `userTask` sin punto de pausa | Inaceptable: destruye la tarea de compensación y cancela el curso dejando recursos reservados. |

flowable-engine/6.8.0/flowable-engine-6.8.0.jar \
  org.flowable.engine.impl.bpmn.behavior.IntermediateThrowCompensationEventActivityBehavior
```

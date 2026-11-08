# ADR: Espera de handlers de compensación en Flowable 6.8

- **Estado:** Aceptado
- **Fecha:** 2026-11-08
- **Contexto:** `flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml`
- **Versión del motor:** Flowable 6.8.0 / Spring Boot 2.7.18
- **Relacionado:** [ADR: Event subprocess de cancelación no interruptivo](cancelacion-no-interruptiva.md)

## Contexto

La cancelación del curso usa compensación BPMN. El handler `liberarequipamiento` es una `userTask` (`isForCompensation="true"`): un humano debe confirmar la liberación del equipamiento antes de que el curso quede cancelado.

El subproceso de cancelación no termina inmediatamente después de lanzar la compensación: pasa por el gateway `equipoliberado` y, si el equipamiento aún no se liberó, espera en el timer `esperarliberacion` (PT10S) y vuelve a consultar. Este ADR documenta por qué ese estado de espera es necesario y qué hace exactamente el motor con el handler.

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

## Secuencia real al recibir la cancelación

1. `compensarrecursoscancelacion*` busca las suscripciones de compensación y las ejecuta (`async=false`).
2. El handler `liberarequipamiento` es una `userTask`: se crea la tarea y su execution, pero no puede completarse dentro de la transacción.
3. El throw event hace `leave()` y el token principal sigue al gateway `equipoliberado`.
4. El gateway consulta `equipoLiberado`; como el humano aún no completó la tarea, va al timer `esperarliberacion` y vuelve a consultar.

Es decir: la tarea de compensación y el flujo principal avanzan en paralelo. El flujo principal no "espera" al handler; solo el gateway + timer lo simulan.

## ¿La tarea queda huérfana?

No en el sentido de "sin execution". La tarea de compensación tiene su propia execution, es consultable con `taskService.createTaskQuery().taskDefinitionKey("liberarequipamiento")` y se puede completar con normalidad. Está viva y en espera, en paralelo al token principal.

Lo que sí ocurre es que **su vida está atada al scope que la creó**. Si ese scope se destruye antes de completarla, Flowable borra la execution y con ella la tarea. En este proceso, terminar el subproceso de cancelación con `errorcancelacion` propaga el error al boundary event `capturarcancelacion` (adjunto a `prepararcurso`), destruye el scope y elimina la tarea pendiente. Sin el gateway/timer, la tarea `liberarequipamiento` se crea y se destruye en la misma transacción: el humano nunca llega a verla y el curso se cancela dejando el equipamiento reservado.

El bucle de espera no es un capricho: mantiene vivo el scope hasta que el handler humano termina.

## Decisión

1. **Mantener el gateway `equipoliberado` + timer `esperarliberacion`** mientras `liberarequipamiento` sea una `userTask`. Es un polling consciente para suplir que Flowable 6.8 no espera handlers de compensación con estado de espera.
2. **No confiar en `waitForCompletion`** en esta versión del motor.
3. Si en el futuro la liberación pasa a ser automática, reemplazar el handler por un `serviceTask` y eliminar el gateway/timer.

## Consecuencias

- La cancelación espera en granularidad de 10 s; en producción requiere el async executor activo (`flowable.async-executor-activate=true`, ya configurado en `application.properties`).
- En tests el executor está apagado y el job se dispara a mano (`CompensationTest`), lo cual es correcto para un test determinista.
- Mientras el handler sea humano, el proceso no puede cerrarse en la misma transacción en que se recibe la cancelación.

## Alternativas consideradas

| Alternativa | Por qué no se eligió |
|---|---|
| `waitForCompletion="true"` en el throw event | El atributo se parsea pero el comportamiento de 6.8 no lo lee; no tiene efecto. |
| Handler `liberarequipamiento` como `serviceTask` | Elimina la espera y el timer, pero cambia el requisito: hoy un humano confirma la liberación. |
| `userTask` normal dentro del event subprocess de cancelación (sin compensación) | Más simple (sin gateway ni timer), pero deja de usar semántica de compensación y la limpieza corre siempre; habría que condicionarla con `equipoContratado`. Es la opción recomendada si se acepta ese cambio de modelo. |
| `receiveTask` / mensaje externo | Solo aplica si otro sistema confirma la liberación; hoy no existe esa integración. |
| Terminar el subproceso sin esperar al handler | Inaceptable: destruye la tarea de compensación y cancela el curso dejando recursos reservados. |

## Reproducción

```bash
cd flowable
mvn -o -Dtest=CompensationTest test

# Evidencia en bytecode
javap -p -c -classpath ~/.m2/repository/org/flowable/flowable-engine/6.8.0/flowable-engine-6.8.0.jar \
  org.flowable.engine.impl.bpmn.behavior.IntermediateThrowCompensationEventActivityBehavior
```

Resultado actual: `Tests run: 8, Failures: 0, Errors: 0`.

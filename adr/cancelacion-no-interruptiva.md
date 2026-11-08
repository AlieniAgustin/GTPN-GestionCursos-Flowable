# ADR: Event subprocess de cancelación no interruptivo

- **Estado:** Aceptado
- **Fecha:** 2026-11-08
- **Contexto:** `flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml`
- **Versión del motor:** Flowable 6.8.0 / Spring Boot 2.7.18
- **Relacionado:** [ADR: Espera de handlers de compensación en Flowable 6.8](compensacion-bpmn.md)

## Contexto

La cancelación del curso se modela con un event subprocess `cancelacion` (`triggeredByEvent="true"`) dentro del subproceso `prepararcurso`. Su start event `recibircancelacion` escucha el mensaje `Cancelación`.

El subproceso de cancelación lanza las compensaciones, espera la liberación del equipamiento y termina con el error end `errorcancelacion` (error `CANCELACION`). Ese error debe ser capturado por el boundary error `capturarcancelacion`, adjunto a `prepararcurso`, que continúa el camino de cierre:

```
capturarcancelacion → notificarcierre → fincierre
```

## Problema: el start event interruptivo mataba al padre

Con `isInterrupting="true"`, al entrar al event subprocess se cancelaban las ejecuciones del padre (`prepararcurso`). Cuando el hijo terminaba con `errorcancelacion`, el boundary error `capturarcancelacion` ya no tenía una actividad padre viva a la que reaccionar: el error no se podía propagar y el camino de cierre (`capturarcancelacion → notificarcierre → fincierre`) nunca se ejecutaba.

En la práctica se perdía la notificación de cierre del curso cancelado.

## Decisión

Usar `isInterrupting="false"` en `recibircancelacion`:

```xml
<startEvent id="recibircancelacion" name="Recibir Cancelación" isInterrupting="false">
  <messageEventDefinition messageRef="Msg_cancel"></messageEventDefinition>
</startEvent>
```

Así el padre sigue vivo mientras corre la cancelación. Al final, el error end propaga el error al boundary `capturarcancelacion` (los boundary error son interruptivos por semántica BPMN), que cancela `prepararcurso` y sigue con `notificarcierre → fincierre`.

## Consecuencias

- Mientras corre la cancelación, el flujo normal de `prepararcurso` sigue activo en paralelo. Es aceptable porque el boundary error termina cancelando ese scope; la alternativa interruptiva rompía la propagación del error.
- El cierre del curso queda centralizado en el boundary error, no dentro del event subprocess.
- El event subprocess puede convivir con las tareas normales del padre sin destruirlas antes de tiempo.

## Alternativas consideradas

| Alternativa | Por qué no se eligió |
|---|---|
| Start event interruptivo (`isInterrupting="true"`) | Al entrar al hijo mata las ejecuciones del padre y el boundary error `capturarcancelacion` ya no puede capturar el `errorcancelacion`; se pierde la notificación de cierre. |
| Terminar el event subprocess con un end event normal en vez de un error end | El flujo de cierre quedaría dentro del hijo y no se reutilizaría el boundary error; además no se cancelaría `prepararcurso`. |
| Poner el event subprocess a nivel de proceso (fuera de `prepararcurso`) | Cambia el alcance de la cancelación y obliga a compensar actividades de otro scope; el boundary error está pensado para `prepararcurso`. |
| Modelar la cancelación como boundary event interruptivo directo en `prepararcurso` | Es una alternativa válida, pero pierde la compensación y la espera del handler que hoy viven en el event subprocess. |

## Reproducción

`CompensationTest` dispara el mensaje `Cancelación` y verifica la compensación y el cierre:

```bash
cd flowable
mvn -o -Dtest=CompensationTest test
```

Resultado actual: `Tests run: 8, Failures: 0, Errors: 0`.

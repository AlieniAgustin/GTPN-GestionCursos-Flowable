# ADR: Event subprocess de cancelación interruptivo con gateway ¿Cancelado?

- **Estado:** Aceptado (reemplaza la versión no interruptiva del 2026-11-08)
- **Fecha:** 2026-11-09
- **Contexto:** `flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml`
- **Versión del motor:** Flowable 6.8.0 / Spring Boot 2.7.18
- **Relacionado:** [ADR: Espera de handlers de compensación en Flowable 6.8](compensacion-bpmn.md)

## Contexto

La cancelación del curso se modela con un event subprocess `cancelacion` (`triggeredByEvent="true"`) dentro del subproceso `prepararcurso`. Su start event `recibircancelacion` escucha el mensaje `Cancelación`.

La versión anterior usaba un start event no interruptivo y terminaba con el error end `errorcancelacion`, capturado por el boundary error `capturarcancelacion` en `prepararcurso`. Funcionaba, pero mientras corría la cancelación el flujo normal de preparación seguía activo en paralelo.

## Problema

- Con `isInterrupting="true"` y error end, el error no tiene quién lo capture: el start interruptivo elimina el boundary error del padre, así que el camino de cierre nunca se ejecuta.
- Con `isInterrupting="false"`, la preparación sigue viva durante la cancelación: un administrativo podía seguir completando tareas de un curso cancelado.

## Decisión

1. **`recibircancelacion` es interruptivo** (`isInterrupting="true"`): al llegar el mensaje se cancela el flujo normal de preparación.
2. **Se eliminan el error end `errorcancelacion` y el boundary `capturarcancelacion`.** El event subprocess termina con un end event normal:

```
recibircancelacion → compensaciones (equipamiento, aula, aula virtual)
  → Registrar Cancelacion (script: cancelado = true)
  → confirmarliberacionrecursos (userTask, administracion) → fin
```

3. **`prepararcurso` inicializa `cancelado = false`** con un execution listener de start.
4. **Después de `prepararcurso`, el gateway exclusivo `¿Cancelado?`** decide el camino:
   - `si`: `${cancelado == true}` → `notificarcierre` → `fincierre` (`resultadoCurso = CANCELADO`)
   - `no` (default) → `enviarconfirmacion`

## Consecuencias

- Al cancelar, se interrumpe la preparación: no quedan tareas normales activas en un curso cancelado.
- El cierre queda fuera del subproceso, en el gateway `¿Cancelado?`, sin depender de propagar errores.
- `confirmarliberacionrecursos` mantiene vivo el scope hasta que terminan las compensaciones humanas (ver [compensacion-bpmn.md](compensacion-bpmn.md)).

## Alternativas consideradas

| Alternativa | Por qué no se eligió |
|---|---|
| Start no interruptivo + error end + boundary error (versión 2026-11-08) | Funciona, pero la preparación sigue activa en paralelo con la cancelación. |
| Start interruptivo + error end | El boundary error del padre ya no existe cuando llega el error; se pierde el cierre. |
| Start interruptivo + `notificarcierre` y terminate end dentro del event subprocess | Funciona, pero mete el cierre dentro del subproceso y necesita terminate end para que el proceso no siga hacia `enviarconfirmacion`. |
| Poner el event subprocess a nivel de proceso (fuera de `prepararcurso`) | Cambia el alcance de la cancelación y obliga a compensar actividades de otro scope. |

## Reproducción

`CompensationTest` dispara el mensaje `Cancelación` y verifica la compensación y el cierre:

```bash
cd flowable
mvn -o -Dtest=CompensationTest test
```

Resultado actual del proyecto: `Tests run: 10, Failures: 0, Errors: 0`.

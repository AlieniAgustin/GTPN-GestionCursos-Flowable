# ADR: Web service de aulas simulado y falta de aulas

- **Estado:** Aceptado
- **Fecha:** 2026-11-09
- **Contexto:** `flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml`
- **Versión del motor:** Flowable 6.8.0 / Spring Boot 2.7.18
- **Relacionado:** [ADR: Espera de handlers de compensación en Flowable 6.8](compensacion-bpmn.md)

## Contexto

La consigna (sección 4) pide que la reserva y liberación del aula física sean automáticas y que se simule una conexión con un web service. El escenario 7 de la defensa pide mostrar la falta de aulas disponibles y la generación de la excepción correspondiente.

## Decisión

1. **`ClassroomService` simula el web service** con un mapa en memoria `aula → instancia de proceso`. La cantidad de aulas sale de la propiedad `aulas.disponibles` (por defecto 3, en `application.properties`).
2. **`Reservar aula`** usa `${reserveClassroom}` (`ReserveClassroomDelegate`): reserva la primera aula libre y la guarda en `aulaAsignada`. Si no hay ninguna, lanza `BpmnError("errSinAulas")`.
3. **`Liberar aula`** (compensación de Reservar aula) usa `${releaseClassroom}` (`ReleaseClassroomDelegate`): libera `aulaAsignada` y pone `aulaLiberada = true`.
4. **El error se captura con el boundary `sinaulas`** (`errorRef="errSinAulas"`) en Reservar aula, que lleva a la userTask `gestionarfaltadeaulas` (grupo `administracion`). Al completarla se vuelve a Reservar aula (reintento). Si sigue sin haber aulas, el error se repite.
5. **No se corta el proceso.** Reservar aula está en una rama paralela de `prepararcurso`: si esa rama terminara, el join nunca se completaría.

## Bug de Flowable 6.8: orden de los boundaries

Reservar aula tiene dos boundaries: `compensacionaula` (compensación) y `sinaulas` (error). En 6.8, `ContinueProcessOperation` crea ejecuciones solo para los boundaries que no son de compensación, pero después las recorre en paralelo con la lista completa de boundaries. Si la compensación aparece primero en el XML, a la ejecución del error se le asigna el comportamiento de compensación y el motor falla con:

```
FlowableException: Parent activity for boundary compensation event could not be found
```

**`sinaulas` tiene que estar antes que `compensacionaula` en el XML.** La UI de Flowable los exporta en el orden inverso, así que después de reexportar hay que revisar ese orden.

## Consecuencias

- La falta de aulas queda visible como una tarea para administración y el curso no se pierde.
- Como el estado está en memoria, al reiniciar el motor todas las aulas quedan libres aunque haya instancias que las tengan asignadas. Es aceptable para una simulación.
- Para mostrar el escenario 7 en vivo: `aulas.disponibles=1`, un curso reserva el aula y el siguiente cae en `Gestionar falta de aula`.
- `ClassroomUnavailableTest` cubre el escenario: sin aula, reintento fallido, liberación por cancelación de otro curso y reintento exitoso.

## Alternativas consideradas

| Alternativa | Por qué no se eligió |
|---|---|
| Excepción Java común (sin `BpmnError`) | Flowable deshace la transacción y el proceso queda trabado en Evaluar recursos, sin camino definido. |
| Terminar el proceso al no haber aulas | El error tendría que capturarse en `prepararcurso`; se pierde el curso por una falta temporal. |
| Servicio REST real en el backend | La consigna pide simular el web service; agrega complejidad sin aportar al proceso. |

# 03 — Preparación del curso

> **Explica:** Cesari · **Cubre:** §1.2 (preparación del curso)
>
> Decisiones relacionadas: [`adr/aulas-simuladas.md`](../../adr/aulas-simuladas.md)

---

## 1. Qué pide el enunciado

Una vez aceptada la propuesta se ejecutan **en paralelo y sin dependencias** tres ramas:

- **Inscripciones:** recibir la nómina que envía la empresa y confeccionar la lista definitiva. No hace falta verificar los datos de cada participante.
- **Materiales:** preparar los materiales didácticos.
- **Recursos:** evaluar las necesidades y realizar **una o varias** de estas acciones: reservar aula física, solicitar aula virtual, contratar equipamiento especial. Deben completarse **todas las seleccionadas**, y hay que seleccionar **al menos una**.

La preparación concluye cuando terminan las tres ramas.

## 2. Cómo se resolvió

Todo vive dentro del subproceso `prepararcurso`, que es también el alcance de la cancelación (ver [04](04-cancelacion-y-compensacion.md)) y de la compensación final.

```text
                    ┌─ Recibir nómina (mensaje) ─► Confeccionar lista definitiva ─┐
                    │                                                             │
[Preparación en ────┼─ Preparar materiales ───────────────────────────────────────┼─► [Preparación
 paralelo]          │                                                             │    completa] ─► fin
 (parallelGateway)  └─ Evaluar Recursos ─► ◇ Recursos necesarios ─┬─ Reservar aula ────┐
                                            (inclusiveGateway)    ├─ Solicitar aula virtual
                                                                  └─ Contratar equipamiento
                                                                         │
                                                                  ◇ Todos los recursos listos
```

### Las tres ramas

**Inscripciones.** Empieza con un evento de mensaje intermedio `Recibir nómina`, que espera la `Nómina de participantes` de la empresa. La envía la app de comunicaciones (§4.2.2). Después, *Confeccionar lista definitiva*: userTask del grupo `administracion` que además tiene el **task listener de asignación** que pide §3.4 — se asigna sola al administrativo con menos tareas pendientes (ver [06](06-variables-listeners-y-decisiones.md)).

**Materiales.** Una sola userTask, *Preparar materiales*, del grupo `administracion`. Es la que se completa desde la **app de escritorio por API Java** del §4.2.1 (ver [10](10-apps-auxiliares.md)), y también desde la bandeja del sitio.

**Recursos.** *Evaluar Recursos* es una userTask de grupo que deja tres variables booleanas: `requiereAula`, `requierePlataforma` y `requiereEquipamiento`. Con ellas, una **compuerta inclusiva** abre solo las ramas elegidas:

| Variable | Actividad | Tipo |
|---|---|---|
| `requiereAula` | Reservar aula | `serviceTask` con `ReserveClassroomDelegate` (web service simulado) |
| `requierePlataforma` | Solicitar Aula Virtual | userTask del grupo `administracion` |
| `requiereEquipamiento` | Contratar equipamiento | userTask del grupo `administracion` |

La compuerta inclusiva de cierre espera **solo las ramas que se abrieron**, que es exactamente el "deben completarse todas las acciones seleccionadas" del enunciado: si no se pidió aula virtual, su rama nunca se activa y el join no la espera.

El "al menos una alternativa" se valida en la pantalla de *Evaluar recursos* del sitio: sin ninguna opción marcada vuelve al formulario con el error y no completa la tarea. La pantalla manda **siempre las tres variables**, aunque sean `false`, porque si falta alguna la compuerta inclusiva falla con `Unknown property`.

### La reserva de aula y la falta de aulas

`ReserveClassroomDelegate` simula la conexión con un web service de aulas ([`ClassroomService`](../../flowable/src/main/java/com/flowable/app/services/ClassroomService.java)): hay `aulas.disponibles` aulas (3 por defecto) y la reserva deja el aula en la variable `aulaAsignada`. Si no quedan, lanza un `BpmnError`, que captura un **evento de borde de error** y deriva a *Gestionar falta de aula*, una userTask del grupo; al completarla se vuelve a intentar la reserva. Es el escenario 7 de la defensa.

### El cierre de la preparación

El join `preparacioncompleta` es una compuerta paralela que espera las tres ramas. Su fin (`finprepararcurso`) tiene un listener, `CloseCancellationListener`, que cierra la escucha de la cancelación: sin eso el subproceso no termina nunca, porque la ejecución que espera el mensaje de cancelación sigue viva. Está explicado en [04](04-cancelacion-y-compensacion.md).

## 3. Cómo se demuestra

Con una instancia en preparación (`preparar_demo.py` deja una):

1. En `/tareas` del sitio aparecen a la vez **Evaluar Recursos** y **Preparar materiales**, y la instancia espera la nómina: son las tres ramas corriendo en paralelo.
2. En `/mis-cursos/{id}`, la empresa ve esas mismas actividades como "pendientes o en ejecución", que es lo que pide §4.1.2.
3. Completar *Evaluar recursos* marcando solo *Aula virtual* abre únicamente esa rama.
4. Completar *Preparar materiales* desde la app de escritorio del §4.2.1.
5. Enviar la nómina desde la app de comunicaciones y completar *Confeccionar lista definitiva*: ahí cierra la preparación y el proceso envía la confirmación.

Tests: `HappyPathTest` (las tres ramas de punta a punta), `TaskAssignmentTest` (grupo candidato, reclamo y asignación al menos ocupado) y `ClassroomUnavailableTest` (falta de aulas).

## 4. Preguntas probables

**¿Por qué una compuerta inclusiva y no tres exclusivas o una paralela?**
Porque la cantidad de ramas no se sabe en diseño: depende de lo que elija el administrativo. La paralela abriría siempre las tres y la exclusiva solo una. La inclusiva abre las que cumplen su condición y el join espera justo a esas.

**¿Qué pasa si no se selecciona ningún recurso?**
El modelo quedaría esperando un join que nunca recibe tokens. Por eso la validación "al menos una" está en la pantalla, antes de completar la tarea. La misma regla la exige el enunciado.

**¿Por qué *Preparar materiales* no tiene nada especial en el modelo si tiene una app propia?**
Porque la app no necesita nada especial: es una userTask de grupo como cualquier otra. La app del §4.2.1 la consulta, la reclama y la completa con la API Java. El modelo no sabe —ni tiene por qué saber— desde dónde se completó.

**¿Cómo se sabe que las ramas corren realmente en paralelo y no una después de la otra?**
Porque las tres tareas están abiertas al mismo tiempo (se ven juntas en la bandeja y en la pantalla de la empresa), y el análisis de §4.3 mide la duración de cada rama desde la **misma** activación: el instante en que la compuerta paralela abrió las tres.

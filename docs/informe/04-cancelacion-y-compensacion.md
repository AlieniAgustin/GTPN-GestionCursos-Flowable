# 04 — Cancelación durante la preparación y compensación

> **Explica:** Alieni · **Cubre:** §1.3 (cancelación durante la preparación)
>
> Diseño detallado: [`docs/cancelacion-durante-preparacion.md`](../cancelacion-durante-preparacion.md) · ADRs: [`cancelacion-no-interruptiva.md`](../../adr/cancelacion-no-interruptiva.md), [`compensacion-bpmn.md`](../../adr/compensacion-bpmn.md)

---

## 1. Qué pide el enunciado

Mientras se ejecuta el subproceso *Preparar curso*, la empresa puede solicitar la cancelación. Al recibirla:

1. Se **interrumpe** la preparación.
2. Se **compensan** las acciones de recursos que hayan **finalizado correctamente**: liberar el aula física, liberar el aula virtual y liberar el equipamiento, **según corresponda**.
3. Solo deben ejecutarse las compensaciones de los recursos **efectivamente gestionados**.
4. Una vez concluidas, se notifica el cierre a la empresa y finaliza el proceso.

## 2. Cómo se resolvió

### La interrupción

Un **subproceso de evento** (`cancelacion`, `triggeredByEvent="true"`) dentro de `prepararcurso`, con un start event de mensaje **interruptor** (`isInterrupting="true"`) suscripto a `Cancelación`.

Al llegar el mensaje, el motor cancela todo lo que esté abierto en la preparación —las tres ramas, con sus tareas— y le pasa el control al subproceso de evento. No hace falta cancelar nada a mano.

```text
[Preparar curso]
   ├─ (las tres ramas corriendo)
   └─ ⬡ Cancelación (subproceso de evento, interruptor)
         └─ Recibir Cancelación (mensaje)
               ├─► Compensar equipamiento   ⟲
               ├─► Compensar aula            ⟲
               ├─► Compensar aula virtual    ⟲
               ├─► Registrar Cancelación (cancelado = true)
               └─► Confirmar liberación de recursos (userTask, grupo administracion)
                        │
                        ▼
                     fin del subproceso
                        │
      ◇ ¿Cancelado? ── sí ─► Notificar Cierre ─► FIN (resultadoCurso = CANCELADO)
                     └─ no ─► Enviar confirmación ─► (sigue el curso)
```

### Por qué solo se compensa lo gestionado

Esto no se programó: es cómo funciona la compensación en BPMN y es la razón de haberla usada.

- Cada actividad de recursos tiene un **evento de borde de compensación** con `cancelActivity="false"`, y una actividad `isForCompensation="true"` asociada:

  | Actividad original | Compensación | Tipo |
  |---|---|---|
  | Reservar aula | Liberar Aula | `serviceTask` con `ReleaseClassroomDelegate` |
  | Solicitar Aula Virtual | Liberar Aula Virtual | `scriptTask` |
  | Contratar equipamiento | Liberar Equipamiento | userTask del grupo `administracion` |

- El motor registra un *compensation handler* **solo cuando la actividad original terminó bien**. Los tres eventos de lanzamiento de compensación del subproceso de cancelación piden compensar las tres actividades, pero el motor ejecuta únicamente las que tienen handler registrado.

Resultado: si el curso solo había reservado aula física, se libera el aula y nada más. Si no se había gestionado ningún recurso todavía, no se ejecuta ninguna compensación. Es exactamente el punto 3 del enunciado, sin un solo `if`.

### El cierre

*Registrar Cancelación* deja `cancelado = true`. Cuando el subproceso termina, el flujo de salida pasa por la compuerta **¿Cancelado?**, que distingue los dos motivos por los que un subproceso puede haber terminado:

- `cancelado == true` → *Notificar Cierre* → fin, con `resultadoCurso = CANCELADO`.
- si no → *Enviar confirmación* y el curso sigue a la etapa de dictado.

Esa compuerta es una de las modificaciones al diagrama de cátedra: sin ella, una preparación interrumpida y una completada salen por el mismo flujo y el proceso le enviaría la confirmación del curso a una empresa que acaba de cancelar.

### El listener que cierra la escucha

Hay un detalle del motor que vale contar porque fue un problema real: un subproceso que tiene a la vez eventos de borde y un subproceso de evento **no termina solo**. La ejecución que espera el mensaje de cancelación queda viva y la preparación nunca cierra, aunque las tres ramas hayan terminado.

Lo resuelve [`CloseCancellationListener`](../../flowable/src/main/java/com/flowable/app/listeners/CloseCancellationListener.java), un `ExecutionListener` en el evento de fin del subproceso: busca entre los hijos del scope la ejecución que está esperando el mensaje y la elimina. Así la preparación cierra y el proceso sigue a *Enviar confirmación*.

### La misma compensación, reutilizada al final

Al finalizar **normalmente**, el proceso lanza un evento de compensación sobre todo el subproceso `prepararcurso` (`liberarrecurso`, con `activityRef="prepararcurso"`). Libera los recursos con las mismas actividades de compensación, sin duplicar lógica. Ver [05](05-desarrollo-y-finalizacion.md).

## 3. Cómo se demuestra

Es el **escenario 6** de la defensa. Con una instancia en preparación que ya tenga recursos gestionados:

1. Contratar equipamiento y/o reservar aula (completar esas actividades).
2. Desde la **app de comunicaciones**, con la instancia seleccionada, aparece *Cancelar el curso*: enviarlo.
3. Las tareas abiertas desaparecen: la preparación se interrumpió.
4. Queda *Liberar Equipamiento* (la compensación con intervención humana) y *Confirmar liberación de recursos* en la bandeja del grupo.
5. Al completarlas, la instancia cierra con `resultadoCurso = CANCELADO` y el curso queda `CANCELADO` en la BD de la institución.

Las variables `aulaLiberada`, `aulaVirtualLiberada` y `equipoLiberado` muestran qué se compensó.

`CompensationTest` cubre tres casos: solo aula, solo equipamiento, y los dos juntos; en cada uno verifica que se compense **únicamente** lo gestionado.

## 4. Preguntas probables

**¿Por qué un subproceso de evento y no un evento de borde interruptor sobre la preparación?**
Un evento de borde saca el token **fuera** del subproceso, y ahí ya se pierde el contexto para compensar sus actividades internas y para que un administrativo confirme la liberación. El subproceso de evento mantiene el alcance de `prepararcurso`, que es justo lo que la compensación necesita. Está argumentado en [`adr/cancelacion-no-interruptiva.md`](../../adr/cancelacion-no-interruptiva.md).

**¿Cómo sabe el motor qué recursos se gestionaron?**
Por los handlers de compensación: se registran al completarse cada actividad original. No hay variables de control involucradas en esa decisión; las variables `aulaLiberada` y compañía son solo para poder verificarlo desde los tests y la demo.

**¿Qué pasa si la cancelación llega cuando la preparación ya terminó?**
No llega: `CloseCancellationListener` da de baja la suscripción al mensaje cuando el subproceso cierra. La app de comunicaciones, además, deja de ofrecer *Cancelar* porque le pregunta al motor qué mensajes están habilitados y ya no hay ninguna ejecución suscripta.

**¿Y si la empresa cancela antes de aceptar la propuesta?**
No corresponde: el enunciado limita la cancelación al subproceso de preparación. Antes de aceptar, la empresa tiene el rechazo. El mensaje `Cancelación` solo está suscripto dentro de `prepararcurso`.

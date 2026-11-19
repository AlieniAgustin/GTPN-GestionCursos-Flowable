# Escenario 6 — Cancelación durante la preparación, con compensación

> **Explica:** Alieni · **Cubre:** §1.3 del enunciado
> · Detalle técnico: [`04-cancelacion-y-compensacion.md`](../04-cancelacion-y-compensacion.md)

---

## Lo importante a mostrar

Dos cosas:

1. **La cancelación interrumpe la preparación.** Las tareas que estaban abiertas
   desaparecen de golpe, sin que nadie las cancele una por una.
2. **Se compensa solo lo que se había gestionado.** Si el curso había reservado aula pero
   no equipamiento, se libera el aula y nada más. Y eso **no está programado con `if`**:
   es cómo funciona la compensación en BPMN.

---

## Punto de partida

El curso **"Trabajo en Equipo"** ya fue cancelado por la empresa. La preparación se
interrumpió, las compensaciones automáticas corrieron y quedó esperando la confirmación
humana de la liberación.

---

## Paso a paso

### 1. Ver en Flowable dónde quedó

En la **Task App**, abrir la instancia de *"Trabajo en Equipo"*.

Está parada en **Confirmar liberacion de recursos**, que vive **dentro del recuadro de
Cancelación** del diagrama — ese subproceso chico que está abajo a la derecha del
subproceso *Preparar curso*.

Lo que conviene señalar: **las tareas de la preparación ya no están**. *Preparar
materiales*, *Evaluar recursos*, *Recibir nómina* — todas se cancelaron cuando llegó el
mensaje. El token está adentro del recuadro de cancelación.

### 2. Mostrar qué se compensó

En el diagrama se ven las tres actividades de compensación colgando de las originales,
con la flechita de rebobinado:

| Actividad original | Su compensación |
|---|---|
| Reservar aula | Liberar Aula |
| Solicitar Aula Virtual | Liberar Aula Virtual |
| Contratar equipamiento | Liberar Equipamiento |

Este curso tenía **aula física reservada**, así que corrió *Liberar Aula*. No tenía aula
virtual ni equipamiento, así que esas dos compensaciones **no se ejecutaron**.

### 3. Confirmar la liberación, desde el sitio

En `http://localhost:8081`, entrar con **`administrativo1` / `password`**.

1. Ir a **Mis tareas**.
2. En *Disponibles para mi grupo* está **Confirmar liberacion de recursos**. Es una tarea
   del grupo Administración, así que primero hay que **Reclamar** y después **Completar**.

> Ese paso de reclamar es parte del enunciado: las tareas abiertas a varias personas se
> reclaman antes de completarse, y una vez reclamada deja de estar disponible para los
> demás.

### 4. El cierre

Al completarla, el subproceso de cancelación termina y la instancia se cierra.

En el diagrama se ve el camino final: la compuerta **¿Cancelado?** manda el flujo a
*Notificar Cierre* en vez de a *Enviar confirmación*. O sea, a la empresa que canceló se
le avisa el cierre; no se le manda la confirmación de un curso que ya no va.

---

## Lo que hay por detrás (para señalar en el diagrama)

**La interrupción.** El recuadro *Cancelación* es un **subproceso de evento** con un
mensaje de arranque **interruptor**. Apenas llega el mensaje `Cancelación`, el motor
cancela todo lo que esté abierto dentro de *Preparar curso* y le pasa el control a ese
subproceso.

**Por qué se compensa solo lo gestionado.** Cada actividad de recursos tiene un evento de
borde de compensación. El motor registra el "handler" de compensación **solo cuando la
actividad original terminó bien**. Después, el subproceso de cancelación pide compensar
las tres, pero el motor ejecuta únicamente las que tienen handler registrado.

Por eso, si no se había gestionado ningún recurso, no se ejecuta ninguna compensación. Es
exactamente lo que pide el enunciado, sin una sola condición escrita.

**Cómo sabe que fue una cancelación.** Dentro del subproceso hay un script que deja la
marca:

```groovy
execution.setVariable("cancelado", true)
```

y la compuerta **¿Cancelado?** la lee para elegir entre *Notificar Cierre* y *Enviar
confirmación*. Esa compuerta es una de las modificaciones que le hicimos al diagrama de
cátedra: sin ella, una preparación interrumpida y una completada salen por el mismo lado.

---

## Si preguntan

**¿Por qué un subproceso de evento y no un evento de borde sobre la preparación?**
Un evento de borde saca el token **afuera** del subproceso, y ahí se pierde el contexto
para compensar lo de adentro y para que un administrativo confirme la liberación. El
subproceso de evento mantiene el alcance de *Preparar curso*, que es lo que la
compensación necesita.

**¿Qué pasa si la cancelación llega cuando la preparación ya terminó?**
No llega: cuando el subproceso cierra se da de baja la suscripción al mensaje. La app de
comunicaciones directamente deja de ofrecer *Cancelar el curso*, porque le pregunta al
motor qué está habilitado.

**¿Y si la empresa quiere cancelar antes de aceptar la propuesta?**
No corresponde. El enunciado limita la cancelación a la etapa de preparación; antes de
aceptar, lo que tiene la empresa es el rechazo (escenario 3).

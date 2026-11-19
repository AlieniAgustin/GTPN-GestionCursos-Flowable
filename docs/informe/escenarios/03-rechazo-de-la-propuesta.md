# Escenario 3 — Rechazo de la propuesta

> **Explica:** Alieni · **Cubre:** §1.1 del enunciado
> · Detalle técnico: [`02-viabilidad-y-acuerdo.md`](../02-viabilidad-y-acuerdo.md)

---

## Lo importante a mostrar

**Que el proceso está esperando cuatro cosas a la vez** —que la empresa acepte, rechace,
pida modificaciones o que venza el plazo— y que apenas llega una, las otras tres se
cancelan solas. Eso es una **compuerta basada en eventos**, y no hay una línea de código
que descarte las demás: lo hace el motor.

---

## Punto de partida

El curso **"Seguridad e Higiene"** ya está preparado: tiene la propuesta enviada y está
esperando la respuesta de la empresa.

En la **Task App** de Flowable se puede abrir esa instancia antes de empezar y mostrar que
está parada en **Esperar respuesta**, con las cuatro salidas abiertas en paralelo.

---

## Paso a paso

### 1. Rechazar, desde la app de la empresa

En la **app de comunicaciones** (la ventana Java):

1. En la lista de solicitudes, hacer clic en **"Seguridad e Higiene"**.
2. Abajo aparecen las comunicaciones habilitadas: *Aceptar la propuesta*, *Rechazar la
   propuesta* y *Pedir modificaciones*. **Solo esas tres**, porque son las que el motor
   tiene abiertas en este punto.
3. Elegir **Rechazar la propuesta** y apretar **Enviar**.

El rechazo no pide ningún dato extra: se manda solo.

### 2. Ver que el proceso avanzó

Volver a Flowable y abrir la instancia de nuevo. Ahora está parada en **Archivar
solicitud**, y las otras tres salidas de la compuerta desaparecieron.

### 3. Archivar, desde el sitio

En `http://localhost:8081`, entrar con **`jefeadministrativo` / `password`**.

1. Ir a **Mis tareas**.
2. En *Asignadas a mí* está **Archivar solicitud**, del curso *Seguridad e Higiene*. Viene
   ya asignada: es una tarea exclusiva del jefe administrativo, no hay que reclamarla.
3. Apretar **Completar**.

### 4. El cierre

La instancia terminó. En Flowable ya no aparece entre las activas, y el recorrido completo
quedó:

```
… → Enviar propuesta → Esperar respuesta → Rechazo → Archivar solicitud → FIN
```

---

## Lo que hay por detrás (para señalar en el diagrama)

**La compuerta de espera.** *Esperar respuesta* es un **eventBasedGateway**: no es una
decisión de la institución, es una espera. Sus cuatro salidas son eventos externos —tres
mensajes y un temporizador— y gana el primero que llega.

**El resultado del cierre.** El flujo que va de la compuerta hacia *Archivar solicitud*
por el lado del rechazo lleva el listener que marca cómo terminó:

```xml
${execution.setVariable('resultadoCurso','RECHAZADO')}
```

Esa variable es la que después lee el listener de fin del proceso para registrar el estado
del curso en la base de datos de la institución.

---

## Si preguntan

**¿Por qué una compuerta basada en eventos y no una exclusiva?**
Porque la institución no elige nada: espera. Una compuerta exclusiva evalúa una condición
con datos que ya tiene; acá los cuatro caminos dependen de algo que pasa afuera.

**¿Qué pasa si la empresa acepta y rechaza casi al mismo tiempo?**
Gana el primer mensaje que el motor procesa. El segundo se encuentra con que la ejecución
ya no existe. Además, la app de comunicaciones solo ofrece lo que el motor declara
habilitado en ese instante, así que después del rechazo esas opciones ya no aparecen.

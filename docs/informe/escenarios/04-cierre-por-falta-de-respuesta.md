# Escenario 4 — Cierre por falta de respuesta, luego de los recordatorios

> **Explica:** Alieni · **Cubre:** §1.1 del enunciado
> · Detalle técnico: [`02-viabilidad-y-acuerdo.md`](../02-viabilidad-y-acuerdo.md)

---

## Lo importante a mostrar

**Que el plazo no es una fecha fija en el diagrama** y que el proceso se cierra solo
después de insistir **dos veces**, sin que nadie tenga que acordarse de hacerlo.

---

## Punto de partida

El curso **"Normativa Laboral"** ya recorrió todo el ciclo de espera: se le envió la
propuesta, venció el plazo dos veces, salieron los dos recordatorios y quedó parado en
**Archivar solicitud**.

Esto es lo que en la vida real tardaría quince días. En la demo ya está hecho.

---

## Paso a paso

### 1. Ver el recorrido en Flowable

En la **Task App**, abrir la instancia de *"Normativa Laboral"*. Lo interesante es el
camino que hizo, que se ve como un **bucle** en el diagrama:

```
Enviar propuesta → Esperar respuesta → 5 días → Enviar recordatorio
                 ↘ Esperar respuesta → 5 días → Enviar recordatorio
                 ↘ Esperar respuesta → 5 días → ¿ya van 2? → sí → Archivar solicitud
```

Pasó tres veces por el mismo tramo. Las dos primeras volvió a esperar; la tercera salió
por el otro lado de la compuerta.

### 2. Archivar, desde el sitio

En `http://localhost:8081`, entrar con **`jefeadministrativo` / `password`**.

1. **Mis tareas** → está **Archivar solicitud** del curso *Normativa Laboral*.
2. Apretar **Completar**.

La instancia cierra con el resultado *cerrado por falta de respuesta*.

---

## Lo que hay por detrás (para señalar en el diagrama)

**El temporizador no tiene fecha.** El evento *5 días* usa una expresión, no una duración
escrita en el modelo:

```xml
<timeDuration>${plazoRespuesta}</timeDuration>
```

Es una variable del proceso. Para una demo en vivo se puede bajar a un minuto sin tocar el
diagrama, cambiando la configuración del motor. Acá no hace falta porque el escenario ya
viene recorrido.

**El script que lleva la cuenta.** La actividad *Enviar recordatorio* es un script task que
incrementa el contador y solo manda el aviso si todavía no se llegó al máximo:

```groovy
def n = (execution.getVariable('recordatorios') ?: 0) + 1
execution.setVariable('recordatorios', n)
if (n <= 2) {
  println "Recordatorio ${n} enviado"
}
```

Son **dos recordatorios**, como pide el enunciado. El nodo se atraviesa una tercera vez,
pero esa vuelta no envía nada: solo pasa de largo hacia el archivo.

**La compuerta que corta el bucle.** *¿Se enviaron 2 recordatorios?* compara la variable y
decide si se vuelve a esperar o se archiva. En su salida por *sí* —el flujo
`flow_recordatorios_si`— está el listener que marca el resultado:

```xml
${execution.setVariable('resultadoCurso','SIN_RESPUESTA')}
```

**El contador se reinicia.** Si la empresa hubiera pedido modificaciones, al reenviar la
propuesta el contador vuelve a cero y tiene de nuevo sus dos recordatorios. Eso lo hace la
actividad *Enviar propuesta*, y es un requisito explícito del enunciado.

---

## Si preguntan

**¿Cómo se garantiza que no haya más de dos recordatorios?**
Por la combinación del script, que no envía cuando ya van dos, y de la compuerta, que
manda la instancia a archivar en la tercera vuelta.

**¿Por qué el plazo es una variable y no una duración fija?**
Para poder demostrarlo sin esperar cinco días y porque el plazo es una regla de negocio
que puede cambiar. Si estuviera escrito en el diagrama habría que rediseñar y redesplegar
el proceso para modificarlo.

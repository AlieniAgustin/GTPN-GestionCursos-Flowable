# Minería de procesos — etapa de negociación

Análisis de las ejecuciones reales de la etapa previa a la preparación del curso
(enunciado §4.4), hecho con **PM4Py** sobre el historial del motor.

- **Event log:** [`negociacion.csv`](negociacion.csv) — 330 eventos, 44 instancias, 15 actividades
- **Gráficos:** [`resultados/dfg-negociacion.png`](resultados/dfg-negociacion.png) y
  [`resultados/heuristica-negociacion.png`](resultados/heuristica-negociacion.png)
- **Reproducir:** ver [`README.md`](README.md)

---

## 1. Comportamiento observado

Las 44 instancias con la negociación concluida recorrieron **6 caminos distintos**. Los
tres más frecuentes cubren el **77,3 %**:

| # | Instancias | % | Recorrido |
|---|---|---|---|
| 1 | 15 | 34,1 % | Solicitud recibida → Evaluar Viabilidad → **Notificar Decisión** |
| 2 | 13 | 29,5 % | … → Elaborar propuesta → Enviar propuesta → Esperar respuesta → **Rechazo** → Archivar → Cerrada |
| 3 | 6 | 13,6 % | … → Esperar respuesta → (5 días → Recordatorio) **×3** → Archivar → Cerrada ⁽*⁾ |

⁽*⁾ El nodo se atraviesa tres veces pero solo envía dos recordatorios: ver 3.1.

Los tres restantes:

| # | Instancias | % | Recorrido |
|---|---|---|---|
| 4 | 5 | 11,4 % | … → Esperar respuesta → **Aceptación** → Habilitar acceso de la empresa |
| 5 | 4 | 9,1 % | … → **Modificaciones** → Revisar propuesta → Enviar propuesta → Esperar respuesta → Aceptación |
| 6 | 1 | 2,3 % | … → **Modificaciones** → Revisar propuesta → Enviar propuesta → Esperar respuesta → Rechazo |

---

## 2. Coincidencias con el modelo BPMN

El comportamiento observado **reproduce la estructura del modelo**, sin caminos que el
diagrama no contemple:

- **La compuerta de viabilidad funciona en los dos sentidos.** El camino más frecuente
  (34,1 %) es el de las solicitudes no viables, que salen por *Notificar Decisión* sin
  llegar a elaborar propuesta. Es el resultado esperable de haber generado los casos con
  cinco cursos ya en ejecución (§3.1).
- **Las cuatro salidas de la compuerta de eventos aparecen todas**: aceptación, rechazo,
  modificaciones y vencimiento del plazo. Ninguna quedó sin ejercitar.
- **El ciclo de renegociación se cierra bien.** En los recorridos 5 y 6 se ve
  *Modificaciones → Revisar propuesta → Enviar propuesta → Esperar respuesta*, o sea que
  la instancia vuelve a la misma compuerta. El modelo permite repetirlo sin límite y el
  log muestra que el ciclo efectivamente reingresa.
- **Todos los caminos terminan donde deben**: *Habilitar acceso de la empresa* cuando se
  acepta, y *Solicitud cerrada* cuando se archiva.

---

## 3. Diferencias encontradas

### 3.1 El log sugiere tres recordatorios, pero solo se envían dos

Es el hallazgo principal y aparece en el recorrido 3:

```
Esperar respuesta → 5 días → Enviar recordatorio
Esperar respuesta → 5 días → Enviar recordatorio
Esperar respuesta → 5 días → Enviar recordatorio   ← tercera vuelta
Archivar solicitud
```

Leído así, parece que el proceso manda un recordatorio de más: el enunciado (§1.1) dice
que *"se enviarán como máximo dos recordatorios"*. **No es lo que ocurre.** El script de
la actividad incrementa el contador siempre, pero solo envía cuando todavía no llegó al
máximo:

```groovy
def n = (execution.getVariable('recordatorios') ?: 0) + 1
execution.setVariable('recordatorios', n)
if (n <= 2) {
  println "Recordatorio ${n} enviado"
}
```

Verificado contra el motor: en un caso sin respuesta el log imprime exactamente
`Recordatorio 1 enviado` y `Recordatorio 2 enviado`, y nada más. El comportamiento
**cumple el enunciado**.

Lo que la minería expone es una **discrepancia entre el modelo y la implementación**: el
control de cuántos recordatorios se mandan vive dentro del script, no en el flujo. En el
diagrama, la compuerta `seenviaron2recordatorios` evalúa `${recordatorios <= 2}` *después*
de pasar por la actividad, así que el token atraviesa *Enviar recordatorio* tres veces y
la tercera no hace nada. El event log —que solo ve las actividades, no el código de
adentro— registra tres ejecuciones de una actividad que se llama "Enviar recordatorio".

Es exactamente el tipo de cosa para la que sirve esta técnica: el log no miente sobre lo
que el motor ejecutó, pero el modelo no cuenta toda la historia. Un analista que mirara
solo el diagrama y el log concluiría que se mandan tres.

> **Mejora sugerida (no es un defecto):** mover la decisión al flujo, evaluando la
> compuerta **antes** de la actividad —`5 días → ¿ya van 2? → sí: archivar / no: enviar
> recordatorio`—. El comportamiento sería el mismo, pero el modelo quedaría
> autoexplicativo y el log dejaría de mostrar una vuelta que no envía nada.

### 3.2 El modelo registra más actividad de la que ocurre

En el historial crudo, cada instancia que llega a *Esperar respuesta* deja registradas
**las cuatro** ramas de la compuerta —aceptación, rechazo, modificaciones y el plazo—
aunque solo una haya ocurrido. Las otras tres quedan marcadas con
`DELETE_REASON_ = 'event based gateway cancel'`.

No es una diferencia de comportamiento sino de **registro**, pero cambia por completo el
análisis: sin filtrarlas, el log dice que cada instancia fue aceptada *y* rechazada *y*
modificada, y los recorridos pasan de 6 a 19 sin significar nada. Por eso el exportador
descarta esas filas (ver la limitación 4.2).

---

## 4. Limitaciones del análisis

**4.1 Los datos son sintéticos.** Las 44 instancias las generó
[`generar_casos.py`](generar_casos.py) recorriendo a propósito los cinco caminos del
modelo. Las proporciones —34 % no viables, 29 % rechazos— reflejan esa mezcla elegida, no
el comportamiento de empresas reales. Lo que sí es válido es la **estructura**: qué
caminos existen, cuáles se encadenan y en qué orden.

**4.2 El log depende de leer la base, no la API.** El campo que distingue las ramas
canceladas no lo expone la API REST de Flowable, solo la tabla `ACT_HI_ACTINST`. Con
`--fuente rest` el análisis es inutilizable por lo dicho en 3.2. Es una limitación
concreta a tener en cuenta si se quisiera repetir esto contra un motor al que solo se
llega por HTTP.

**4.3 Los tiempos no son interpretables.** Los plazos de respuesta se dispararon a mano
con la API de temporizadores para no esperar cinco días reales, así que las duraciones del
log no dicen nada sobre cuánto tarda de verdad una negociación. El análisis es de
**flujo**, no de performance.

**4.4 Se analizan solo las negociaciones concluidas.** Las 13 instancias que siguen
esperando respuesta quedan afuera: tienen las cuatro ramas de la compuerta abiertas a la
vez —porque de verdad están esperando en paralelo— y eso no es un recorrido sino una foto
a mitad de camino. Incluirlas agregaba variantes que no corresponden a ningún camino real.

**4.5 Seis caminos son pocos para técnicas de descubrimiento.** Con esta cantidad de
variantes, el DFG y la red heurística reproducen el modelo casi exactamente, lo cual
confirma la implementación pero no aporta hallazgos por sí solo. El valor del análisis
acá estuvo en comparar las **secuencias observadas** contra lo que pide el enunciado, que
es lo que destapó la diferencia entre lo que el diagrama sugiere y lo que el proceso hace.

---

## 5. Conclusión

La implementación respeta la estructura de la etapa de negociación: los seis recorridos
observados son todos caminos previstos en el diagrama y no apareció ninguno espurio. La
compuerta de viabilidad, las cuatro respuestas posibles de la empresa y el ciclo de
renegociación se comportan como el modelo indica. **No se encontró ninguna desviación
respecto de lo que pide el enunciado.**

El análisis sí dejó una observación que vale la pena: el event log sugiere que se envían
tres recordatorios cuando en realidad se envían dos, porque el límite está dentro del
script de la actividad y no en el flujo del diagrama. El comportamiento es correcto, pero
modelo y log cuentan historias distintas. Es un buen ejemplo de para qué sirve la minería
—y también de su principal limitación: ve lo que el motor ejecutó, no lo que el código
decidió adentro—. Mover esa decisión a la compuerta dejaría el modelo autoexplicativo.

Sobre la técnica en sí: con seis variantes y datos generados a propósito, el valor no
estuvo en el descubrimiento automático —el DFG reproduce el modelo casi exactamente— sino
en **comparar las secuencias observadas contra el enunciado**, actividad por actividad.
Ahí es donde apareció la diferencia entre lo que el diagrama sugiere y lo que el proceso
hace.

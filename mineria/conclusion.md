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
| 3 | 6 | 13,6 % | … → Esperar respuesta → (5 días → Recordatorio) **×3** → Archivar → Cerrada |

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

### 3.1 El modelo envía tres recordatorios, no dos

Es el hallazgo principal y surge directamente del recorrido 3:

```
Esperar respuesta → 5 días → Enviar recordatorio
Esperar respuesta → 5 días → Enviar recordatorio
Esperar respuesta → 5 días → Enviar recordatorio   ← uno de más
Archivar solicitud
```

El enunciado (§1.1) dice que *"se enviarán como máximo dos recordatorios"* y que
*"si transcurren otros cinco días después del segundo sin recibir respuesta, se archiva
la solicitud"*. El log muestra **tres envíos** en el 100 % de los casos sin respuesta
(6 de 6).

La causa está en la compuerta `seenviaron2recordatorios` del BPMN:

```xml
flow_recordatorios_no → esperarrespuesta    ${recordatorios <= 2}
flow_recordatorios_si → archivarsolicitud   ${recordatorios > 2}
```

El contador se incrementa **antes** de evaluar la condición, así que con
`recordatorios = 2` todavía entra por la rama *no* y manda un tercero. Las condiciones
correctas serían `${recordatorios < 2}` y `${recordatorios >= 2}`.

Vale la pena notar que **ningún test lo detectó**: `NoResponseTest` verifica que la
solicitud termine archivada por falta de respuesta, que es lo que efectivamente pasa, pero
no cuenta los recordatorios. La minería lo expuso porque mira la secuencia completa y no
solo el resultado.

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
es lo que destapó el problema de los recordatorios.

---

## 5. Conclusión

La implementación respeta la estructura de la etapa de negociación: los seis recorridos
observados son todos caminos previstos en el diagrama y no apareció ninguno espurio. La
compuerta de viabilidad, las cuatro respuestas posibles de la empresa y el ciclo de
renegociación se comportan como el modelo indica.

La minería sí detectó **una desviación respecto del enunciado**: el proceso envía tres
recordatorios en lugar de dos, por un error de borde en la condición de la compuerta. Es
un defecto que la suite de tests no cubría, porque los tests verifican el resultado final
—la solicitud archivada— y no la cantidad de vueltas del ciclo. Encontrar eso justifica el
análisis: es precisamente el tipo de discrepancia entre modelo y ejecución que esta
técnica busca.

> **Pendiente:** corregir las condiciones a `${recordatorios < 2}` / `${recordatorios >= 2}`
> y agregar al `NoResponseTest` una aserción sobre la cantidad de recordatorios. Después
> de eso conviene regenerar el log para que el entregable refleje el modelo corregido.

# 11 — Análisis de las ejecuciones y minería de procesos

> **Explica:** Cesari · **Cubre:** §4.3 (análisis de las ejecuciones), §4.4 (introducción a la minería de procesos)
>
> Conclusión de la minería: [`mineria/conclusion.md`](../../mineria/conclusion.md)

---

## Parte A — Análisis de las ejecuciones (§4.3)

### Qué pide el enunciado

Tres consultas sobre la **información histórica** del motor:

1. **Resultados de las solicitudes:** cantidad y porcentaje de cada resultado —curso finalizado, cancelación, rechazo, no viable, cierre por falta de respuesta—, con los porcentajes sobre el total de instancias **terminadas**.
2. **Análisis de la preparación paralela:** duración promedio de las ramas de inscripciones, materiales y recursos, medida **desde su activación hasta su finalización**. La rama de recursos termina cuando se completaron **todas las acciones seleccionadas**. Se **excluyen** las preparaciones interrumpidas por cancelación.
3. **Distribución de tareas:** cantidad de tareas de usuario completadas por cada administrativo y docente, **excluyendo** las pendientes y las interrumpidas o canceladas.

Se pueden incorporar a la app de materiales **o** mostrarse por consola.

### Cómo se resolvió

Están **las dos formas**: el botón *Análisis de ejecuciones* en la app de materiales y un informe por consola, que es el que conviene para la captura de la entrega. El código está en [`materiales/src/main/java/com/materiales/app/analysis/`](../../materiales/src/main/java/com/materiales/app/analysis/) y consulta el historial por la **API Java** (`HistoryService`).

```bash
java -cp materiales/target/app.jar com.materiales.app.analysis.AnalysisApp
```

### Cómo se mide cada cosa

**1. Resultados.** Instancias terminadas (`finished()`) con sus variables, leyendo `resultadoCurso` —la que dejan los cinco caminos de cierre de §3.4—. Los porcentajes van sobre el total de terminadas y **las cinco filas aparecen siempre**, aunque estén en cero. Si apareciera una instancia terminada sin resultado se muestra en una fila aparte, pero `ClosingPathsTest` garantiza que no pase.

**2. Ramas de la preparación.** Lo más delicado del punto:

- La **activación** es el fin del gateway `preparacionenparalelo`: es el instante en que las tres ramas se abren, así que es **la misma para las tres**. Eso hace comparables los tres promedios.
- El **cierre** de cada rama es la última finalización de su actividad: `confeccionarlistadefinitiva`, `prepararmateriales` y —para recursos— **la más tardía** entre `reservaraula`, `solicitaraulavirtual` y `contratarequipamiento`. Esa "más tardía" es el *"cuando se hayan completado todas las acciones seleccionadas"* del enunciado: las acciones que no se seleccionaron **no están en el historial**, así que no hay que filtrarlas.
- Se toma la **última** finalización y no la primera porque `reservaraula` se repite si hubo que gestionar la falta de aulas.
- Se miden **solo** las preparaciones que llegaron a `finprepararcurso`. Una cancelación interrumpe el subproceso **antes** de ese punto, así que queda excluida sola, sin una regla aparte. El informe además dice cuántas excluyó por cancelación, cuántas están en curso y cuántas instancias nunca entraron en preparación.

**3. Distribución de tareas.** `finished()` más `taskWithoutDeleteReason()`, agrupado por responsable. Ese segundo filtro es el que deja afuera las pendientes y las que el motor **borra** al interrumpir la preparación por una cancelación. Los cinco integrantes aparecen siempre, aunque tengan cero.

### Cómo se ve

```text
1) Resultados de las solicitudes (§4.3.1)
   Instancias terminadas: 12

   Resultado                            Cantidad     Porcentaje
   ---------------------------------- ---------- --------------
   Curso finalizado                            5          41.7%
   Cancelación                                 2          16.7%
   Rechazo de la propuesta                     3          25.0%
   Solicitud no viable                         1           8.3%
   Cierre por falta de respuesta               1           8.3%
   ---------------------------------- ---------- --------------
   Total                                      12         100.0%

2) Preparación en paralelo (§4.3.2)
   Preparaciones completadas normalmente: 5
   Excluidas: 2 por cancelación, 0 en curso

   Rama                                 Muestras       Promedio
   ---------------------------------- ---------- --------------
   Inscripciones                               5     1 h 12 min
   Materiales                                  5    35 min 10 s
   Recursos                                    5      2 h 4 min

3) Distribución de tareas de usuario (§4.3.3)
   ...
```

### Tests

**10 tests** dentro de los 26 del módulo. Los de las consultas corren contra un esqueleto BPMN que copia del modelo real los ids de las actividades, el subproceso de preparación con sus tres ramas y el de evento de cancelación, y **mueven el reloj del motor a mano**: así las duraciones salen exactas (5, 10 y 20 minutos) y no dependen de lo que tarde la máquina. Montan tres instancias: una preparación normal, una cancelada y una solicitud que nunca entró en preparación.

---

## Parte B — Minería de procesos (§4.4)

### Qué pide el enunciado

Con PM4Py, un análisis breve de la **etapa de negociación** (todo lo previo a la preparación): generar el CSV de las actividades históricas adaptando el script provisto, obtener la representación gráfica, identificar los **tres recorridos más frecuentes** con cantidad y porcentaje, y redactar una conclusión comparando con el modelo BPMN —coincidencias, diferencias y limitaciones—. Se entregan el CSV, el código o las instrucciones, y una captura de los resultados.

### Cómo se resolvió

Todo en [`mineria/`](../../mineria/):

| Archivo | Qué hace |
|---|---|
| [`exportar_history.py`](../../mineria/exportar_history.py) | Arma el event log con los nombres de columna que espera PM4Py y recorta la etapa de negociación |
| [`generar_casos.py`](../../mineria/generar_casos.py) | Crea instancias recorriendo a propósito los cinco caminos, para tener con qué analizar |
| [`analizar.py`](../../mineria/analizar.py) | Genera el DFG y la red heurística, e imprime los recorridos con cantidad y porcentaje |
| [`negociacion.csv`](../../mineria/negociacion.csv) | El event log: 330 eventos, 44 instancias, 15 actividades |
| [`resultados/`](../../mineria/resultados/) | Los dos gráficos (DFG y heurística) |
| [`conclusion.md`](../../mineria/conclusion.md) | La conclusión |

**Una decisión que vale explicar:** el export **lee de la base, no de la API REST**. El motivo es del modelo: la compuerta basada en eventos hace que cada instancia que espera respuesta deje registradas las **cuatro** ramas, aunque solo una haya ocurrido, y las canceladas se distinguen únicamente por `DELETE_REASON_`, un campo que el REST no expone. Sin filtrar eso, el log pasa de 6 a 19 recorridos y deja de significar nada. Queda `--fuente rest` como alternativa, avisando de la limitación.

### Resultados

44 instancias con la negociación concluida, **6 recorridos distintos**. Los tres más frecuentes cubren el **77,3 %**:

| # | Instancias | % | Recorrido |
|---|---|---|---|
| 1 | 15 | 34,1 % | Solicitud recibida → Evaluar Viabilidad → **Notificar Decisión** (no viable) |
| 2 | 13 | 29,5 % | … → Enviar propuesta → Esperar respuesta → **Rechazo** → Archivar |
| 3 | 6 | 13,6 % | … → Esperar respuesta → (5 días → Recordatorio) → Archivar |

Los otros tres: aceptación directa (11,4 %), modificaciones y después aceptación (9,1 %) y modificaciones y después rechazo (2,3 %).

### Conclusión

El análisis **no encontró desviaciones** respecto del enunciado: los seis recorridos son todos caminos previstos en el diagrama. Las cuatro salidas de la compuerta de eventos aparecen todas, el ciclo de renegociación reingresa correctamente y todos los caminos terminan donde deben.

**La única observación es de modelado.** El log muestra que *Enviar recordatorio* se atraviesa **tres** veces, lo que leído de golpe parece incumplir el "máximo dos recordatorios" de §1.1. **No lo incumple**: se verificó contra el motor que imprime exactamente `Recordatorio 1 enviado` y `Recordatorio 2 enviado`. Lo que pasa es que el límite vive **dentro del script** y no en el flujo, así que el token da una vuelta de más sin hacer nada. La mejora —mover la decisión a la compuerta para que evalúe **antes** de la actividad— está anotada como opcional: el comportamiento no cambia, pero el modelo queda autoexplicativo y el log deja de mostrar una vuelta vacía.

**Limitaciones**, que es lo que el enunciado pide decir explícitamente: el log es de datos **generados por nosotros**, así que las frecuencias reflejan cómo se generaron los casos y no una demanda real —de ahí que el camino más frecuente sea el de las no viables, porque se generaron con cinco cursos ya en ejecución—; y las duraciones no son significativas, porque las instancias se recorrieron en minutos con los plazos acortados.

### Reproducirlo

```bash
python3 -m venv .venv && .venv/bin/pip install pm4py pandas requests psycopg2-binary
.venv/bin/python mineria/generar_casos.py
.venv/bin/python mineria/exportar_history.py
.venv/bin/python mineria/analizar.py
```

---

## Preguntas probables

**¿Por qué la activación de las tres ramas es la misma?**
Porque el gateway paralelo las abre en el mismo instante: es un único punto de bifurcación. Medir desde ahí es lo que hace comparables los tres promedios, y es la lectura literal de "desde su activación".

**¿Cómo se excluyen las preparaciones canceladas sin una consulta especial?**
Se piden solo las que llegaron al evento de fin del subproceso. La cancelación lo interrumpe antes, así que nunca pasa por ahí. El informe igual cuenta cuántas quedaron excluidas, para que se vea que no se perdieron.

**¿Por qué el análisis de §4.3 está en la app de materiales y no en el sitio?**
Porque el enunciado lo permite explícitamente y la app ya tiene el motor embebido por API Java: `HistoryService` queda a mano. Además, por consola es la forma más cómoda de sacar la captura que pide la entrega.

**¿Por qué el CSV se arma desde la base y no desde el REST, si el enunciado habla de la API?**
El enunciado pide generar el CSV a partir de las actividades históricas de Flowable, sin exigir el medio. Por REST el log queda contaminado con las ramas no tomadas de la compuerta de eventos y sin el motivo de borrado, que es lo único que distingue una cancelación. La alternativa por REST está implementada, pero avisa de esa limitación.

**¿No es trampa analizar datos que generaron ustedes?**
Es lo que permite el enunciado, que pide generar instancias para el análisis. Lo importante es decirlo: está escrito en las limitaciones de la conclusión, junto con qué sí se puede concluir —la estructura de los caminos— y qué no —las frecuencias y las duraciones—.

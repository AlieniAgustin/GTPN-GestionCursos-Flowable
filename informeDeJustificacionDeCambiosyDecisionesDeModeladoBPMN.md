# Informe de Modificaciones y Decisiones de Diseño BPMN

**Proyecto:** Gestión y Tecnologías de Procesos de Negocio — Organización de Cursos de Capacitación  
**Modelos comparados:** `cursosOriginal.bpmn` (Modelo base de la cátedra) vs. `cursosNuestraVersion.bpmn` (Modelo refinado por el equipo)

---

## 1. Introducción

El presente documento detalla y justifica las modificaciones realizadas sobre el modelo BPMN conceptual provisto por la cátedra (`cursosOriginal.bpmn`), dando lugar a nuestra versión refinada (`cursosNuestraVersion.bpmn`). Todos los ajustes fueron diseñados contemplando tanto las **7 Guías de Modelado de Procesos (7PMG)** y las **buenas prácticas de BPMN 2.0** vistas en la teoría, como los requerimientos técnicos de ejecución en el motor **Flowable** establecidos en el enunciado del proyecto.

---

## 2. Modificaciones Realizadas y Decisiones de Diseño

### 2.1. Incorporación de compuerta exclusiva (XOR) convergente antes de «Enviar propuesta»

* **Cambio realizado:** En `cursosOriginal.bpmn`, las flechas de secuencia salientes de las tareas `Registrar y Elaborar propuesta` y `Revisar propuesta` ingresaban directamente a la tarea de script `Enviar propuesta`. En `cursosNuestraVersion.bpmn`, colocamos una compuerta exclusiva convergente previa a `Enviar propuesta`, de modo que ambos flujos converjan en la compuerta y salga un único flujo hacia la tarea.
* **Justificación teórica (Buenas prácticas y Patrones):** 
  * Se elimina el *join implícito* sobre la actividad `Enviar propuesta`.
  * Se cumplen las guías **G2 (Minimizar los caminos de ruteo por elemento)**, evitando que una tarea tenga múltiples aristas de entrada, y **G4 (Modelar de la forma más estructurada posible)** de las 7PMG, cerrando explícitamente el ciclo de retrabajo/iteración.
* **Justificación técnica en Flowable (Por qué evita la duplicación de código):**
  * El enunciado (Sección 1.1) exige que: *"El contador de recordatorios se reinicia con cada nueva versión enviada de la propuesta"*. 
  * **¿Por qué antes había duplicación de código?** En el modelo original sin compuerta, si se deseaba inicializar o reiniciar la variable `${recordatorios = 0}` mediante un *Execution Listener* en las transiciones de entrada antes de ejecutar `Enviar propuesta`, estábamos obligados a configurar exactamente el mismo listener en **dos lugares distintos**: uno en la flecha que viene de `Registrar y Elaborar propuesta` y otro idéntico en la flecha (que viene de `Revisar propuesta`.
  * Al unificar ambos caminos en la compuerta, existe una única arista de entrada hacia `Enviar propuesta`. Esto permite centralizar la inicialización/reinicio de `${recordatorios = 0}` en un único punto (ya sea en el flujo o dentro del propio script de `Enviar propuesta`), eliminando cualquier redundancia y facilitando el mantenimiento.

---

### 2.2. Análisis de la tarea «Revisar propuesta» y actualización del Objeto de Datos «Propuesta y presupuesto»

* **Decisión sobre la granularidad de la tarea (Por qué NO agregamos una nueva tarea):**
  * Durante el análisis inicial, evaluamos dividir o agregar una nueva tarea inmediatamente después de `Revisar propuesta` (por ejemplo, *"Elaborar nueva versión de propuesta"*) para explicitar que tras la revisión se confecciona un nuevo documento. Sin embargo, **decidimos descartar esta idea y mantener una única tarea `Revisar propuesta`** por tres motivos fundamentales:
    
    1. **Alineación estricta con los requisitos de implementación:** En la Sección 4.1.3 del PDF (*Funciones para Administración*), el documento nombra explícitamente a esta única tarea y le asigna ambas responsabilidades en el sitio web: *"Completar la tarea Revisar propuesta, generando una nueva versión del PDF"*. Agregar otra tarea de usuario rompería la correspondencia 1 a 1 con las pantallas requeridas y obligaría al jefe administrativo a completar dos pasos consecutivos en la bandeja de entrada para una misma acción lógica.
    
    2. **Cumplimiento de las 7PMG:** Mantener una sola actividad respeta la guía **G1 (Usar la menor cantidad posible de elementos)**, evitando inflar el modelo con tareas secuenciales realizadas por el mismo actor (`Jefe administrativo`) en el mismo momento.
* **Cambio en las asociaciones del Objeto de Datos `Propuesta y presupuesto`:**
  * **Problema detectado en `cursosOriginal.bpmn`:** En el modelo original, el objeto de datos `Propuesta y presupuesto` solo tiene una asociación de entrada (`DataInputAssociation`) que apunta hacia `Revisar propuesta`. Esto modela que la tarea únicamente *lee* la propuesta original, pero no refleja que el documento sufre modificaciones y genera una nueva versión que luego debe ser leída por `Enviar propuesta`.
  * **Por qué se agrega la arista de salida (`DataOutputAssociation`):** Dado que en `Revisar propuesta` el jefe administrativo consulta la versión vigente junto con las observaciones de la empresa y **produce una nueva versión actualizada del programa, presupuesto y fechas**, la tarea actúa simultáneamente como **lectora y escritora** del artefacto. Por lo tanto, corresponde agregar una asociación de datos de salida (`DataOutputAssociation`, flecha punteada desde `Revisar propuesta` hacia `Propuesta y presupuesto`) para dejar expresado formalmente que el objeto de datos se actualiza antes de volver a ejecutarse `Enviar propuesta`.

---

### 2.3. Incorporación de compuerta exclusiva (XOR) convergente antes de «Archivar solicitud» (`Gateway_0bveuld`)

* **Cambio realizado:** En `cursosOriginal.bpmn`, tanto el flujo de rechazo (`Rechazado`) como el flujo de vencimiento tras el segundo recordatorio (`SinRespuesta`) entraban directamente a la tarea de usuario `Archivar solicitud`. En `cursosNuestraVersion.bpmn`, incorporamos la compuerta exclusiva convergente para unir ambos flujos antes de ingresar a `Archivar solicitud`.
* **Justificación teórica:** Al igual que en el punto 2.1, se elimina el *join implícito* sobre la tarea, aplicando el patrón **Simple Merge (Patrón 5)** y cumpliendo la guía **G2 de las 7PMG** (evitar múltiples flujos de secuencia entrando directamente a una actividad).
* **Problema potencial en Flowable y su solución técnica:**
  * **El problema:** La Sección 3.4 del enunciado exige implementar un *Execution Listener* al finalizar la instancia del proceso que actualice el estado del curso en la base de datos de la institución, distinguiendo explícitamente entre los estados **`rechazado`** y **`cerrado por falta de respuesta`**. Al converger ambos caminos, pasar por la misma tarea `Archivar solicitud` y desembocar en un único evento de fin (`FinSinCurso`), cuando el *token* llega al final del proceso **se pierde la trazabilidad directa de cuál de los dos eventos originó el cierre**.
  * **Cómo se soluciona:** Como el propio enunciado advierte (*"El resultado del proceso deberá establecerse en cada camino de cierre para que el Execution Listener pueda registrar el estado correspondiente"*), la solución limpia en Flowable consiste en configurar un *Execution Listener* sobre cada arista **antes** de que entren a la compuerta:
    * En el flujo `Rechazado`: se asigna la variable de proceso `resultadoCurso = 'RECHAZADO'`.
    * En el flujo `SinRespuesta`: se asigna la variable de proceso `resultadoCurso = 'CERRADO_SIN_RESPUESTA'`.
      De este modo, mantenemos la buena práctica estructural de la compuerta XOR y el evento de fin único para esa rama, mientras el *Execution Listener* de fin de proceso lee la variable `resultadoCurso` sin ninguna ambigüedad.

---

### 2.4. Reemplazo de la tarea de recepción «Recibir nómina» por un Evento Intermedio de Captura de Mensaje

* **Cambio realizado:** Dentro del subproceso `Preparar curso`, en `cursosOriginal.bpmn` la espera de la nómina de participantes estaba modelada mediante una tarea de recepción. En `cursosNuestraVersion.bpmn`, la reemplazamos por un **Evento Intermedio de Captura de Mensaje** (`"Recibir nómina"`).
* **Justificación teórica y práctica:**
  * Cumple directamente con la primera de las buenas prácticas adicionales de BPMN 2.0 vistas en la materia: **«No mezclar eventos de mensaje con tareas de mensaje (envío/recepción) en el mismo modelo; elegir un estilo»**.
  * Dado que en todo el resto del diagrama las recepciones de mensajes desde la empresa (`Solicitud recibida`, `Aceptación`, `Rechazo`, `Modificaciones` y `Recibir Cancelación`) están modeladas mediante **eventos de mensaje**, reemplazar la única `ReceiveTask` por un `IntermediateCatchEvent` unifica el estilo visual y semántico del modelo, facilitando su lectura y manteniendo la misma capacidad de correlación mediante la API REST de Flowable.

---

## 3. Consulta para el profe

Al revisar los escenarios de evaluación detallados en la **Sección 5 (Entrega y demostración, página 7 del PDF)**, encontramos el siguiente punto obligatorio a cubrir durante la defensa:

> **Escenario 7:** *"Falta de aulas disponibles y generación de la excepción correspondiente."*

Sin embargo, al inspeccionar tanto la descripción funcional del problema (Secciones 1.2 y 1.3) como el diagrama original provisto (`cursosOriginal.bpmn`), observamos que la tarea automática **`Reservar aula`** únicamente tiene adherido un evento de borde de compensación, sin contar con un flujo de excepción por error de negocio para el caso en que no existan aulas físicas disponibles.

**Nuestra consulta para el profesor es la siguiente:**
¿Cómo esperas que modelemos y manejemos en el diagrama BPMN la excepción por falta de aulas disponibles en la tarea `Reservar aula`? En particular, ¿deberíamos incorporar un **Evento de Borde de Error** en `Reservar aula` que encadene el disparo de la compensación y la interrupción del subproceso `Preparar curso` (de forma análoga a como actúa el subproceso de evento `Cancelación` lanzando compensación y luego fin de error hacia el borde), o se pretende que el servicio `Reservar aula` lance directamente un `BpmnError` que sea capturado por el evento de borde de error del subproceso `Preparar curso`
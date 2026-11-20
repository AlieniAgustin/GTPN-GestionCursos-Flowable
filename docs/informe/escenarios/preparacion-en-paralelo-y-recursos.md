# Preparación en paralelo y evaluación de recursos

> **Explica:** Cesari · **Cubre:** §1.2 y §4.1.3 (*Evaluar recursos*) del enunciado
> · Detalle técnico: [`03-preparacion-del-curso.md`](../03-preparacion-del-curso.md)
>
> No es uno de los once escenarios numerados: es la parte de la preparación que muestra
> Cesari. Se apoya en la instancia del escenario 10 y conviene mostrarla **después** de
> que Fran muestre la vista de la empresa, porque acá se avanzan esas tareas.

---

## Lo importante a mostrar

1. **Las tres ramas corren a la vez.** Apenas se acepta la propuesta, el subproceso abre
   inscripciones, materiales y recursos al mismo tiempo, con una **compuerta paralela**.
2. **Recursos abre solo lo que se elige.** *Evaluar recursos* deja tres variables y una
   **compuerta inclusiva** abre una rama por cada recurso marcado (una o varias), y el
   cierre espera solo esas.
3. **"Al menos una alternativa"** se valida en la pantalla, antes de completar.

---

## Punto de partida

El curso **"Atencion al Cliente"** (Cooperativa La Union) que deja `preparar_demo.py` está
**recién aceptado**, con las tres ramas abiertas: *Evaluar recursos* y *Preparar
materiales* sin reclamar, y la rama de inscripciones esperando la nómina.

En la Task App de Flowable se puede abrir la instancia y señalar los tres puntos activos
dentro del subproceso *Preparar curso*.

---

## Paso a paso

### 1. Las tres ramas, en la bandeja

En el sitio, entrar con **`administrativo1` / `password`** → **Mis tareas**:

- En **Disponibles para mi grupo** están, a la vez, **Evaluar Recursos** y **Preparar
  materiales** de *Atencion al Cliente*. Son de grupo: hay que reclamarlas.
- La tercera rama no aparece como tarea porque está esperando un **mensaje** de la
  empresa (la nómina).

### 2. Evaluar recursos: la validación

1. **Reclamar** *Evaluar Recursos* → pasa a *Asignadas a mí* → **Completar**.
2. Enviar **sin marcar nada**: vuelve al formulario con *"Elegí al menos un recurso para
   el curso"* y la tarea sigue pendiente.

### 3. Evaluar recursos: abrir dos ramas

Marcar **Aula virtual** y **Equipamiento especial** → **Completar tarea**.

De vuelta en la bandeja, en **Disponibles para mi grupo** aparecen
**Solicitar Aula Virtual** y **Contratar equipamiento**: la compuerta inclusiva abrió
exactamente esas dos. *Reservar aula* no corre, porque no se eligió.

### 4. Cerrar la rama de recursos

Reclamar y completar *Solicitar Aula Virtual* y *Contratar equipamiento*. Recién cuando
terminan **las dos** se cierra la rama de recursos.

### 5. (Opcional) Las otras ramas

- *Preparar materiales*: desde la **app de materiales** (API Java), reclamar y completar.
- Inscripciones: desde la **app de comunicaciones**, mandar la **nómina**. Aparece
  *Confeccionar lista definitiva* **ya asignada** a un administrativo (la asigna un
  listener al que tiene menos tareas). Completarla.

Cuando terminan las tres ramas, el proceso envía la confirmación y aparece **Definir
docente** en la bandeja de `jefedocente` (eso sigue en el escenario 1).

---

## Dónde está implementado (para abrir si piden el código)

| Qué | Archivo | Detalle |
|---|---|---|
| Las 3 ramas | BPMN, subproceso `prepararcurso`: `parallelGateway` `preparacionenparalelo` / `preparacioncompleta` | Abre y espera las tres |
| La pantalla | `backend/src/main/resources/templates/tareas/evaluar-recursos.html` | Tres casillas |
| GET / POST | `backend/.../web/TaskFormSiteController.java` → `/tareas/{id}/evaluar-recursos` | Valida y completa |
| Que sea la tarea correcta | `TaskFormSiteController.esDelTipo()` → `TaskService.findTaskDefinitionKey()` | Una URL armada a mano con otra tarea no sirve |
| Reclamar antes de completar | `backend/.../service/TaskService.java` → `complete()` → `assertClaimedBy()` | Lo controla el backend, no el motor |
| El botón *Completar* lleva al formulario | `backend/.../types/TaskCatalog.java` (`evaluarrecursos` → `evaluar-recursos`) y `templates/tareas/bandeja.html` | |
| La compuerta inclusiva | BPMN: `inclusiveGateway` `recursosnecesarios` / `todoslosrecursosseleccionadoslistos` | Condiciones `${requiereAula}`, `${requierePlataforma}`, `${requiereEquipamiento}` |
| Aula física (automática) | `flowable/.../delegates/ReserveClassroomDelegate.java` → `services/ClassroomService.java` | Web service simulado, 3 aulas (`aulas.disponibles`) |
| Sin aulas | BPMN `boundaryEvent sinaulas` (`errSinAulas`) → `gestionarfaltadeaulas` → vuelve a `reservaraula` | Escenario 7 |
| Asignación de la lista definitiva | `flowable/.../listeners/AssignLeastBusyListener.java` → `services/TaskAssignmentService.java` | Task listener en *create*, §3.4 |

## Cómo lo resolvimos

**Las tres variables, siempre.** La compuerta inclusiva evalúa las tres condiciones. Si
falta una variable, el motor falla con *Unknown property*. Por eso la pantalla manda las
tres aunque sean `false` (una casilla sin marcar no viaja en el formulario, así que se toma
`false` por defecto):

```java
@PostMapping("/tareas/{taskId}/evaluar-recursos")
public String completarRecursos(@PathVariable String taskId,
        @RequestParam(defaultValue = "false") boolean requiereAula,
        @RequestParam(defaultValue = "false") boolean requierePlataforma,
        @RequestParam(defaultValue = "false") boolean requiereEquipamiento, ...) {

    if (!esDelTipo(taskId, usuario, EVALUAR_RECURSOS)) { ... "Esa pantalla no corresponde..." }

    if (!requiereAula && !requierePlataforma && !requiereEquipamiento) {   // §1.2: al menos una
        model.addAttribute("error", "Elegí al menos un recurso para el curso.");
        return "tareas/evaluar-recursos";
    }

    taskService.complete(taskId, Map.of(
            "requiereAula", requiereAula,
            "requierePlataforma", requierePlataforma,
            "requiereEquipamiento", requiereEquipamiento), usuario);
}
```

**Por qué la validación está en la pantalla y no en el modelo.** Si se completara sin
ningún recurso, la compuerta inclusiva no abriría ninguna rama y el cierre esperaría para
siempre. Validar antes de completar evita dejar la instancia trabada.

**Por qué inclusiva.** La cantidad de ramas no se conoce al diseñar: depende de lo que elija
el administrativo. Una paralela abriría siempre las tres; una exclusiva, solo una. La
inclusiva abre las elegidas y su cierre espera exactamente esas, que es el *"deben
completarse todas las acciones seleccionadas"* del enunciado.

**Por qué el aula física es automática y el resto no.** El enunciado pide que la reserva y
liberación del aula física sean automáticas contra un web service simulado; *Solicitar aula
virtual* y *Contratar equipamiento* son tareas de Administración (§2).

---

## Si preguntan

**¿Por qué una compuerta inclusiva y no una paralela?**
Porque no siempre se necesitan los tres recursos. La paralela abriría siempre las tres
ramas; la inclusiva abre solo las elegidas y espera solo esas.

**¿Qué pasa si elijo dos recursos?**
Se abren dos ramas y la rama de recursos termina cuando terminan las dos.

**¿Qué pasa si no elijo ninguno?**
La pantalla no deja completar. Si se completara igual, el proceso quedaría trabado.

**¿Cómo se sabe que las ramas corren en paralelo?**
Las tareas de las tres ramas están abiertas al mismo tiempo (se ven juntas en la bandeja y
en la pantalla de la empresa), y el análisis §4.3.2 las mide a todas desde el mismo
instante: cuando la compuerta paralela las abre.

**¿Quién asigna *Confeccionar lista definitiva*?**
Un task listener en el evento *create*, que se la da al integrante de Administración con
menos tareas asignadas (§3.4). Por eso no hay que reclamarla.

**¿Y si no hay aulas?**
El delegate lanza `BpmnError("errSinAulas")`, lo atrapa un evento de borde y aparece
*Gestionar falta de aula*; al completarla se reintenta la reserva (escenario 7, Damián).

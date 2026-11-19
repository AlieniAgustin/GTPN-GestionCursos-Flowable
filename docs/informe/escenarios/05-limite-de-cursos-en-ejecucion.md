# Escenario 5 — Imposibilidad de continuar por el límite de cursos en ejecución

> **Explica:** Alieni · **Cubre:** §1.1 y §3.1 del enunciado
> · Detalle técnico: [`02-viabilidad-y-acuerdo.md`](../02-viabilidad-y-acuerdo.md)
>
> **Hacer este escenario primero.** Justo en este momento hay cinco cursos en ejecución,
> así que cualquier solicitud nueva sale *no viable* y se ve el cierre en el acto. Si se
> hacen otros escenarios antes, algunos cursos terminan, se libera lugar y esto deja de
> dispararse solo.

---

## Lo importante a mostrar

**Que la institución no decide esto a mano.** La cantidad de cursos en ejecución la
cuenta el motor, la decisión la toma una **tabla de decisión DMN**, y el proceso se cierra
solo sin molestar a nadie: no se crea ninguna tarea para el jefe administrativo.

---

## Paso a paso

### 1. Mandar una solicitud como si fuéramos una empresa

Abrir `http://localhost:8081` — **sin iniciar sesión**, la portada es pública.

Completar el formulario con cualquier dato:

| Campo | Qué poner |
|---|---|
| Razón social | *Demo SA* |
| Email de contacto | *demo@demo.com* |
| Teléfono | *+543580000000* |
| Temática | *Curso que no va a entrar* |
| Descripción | cualquier cosa |
| Cantidad de participantes | *10* |
| Fechas preferidas | una fecha futura, por ejemplo *2027-09-01* |

Apretar **Enviar solicitud**.

Aparece la pantalla *"Solicitud registrada"* con el número de solicitud. Desde el punto de
vista de la empresa, el trámite entró.

### 2. Ver en Flowable que ya terminó

Ir a la **Task App** de Flowable (`http://localhost:8088/flowable-ui/`, con `admin` /
`test`) y buscar la instancia recién creada.

**Ahí está el punto de la demo:** la instancia **ya está terminada**, hace un segundo. No
quedó esperando a nadie. El recorrido que hizo es cortito:

```
Solicitud recibida → Evaluar Viabilidad → ¿Es viable? → Notificar Decisión → FIN
```

Nunca llegó a *Registrar y elaborar propuesta*. Comparado con cualquiera de las otras
instancias que están en el tablero, esta se cerró de inmediato.

### 3. Mostrarlo sobre el diagrama

En el diagrama importado en el Modeler se señala el arranque:

- **Evaluar Viabilidad** es la tarea con el ícono de tabla: es una **decision task**, no
  un service task común. La regla vive en una tabla DMN, no en código Java.
- La compuerta **¿Es viable?** tiene dos salidas: por *sí* sigue a la propuesta, por *no*
  va derecho a *Notificar Decisión* y termina.

---

## Lo que hay por detrás (para señalar en el diagrama)

**Quién cuenta los cursos.** Sobre *Evaluar Viabilidad* hay un listener de inicio,
`countActiveCourses`, que le pregunta al motor cuántas instancias están en ejecución y
deja el número en la variable `cantidadCursosActivos`. El enunciado pide justamente eso:
que la cantidad se obtenga del motor y no se cargue a mano.

```java
runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("Process_Curso")   // todas las versiones desplegadas
        .variableValueEquals("cursoActivo", true)
        .count();
```

La instancia **no se cuenta a sí misma**: la variable `cursoActivo` recién se marca más
adelante, cuando el curso pasa a preparación, así que una solicitud que todavía está
evaluando su viabilidad queda fuera del conteo por construcción.

**Quién decide.** La tabla DMN `evaluarViabilidad`, con dos reglas:

| Cursos activos | Viable |
|---|---|
| `< 5` | `true` |
| `>= 5` | `false` |

**El resultado del cierre.** El flujo que sale de la compuerta hacia *Notificar Decisión*
lleva un listener que deja asentado cómo terminó:

```xml
${execution.setVariable('resultadoCurso','NO_VIABLE')}
```

Es la misma mecánica en los cinco finales posibles del proceso: cada camino de cierre
marca su resultado antes de terminar.

---

## Si preguntan

**¿Por qué el conteo no está adentro del DMN?**
Porque el enunciado pide que la cantidad se obtenga del motor. El DMN solo decide con el
número que le llega; quien mide es el listener. Así la regla queda declarativa y se puede
cambiar el umbral sin tocar una línea de Java.

**¿Cuenta las solicitudes que están negociando?**
No: contamos como "curso en ejecución" solo los que ya fueron aceptados. Una solicitud en
negociación todavía puede rechazarse y no está consumiendo recursos de la institución.
Está asentado como decisión de diseño en [`02-viabilidad-y-acuerdo.md`](../02-viabilidad-y-acuerdo.md).

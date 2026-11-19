# Escenario 7 — Falta de aulas disponibles y generación de la excepción

> **Explica:** Damián · **Cubre:** §1.2 del enunciado
> · Detalle técnico: [`03-preparacion-del-curso.md`](../03-preparacion-del-curso.md)
> · Decisión de diseño: [`adr/aulas-simuladas.md`](../../../adr/aulas-simuladas.md)
>
> **Este escenario tiene su propio setup y conviene mostrarlo aparte**, no sobre las
> instancias que deja `preparar_demo.py`. El motivo está en *Por qué va aparte*, al final
> del setup: hacen falta **dos cursos peleándose por la misma aula**, y el script de la
> demo evita a propósito que eso pase.

---

## Lo importante a mostrar

**Que el error no lo maneja el código: lo maneja el modelo.** El web service de aulas
devuelve "no hay aulas", el delegate lanza un error de BPMN y el proceso **no se cae ni
queda trabado**: un evento de borde de error desvía el token a una tarea humana,
*Gestionar falta de aula*, y al completarla el flujo **vuelve a intentar** la reserva.

El segundo punto, más fino: la rama de recursos queda esperando, pero **las otras dos
ramas de la preparación siguen corriendo**. El error de una rama no frena a las demás.

---

## Antes de empezar: qué archivo hay que cambiar

El web service de aulas está simulado y tiene **tres aulas** por defecto. Para que la
falta de aulas se dispare con dos cursos, hay que bajarlo a una.

### Opción recomendada: `docker-compose.yml`

Descomentar la última línea del bloque `environment` del servicio `flowable-app`:

```yaml
      # §4 Aulas del web service simulado. Bajalo a 1 para mostrar el escenario 7
      # (falta de aulas) con dos cursos pidiendo aula a la vez
      AULAS_DISPONIBLES: 1
```

Y recrear **solo ese contenedor**. No hace falta `--build` ni recompilar: cambia una
variable de entorno, no el jar.

```bash
docker compose up -d
```

`AULAS_DISPONIBLES` es la variable de entorno que corresponde a la propiedad
`aulas.disponibles`, así que Spring la toma sola.

### Alternativa: `flowable/src/main/resources/application.properties`

Si en vez de Docker se corre el motor con Maven, se cambia la propiedad directamente:

```properties
aulas.disponibles=1
```

Esto **sí** obliga a recompilar (`cd flowable && ./build.sh`) si después se levanta por
Docker, porque la imagen solo copia el jar ya hecho.

### El orden importa

> El pool de aulas vive **en memoria** del motor: al reiniciar `flowable-app` **todas las
> aulas quedan libres**, aunque haya cursos que las tenían reservadas. Así que primero se
> cambia la variable y se recrea el contenedor, y **después** se crean las instancias. Si
> se reinicia en medio de la prueba, *Gestionar falta de aula* se completa sin problema y
> parece que se arregló solo.

### Cargar los dos cursos

```bash
./scripts/seed-demo.sh 5
```

Deja cinco solicitudes y **dos de ellas aceptadas y en preparación**, con las tres ramas
abiertas y *Evaluar Recursos* sin reclamar:

| Curso | Empresa | Rol en el escenario |
|---|---|---|
| *Introduccion a BPMN* | ACME SA | se queda con la única aula |
| *Primeros Auxilios* | Cooperativa La Union | se encuentra sin aulas |

Las otras tres quedan rechazada, con modificaciones y recién ingresada: no molestan.

### Por qué va aparte

Dos razones, y las dos son buenas para contar si preguntan:

- `preparar_demo.py` deja **cinco cursos en ejecución**, que es el límite del enunciado,
  así que no se puede dar de alta un curso nuevo: saldría *no viable*.
- Ese script pide **aula virtual** en todos los escenarios menos el de cancelación,
  justamente para que los cursos no se agoten las aulas entre ellos y ninguna preparación
  quede trabada en *Gestionar falta de aula*. Es decir: el resto de la demo está armada
  para que este escenario **no** se dispare.

Si igual se quiere mostrar sobre la demo preparada, el camino es hacerlo **después del
escenario 10** y sobre ese mismo curso (*Atencion al Cliente*, el que está en preparación),
con `AULAS_DISPONIBLES: 0`: ahí no hay ninguna aula libre y la excepción salta con un solo
curso. La contra es que deja al escenario 6 sin aula física que compensar.

---

## Paso a paso

Todo desde el sitio: `http://localhost:8081`, con **`administrativo1` / `password`**
(Pablo Ferreyra, que es quien tiene a cargo los recursos).

### 1. El primer curso se queda con la única aula

En **Tareas**, en la lista de *disponibles para mi grupo*, buscar *Evaluar Recursos* del
curso **Introduccion a BPMN**.

1. **Reclamar** la tarea. Recién ahí aparece el botón de completar: es una tarea de grupo,
   y el enunciado pide que se reclame antes de completarla.
2. **Completar** → se abre la pantalla de recursos.
3. Marcar **solo "Aula física"** y enviar.

No pasa nada visible, y eso es lo correcto: la reserva salió bien. El curso siguió a la
siguiente rama con `AULA-1` reservada.

### 2. El segundo curso se encuentra sin aulas

Mismo recorrido con *Evaluar Recursos* del curso **Primeros Auxilios**: reclamar,
completar, marcar **solo "Aula física"**, enviar.

**Acá está el escenario.** Al volver a la bandeja aparece una tarea nueva que nadie pidió:

> **Gestionar falta de aula** — *Primeros Auxilios*

El motor intentó reservar, el web service no tenía aulas y el proceso derivó el problema a
una persona en vez de fallar.

### 3. Mostrar que las otras ramas no se frenaron

En el mismo curso *Primeros Auxilios*, en la bandeja siguen estando **Preparar
materiales** y la instancia sigue esperando la **nómina de participantes**. La rama de
recursos está detenida esperando que alguien consiga un aula, y las otras dos avanzan por
su cuenta.

Desde la pantalla de la empresa se ve igual de bien: entrando a **Mis cursos** con la
cuenta de *Cooperativa La Union*, el curso muestra las actividades en ejecución y entre
ellas está *Gestionar falta de aula*.

### 4. En Flowable, sobre el diagrama

En el **Modeler** (`http://localhost:8088/flowable-ui/`, `admin` / `test`), sobre
*Reservar aula*:

- Tiene **dos eventos de borde**: uno es el de **error** (`sinaulas`, el del rayo), que es
  el que acaba de dispararse; el otro es el de **compensación**, que sirve para liberar el
  aula más adelante.
- Del evento de error sale la flecha a *Gestionar falta de aula*, y de esa tarea
  **vuelve una flecha a *Reservar aula***. Ese es el reintento, y está en el modelo: no es
  un `catch` en Java.

En la **Task App**, la instancia de *Primeros Auxilios* está parada ahí.

---

## Cerrar el círculo: liberar el aula y reintentar

La tarea no se puede completar con éxito mientras no haya un aula libre: si se completa,
reintenta la reserva, vuelve a fallar y **vuelve a aparecer**. Eso también vale mostrarlo
una vez, porque demuestra que el reintento es real.

Para que el reintento funcione hay que liberar `AULA-1`, y **el aula se libera por
compensación**, no cuando termina el dictado:

**Camino corto — cancelar el primer curso.** Desde la **app de comunicaciones**
(`java -jar comunicaciones/target/app.jar`), elegir *Introduccion a BPMN* y enviar
**Cancelar el curso**. La compensación corre sola y libera el aula.

**Camino completo — terminar el primer curso.** Llevarlo hasta *Enviar informe a empresa y
archivar expediente*: la compensación se lanza ahí, al cerrar el proceso. Es más largo
—hay que completar las tres ramas, definir docente, disparar el temporizador de la fecha
de inicio, dictar el curso y elaborar el informe— pero es el final normal.

En los dos casos se ve en el log del motor:

```bash
docker compose logs flowable-app | grep -iE "aula"
```

Tiene que aparecer `Aula reservada: AULA-1`, después `no hay aulas disponibles` y al final
la liberación.

**Con el aula libre, completar *Gestionar falta de aula*** del curso *Primeros Auxilios*:
el flujo reintenta, esta vez reserva `AULA-1` y la rama de recursos sigue su camino. La
tarea no vuelve a aparecer.

---

## Lo que hay por detrás (para señalar en el diagrama)

**El web service simulado.** [`ClassroomService`](../../../flowable/src/main/java/com/flowable/app/services/ClassroomService.java)
mantiene un mapa de aulas en memoria: cada una libre u ocupada por una instancia. 

**Quién lanza el error.** [`ReserveClassroomDelegate`](../../../flowable/src/main/java/com/flowable/app/delegates/ReserveClassroomDelegate.java),
el delegate del service task *Reservar aula*:

```java
Optional<String> classroom = classroomService.reserve(execution.getProcessInstanceId());

if (classroom.isEmpty()) {
    throw new BpmnError(NO_CLASSROOM_ERROR, "No hay aulas disponibles");   // errSinAulas
}
execution.setVariable(CLASSROOM_VARIABLE, classroom.get());                // aulaAsignada
```

Es un `BpmnError`, no una excepción Java común: eso es lo que hace que el motor lo trate
como un error **del proceso** y busque un manejador en el modelo.

**Quién lo atrapa.** El evento de borde de error sobre la tarea, y el flujo de vuelta:

```xml
<boundaryEvent id="sinaulas" attachedToRef="reservaraula">
  <errorEventDefinition errorRef="errSinAulas" />
</boundaryEvent>

<sequenceFlow sourceRef="sinaulas"              targetRef="gestionarfaltadeaulas" />
<sequenceFlow sourceRef="gestionarfaltadeaulas" targetRef="reservaraula" />
```

**Cómo se nota en las variables.** El curso que reservó tiene `aulaAsignada = AULA-1`; el
que falló **no tiene la variable**, porque el delegate la escribe solo cuando la reserva
sale bien.

```bash
curl -s -u admin:test "http://localhost:8080/process-api/runtime/process-instances?processDefinitionKey=Process_Curso&includeProcessVariables=true"
```

Y que el evento de borde se disparó queda en el historial:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-activity-instances?activityId=sinaulas"
```

**El test que lo cubre.** `ClassroomUnavailableTest` baja el pool a cero por configuración,
llega a la reserva y verifica que la instancia quede en *Gestionar falta de aula*.

---

## Si preguntan

**¿Por qué el aula se pide a un web service simulado y no es una tabla del sistema?**
Porque el enunciado pide simular una conexión con un web service externo: las aulas de la
institución no son parte del proceso de cursos. Está asentado en
[`adr/aulas-simuladas.md`](../../../adr/aulas-simuladas.md).

**¿Por qué un error de BPMN y no una excepción Java?**
Una excepción común haría fallar la transacción y el job se reintentaría solo, sin que
nadie se enterara. El `BpmnError` convierte el problema en algo que el **modelo** decide
cómo manejar, y acá la decisión del negocio es clara: que lo resuelva un administrativo.

**¿Y si completan *Gestionar falta de aula* y sigue sin haber aulas?**
Vuelve a intentar la reserva, vuelve a fallar y la tarea aparece de nuevo. El ciclo es
intencional: el proceso no avanza hasta que haya un aula, pero tampoco se pierde ni queda
en un estado inválido.

**¿Se frena toda la preparación?**
No. Solo la rama de recursos. Las otras dos —materiales e inscripciones— siguen corriendo,
y la compuerta de cierre espera a las tres. Es la ventaja de haberlas modelado en paralelo.

**¿Cuándo se libera el aula?**
Por compensación: al cancelar el curso, o al cerrarlo normalmente después de enviar el
informe. No se libera al terminar el dictado, y tampoco tiene vencimiento por fecha.

**¿Por qué reiniciar el motor libera las aulas?**
Porque el pool es en memoria, y eso es parte de la simulación: no persistimos el estado de
un sistema que es de otro. Si la institución tuviera un web service real, el estado viviría
allá.

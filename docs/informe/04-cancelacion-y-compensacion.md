# 04 — Cancelación durante la preparación y compensación

> **Explica:** Alieni · **Cubre:** §1.3 (cancelación durante la preparación)
>
> Diseño detallado: [`docs/cancelacion-durante-preparacion.md`](../cancelacion-durante-preparacion.md) · ADRs: [`cancelacion-no-interruptiva.md`](../../adr/cancelacion-no-interruptiva.md), [`compensacion-bpmn.md`](../../adr/compensacion-bpmn.md)

---

## 1. Qué pide el enunciado

Mientras se ejecuta el subproceso *Preparar curso*, la empresa puede solicitar la cancelación. Al recibirla:

1. Se **interrumpe** la preparación.
2. Se **compensan** las acciones de recursos que hayan **finalizado correctamente**: liberar el aula física, liberar el aula virtual y liberar el equipamiento, **según corresponda**.
3. Solo deben ejecutarse las compensaciones de los recursos **efectivamente gestionados**.
4. Una vez concluidas, se notifica el cierre a la empresa y finaliza el proceso.

## 2. Cómo se resolvió

### La interrupción

Un **subproceso de evento** (`cancelacion`, `triggeredByEvent="true"`) dentro de `prepararcurso`, con un start event de mensaje **interruptor** (`isInterrupting="true"`) suscripto a `Cancelación`.

Al llegar el mensaje, el motor cancela todo lo que esté abierto en la preparación —las tres ramas, con sus tareas— y le pasa el control al subproceso de evento. No hace falta cancelar nada a mano.

```text
[Preparar curso]
   ├─ (las tres ramas corriendo)
   └─ ⬡ Cancelación (subproceso de evento, interruptor)
         └─ Recibir Cancelación (mensaje)
               ├─► Compensar equipamiento   ⟲
               ├─► Compensar aula            ⟲
               ├─► Compensar aula virtual    ⟲
               ├─► Registrar Cancelación (cancelado = true)
               └─► Confirmar liberación de recursos (userTask, grupo administracion)
                        │
                        ▼
                     fin del subproceso
                        │
      ◇ ¿Cancelado? ── sí ─► Notificar Cierre ─► FIN (resultadoCurso = CANCELADO)
                     └─ no ─► Enviar confirmación ─► (sigue el curso)
```

### Por qué solo se compensa lo gestionado

Esto no se programó: es cómo funciona la compensación en BPMN y es la razón de haberla usada.

- Cada actividad de recursos tiene un **evento de borde de compensación** con `cancelActivity="false"`, y una actividad `isForCompensation="true"` asociada:

  | Actividad original | Compensación | Tipo |
  |---|---|---|
  | Reservar aula | Liberar Aula | `serviceTask` con `ReleaseClassroomDelegate` |
  | Solicitar Aula Virtual | Liberar Aula Virtual | `scriptTask` |
  | Contratar equipamiento | Liberar Equipamiento | userTask del grupo `administracion` |

- El motor registra un *compensation handler* **solo cuando la actividad original terminó bien**. Los tres eventos de lanzamiento de compensación del subproceso de cancelación piden compensar las tres actividades, pero el motor ejecuta únicamente las que tienen handler registrado.

Resultado: si el curso solo había reservado aula física, se libera el aula y nada más. Si no se había gestionado ningún recurso todavía, no se ejecuta ninguna compensación. Es exactamente el punto 3 del enunciado, sin un solo `if`.

### El cierre

*Registrar Cancelación* deja `cancelado = true`. Cuando el subproceso termina, el flujo de salida pasa por la compuerta **¿Cancelado?**, que distingue los dos motivos por los que un subproceso puede haber terminado:

- `cancelado == true` → *Notificar Cierre* → fin, con `resultadoCurso = CANCELADO`.
- si no → *Enviar confirmación* y el curso sigue a la etapa de dictado.

Esa compuerta es una de las modificaciones al diagrama de cátedra: sin ella, una preparación interrumpida y una completada salen por el mismo flujo y el proceso le enviaría la confirmación del curso a una empresa que acaba de cancelar.

### El listener que cierra la escucha

Hay un detalle del motor que vale contar porque fue un problema real: un subproceso que tiene a la vez eventos de borde y un subproceso de evento **no termina solo**. La ejecución que espera el mensaje de cancelación queda viva y la preparación nunca cierra, aunque las tres ramas hayan terminado.

Lo resuelve [`CloseCancellationListener`](../../flowable/src/main/java/com/flowable/app/listeners/CloseCancellationListener.java), un `ExecutionListener` en el evento de fin del subproceso: busca entre los hijos del scope la ejecución que está esperando el mensaje y la elimina. Así la preparación cierra y el proceso sigue a *Enviar confirmación*.

### La misma compensación, reutilizada al final

Al finalizar **normalmente**, el proceso lanza un evento de compensación sobre todo el subproceso `prepararcurso` (`liberarrecurso`, con `activityRef="prepararcurso"`). Libera los recursos con las mismas actividades de compensación, sin duplicar lógica. Ver [05](05-desarrollo-y-finalizacion.md).

## 3. Cómo se demuestra

Es el **escenario 6**. Lo que hay que dejar claro en la defensa son dos cosas: que la cancelación **interrumpe** la preparación, y que se compensa **solo lo que se había gestionado**.

> La preparación de la máquina (compilar, levantar Docker, correr `preparar_demo.py`) está en [02 §3.0](02-viabilidad-y-acuerdo.md#30-preparar-la-máquina-una-sola-vez-antes-de-la-defensa). El truco para ver el diagrama con la instancia resaltada, en [02 §3.1](02-viabilidad-y-acuerdo.md#31-el-truco-para-mostrarlo-gráficamente).

### El código que hay que mostrar

| Qué | Dónde |
|---|---|
| El subproceso de evento | BPMN **línea 183**, `<subProcess id="cancelacion" triggeredByEvent="true">` |
| El mensaje que lo dispara, **interruptor** | BPMN **línea 184**, `<startEvent id="recibircancelacion" isInterrupting="true">` |
| Los tres lanzamientos de compensación | BPMN **líneas 188, 191 y 194**, `<compensateEventDefinition activityRef="...">` |
| Los bordes que registran el handler | BPMN **líneas 156, 176 y 179**, `cancelActivity="false"` |
| Las tres actividades de compensación | BPMN **líneas 159** (equipamiento), **165** (aula) y **166** (aula virtual), todas con `isForCompensation="true"` |
| El delegate que libera el aula | [`ReleaseClassroomDelegate.java`](../../flowable/src/main/java/com/flowable/app/delegates/ReleaseClassroomDelegate.java) |
| Marcar que fue cancelación | BPMN **línea 197**, `setVariable("cancelado", true)` |
| La compuerta ¿Cancelado? | BPMN **línea 401**, `${cancelado == true}` |
| El listener que cierra la escucha | [`CloseCancellationListener.java`](../../flowable/src/main/java/com/flowable/app/listeners/CloseCancellationListener.java) |

Para abrir el BPMN en el editor y saltar a una línea: `Ctrl+G` en VS Code y escribir el número.

### Paso a paso

De la lista que imprimió el script, buscá el escenario **"6. Cancelación con compensación"** y copiá su id. Esa instancia ya está cancelada y esperando que se confirmen las liberaciones, así que para mostrar la secuencia completa conviene armar una nueva en vivo.

**Opción A — mostrar la que ya está preparada (más rápido):**

1. Mostrá el diagrama con el comando de [02 §3.1](02-viabilidad-y-acuerdo.md#31-el-truco-para-mostrarlo-gráficamente): el círculo rojo está en el recuadro **Cancelación**, sobre *Confirmar liberación de recursos*. Las tareas de la preparación **ya no están**: se interrumpieron.
2. Mostrá qué se compensó:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-variable-instances?processInstanceId=PEGAR_EL_ID&size=50" | python3 -m json.tool | grep -A2 -E "aulaLiberada|equipoLiberado|aulaVirtualLiberada|cancelado"
```

3. Entrá al sitio como `administrativo1` / `password` → **Mis tareas**. Está *Confirmar liberación de recursos* en "Disponibles para mi grupo". Apretá **Reclamar** y después **Completar**.
4. La instancia cierra. Mostralo:

```bash
docker exec flowable-postgres psql -U flowable -d flowable -c "select id, name, status from courses where status is not null order by id desc limit 3;"
```

Tiene que decir `CANCELADO`.

**Opción B — hacerlo en vivo de punta a punta (más vistoso):**

1. Buscá en la lista el escenario **"10. Acceso de la empresa y preparación en paralelo"**: esa instancia está en preparación con las tres ramas abiertas.
2. Entrá al sitio como `administrativo1` → **Mis tareas** → *Evaluar Recursos* → **Reclamar** → **Completar**.
3. En la pantalla de recursos, **tildá "Aula física" y "Equipamiento especial"** y apretá **Confirmar recursos**. Así hay dos recursos gestionados y la compensación tendrá algo que liberar.
4. Mostrá que el aula se reservó sola (es un service task, no la hizo nadie):

```bash
docker compose logs --tail 20 flowable-app | grep -i aula
```

5. Volvé a **Mis tareas** y completá *Contratar equipamiento* (reclamar + completar). Ahora hay **aula física y equipamiento** gestionados.
6. Abrí la app de la empresa:

```bash
java -jar comunicaciones/target/app.jar
```

7. En la ventana, **seleccioná el curso "Atención al Cliente"**. Abajo aparece **Cancelación** entre las comunicaciones habilitadas (solo aparece porque la instancia está dentro de la preparación). Apretá **Enviar**.
8. **Acá está el momento de la demo:** volvé al sitio, recargá **Mis tareas**. Las tareas de la preparación **desaparecieron** — el mensaje interruptor las canceló todas de una. En su lugar aparecen *Liberar Equipamiento* y después *Confirmar liberación de recursos*.
9. Mostrá el diagrama otra vez: el token está adentro del recuadro **Cancelación**.
10. Completá *Liberar Equipamiento* y *Confirmar liberación de recursos* (reclamar + completar cada una).
11. Verificá que se compensó **solo lo gestionado**:

```bash
curl -s -u admin:test "http://localhost:8080/process-api/history/historic-variable-instances?processInstanceId=PEGAR_EL_ID&size=50" | python3 -m json.tool | grep -B1 -A2 "Liberada\|Liberado"
```

Van a estar `aulaLiberada: true` y `equipoLiberado: true`, y **`aulaVirtualLiberada` no va a existir**, porque nunca se solicitó aula virtual. Ese es el punto 3 del enunciado, y no hay ningún `if` en el código que lo decida: lo hace el motor con los handlers de compensación.

### Correr el test en vivo

`CompensationTest` cubre los tres casos (solo aula, solo equipamiento, los dos):

```bash
cd flowable && mvn test -DforkCount=0 -Dtest=CompensationTest && cd ..
```

Son tres tests y los tres tienen que dar verde. Si te preguntan cómo verifican que **no** se compense lo que no se gestionó, el test lo comprueba chequeando que la variable de liberación del recurso no usado no exista.

### Si algo sale mal en vivo

| Síntoma | Qué hacer |
|---|---|
| La app de comunicaciones no ofrece *Cancelación* | La instancia no está en preparación. Elegí una que sí lo esté, o fijate el diagrama |
| Tras cancelar no aparece ninguna tarea | Puede que no se hubiera gestionado ningún recurso: entonces no hay nada que compensar y va directo a *Confirmar liberación*. Es correcto, y vale la pena decirlo |
| *Liberar Equipamiento* no aparece | Solo sale si se completó *Contratar equipamiento* antes. Es exactamente lo que el escenario quiere mostrar |

## 4. Preguntas probables

**¿Por qué un subproceso de evento y no un evento de borde interruptor sobre la preparación?**
Un evento de borde saca el token **fuera** del subproceso, y ahí ya se pierde el contexto para compensar sus actividades internas y para que un administrativo confirme la liberación. El subproceso de evento mantiene el alcance de `prepararcurso`, que es justo lo que la compensación necesita. Está argumentado en [`adr/cancelacion-no-interruptiva.md`](../../adr/cancelacion-no-interruptiva.md).

**¿Cómo sabe el motor qué recursos se gestionaron?**
Por los handlers de compensación: se registran al completarse cada actividad original. No hay variables de control involucradas en esa decisión; las variables `aulaLiberada` y compañía son solo para poder verificarlo desde los tests y la demo.

**¿Qué pasa si la cancelación llega cuando la preparación ya terminó?**
No llega: `CloseCancellationListener` da de baja la suscripción al mensaje cuando el subproceso cierra. La app de comunicaciones, además, deja de ofrecer *Cancelar* porque le pregunta al motor qué mensajes están habilitados y ya no hay ninguna ejecución suscripta.

**¿Y si la empresa cancela antes de aceptar la propuesta?**
No corresponde: el enunciado limita la cancelación al subproceso de preparación. Antes de aceptar, la empresa tiene el rechazo. El mensaje `Cancelación` solo está suscripto dentro de `prepararcurso`.

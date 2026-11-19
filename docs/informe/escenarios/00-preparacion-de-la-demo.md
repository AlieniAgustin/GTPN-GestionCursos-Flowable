# 00 — Preparación de la demo

> Guion de la defensa para los escenarios de **Alieni**: 1, 3, 4, 5 y 6.
>
> La idea es **no tocar la terminal durante la presentación**. Todo lo de abajo se corre
> una sola vez antes de empezar; después se muestra todo desde el navegador, desde
> Flowable y desde la app de escritorio.

---

## 1. Lo único que se ejecuta por consola (antes de empezar)

Abrí una terminal y pegá esto en orden.

**Entrar a la carpeta del proyecto:**

```bash
cd ~/Desktop/"Computer's Science"/"4rd year"/"Gestion y Tecnologias de Procesos de Negocio"/GTPN-GestionCursos-Flowable
```

**Compilar los dos jars** (las imágenes de Docker no compilan: solo copian el jar ya hecho):

```bash
cd flowable && ./build.sh && cd ../backend && ./build.sh && cd ..
```

**Levantar los cuatro servicios:**

```bash
docker compose up -d --build
```

Esperá unos 40 segundos y comprobá que estén todos arriba:

```bash
docker compose ps
```

Tienen que aparecer `flowable-app`, `flowable-backend`, `flowable-postgres` y `flowable-ui`.

**Dejar los escenarios preparados:**

```bash
.venv/bin/python scripts/preparar_demo.py --limpiar
```

Y listo: a partir de acá, **terminal cerrada**.

---

## 2. Para qué sirve `preparar_demo.py`

Este es el punto importante del guion.

El proceso de cursos es largo: una solicitud tiene que pasar por la propuesta, la
respuesta de la empresa, la preparación en paralelo, el dictado y el cierre. Recorrerlo
entero en vivo, para cada escenario, no entra en el tiempo de una defensa.

Lo que hace el script es **dejar una instancia del proceso parada en cada punto**, como
si fueran once solicitudes reales que vienen avanzando desde hace días. Entonces cada
escenario se muestra **entrando a la pantalla donde ya está esperando** y completando lo
que falta, en vez de empezar de cero.

Cuando termina imprime una lista diciendo, para cada escenario, qué curso quedó dónde y
con qué usuario verlo. **Conviene sacarle una foto o dejar esa terminal abierta**, aunque
los documentos de cada escenario ya dicen qué curso buscar.

El `--limpiar` cierra lo que haya quedado de una corrida anterior. Hace falta porque solo
cinco cursos pueden estar en ejecución a la vez (es la regla de viabilidad del enunciado):
si quedan cinco ocupando lugar, las solicitudes nuevas salen *no viables* y los escenarios
no se arman.

---

## 3. Las herramientas de la demo

### 3.1 El sitio web de la institución — `http://localhost:8081`

Es **la aplicación propia del proyecto** y donde se hace casi todo:

- **La portada es pública:** desde ahí una empresa manda una solicitud de capacitación,
  sin tener cuenta.
- **Con usuario y contraseña** entra el personal de la institución a su bandeja de
  tareas: reclamar, completar, cargar la propuesta, elegir el docente, escribir el
  informe.

**Las tareas se completan desde acá, no desde Flowable.** Flowable se usa para *ver* el
proceso; el trabajo humano pasa por el sitio, que es lo que pide el enunciado.

Usuarios (la contraseña de todos es `password`):

| Usuario | Quién es |
|---|---|
| `jefeadministrativo` | Laura Giménez — elabora y revisa propuestas, archiva, envía el informe |
| `administrativo1` | Pablo Ferreyra — materiales, recursos, listas, liberaciones |
| `jefedocente` | Marcela Ibáñez — define el docente y elabora el informe |
| `docente1` | Hernán Rossi — dicta el curso si lo eligen |
| `docente2` | Sofía Quiroga — ídem |

### 3.2 La app de comunicaciones (escritorio) — hace de empresa

Es la aplicación Java que simula a la empresa del otro lado. Se abre con:

```bash
java -jar comunicaciones/target/app.jar
```

Se abre una ventana con la lista de solicitudes. Al elegir una, abajo aparecen **solo las
comunicaciones que el motor tiene habilitadas en ese momento** — aceptar, rechazar, pedir
modificaciones, mandar la nómina de participantes o cancelar. Si una no corresponde en ese
estado, no aparece.

Conviene dejarla abierta durante toda la demo.

> Hay una segunda app, la de **preparación de materiales**, que usa la API Java de
> Flowable en vez de la REST (`java -jar materiales/target/app.jar`). No hace falta para
> los escenarios 1, 3, 4, 5 y 6.

### 3.3 Flowable — para ver el proceso gráficamente

`http://localhost:8088/flowable-ui/` — se entra con **`admin` / `test`**.

**Importar el modelo para poder verlo.** Una vez, al principio:

1. Entrar al **Modeler App**.
2. Botón **Import process model**.
3. Elegir el archivo:
   `flowable/src/main/resources/processes/Institución_de_capacitación.bpmn20.xml`
4. Guardar.

A partir de ahí el diagrama se puede abrir en pantalla grande y señalar sobre él mientras
se explica: la compuerta de viabilidad, la de espera de respuesta, el subproceso de
preparación, el de cancelación.

**Ver las instancias en curso.** En la **Task App**, en la sección de procesos, están
corriendo las instancias que dejó `preparar_demo.py`. Se puede abrir cada una y ver en qué
punto del diagrama está parada. Como hay varias a la vez, se ve bien que el motor maneja
instancias independientes del mismo proceso.

---

## 4. El orden sugerido de los escenarios

Conviene arrancar por el **5**, por un motivo concreto: en ese momento hay exactamente
cinco cursos en ejecución, así que cualquier solicitud nueva sale *no viable* y se ve el
cierre inmediato. Si se hacen otros escenarios antes, algunos cursos terminan, se libera
lugar y el 5 deja de dispararse solo.

| Orden | Escenario | Documento |
|---|---|---|
| 1° | 5 — Límite de cursos en ejecución | [05-limite-de-cursos-en-ejecucion.md](05-limite-de-cursos-en-ejecucion.md) |
| 2° | 3 — Rechazo de la propuesta | [03-rechazo-de-la-propuesta.md](03-rechazo-de-la-propuesta.md) |
| 3° | 4 — Cierre por falta de respuesta | [04-cierre-por-falta-de-respuesta.md](04-cierre-por-falta-de-respuesta.md) |
| 4° | 6 — Cancelación con compensación | [06-cancelacion-con-compensacion.md](06-cancelacion-con-compensacion.md) |
| 5° | 1 — Aceptación y finalización normal | [01-aceptacion-y-finalizacion-normal-con-liberacion-de-recursos.md](01-aceptacion-y-finalizacion-normal-con-liberacion-de-recursos.md) |

El 1 va al final porque es el más largo: recorre la preparación, el dictado y el cierre.

---

## 5. Al terminar

Para bajar todo y dejar la máquina limpia:

```bash
docker compose down -v
```

El `-v` borra también la base de datos, así que la próxima vez arranca de cero y hay que
volver a correr `preparar_demo.py`. Si se quiere conservar lo hecho, basta con
`docker compose down` sin el `-v`.

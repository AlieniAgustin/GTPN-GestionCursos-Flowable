# App de preparación de materiales (§4.2.1) + análisis de ejecuciones (§4.3)

App de escritorio, local a la intranet, con alcance a la base del motor. Usa la **API Java de Flowable**, no la REST: levanta un `ProcessEngine` embebido contra el mismo PostgreSQL que la app del motor y trabaja con `IdentityService` y `TaskService`. No pasa por el BFF.

Incluye además las tres consultas del análisis de ejecuciones (§4.3), que el enunciado permite incorporar a esta app.

> Informe de la defensa: [`docs/informe/10-apps-auxiliares.md`](../docs/informe/10-apps-auxiliares.md) y [`11-analisis-y-mineria.md`](../docs/informe/11-analisis-y-mineria.md)

## Cómo correrla

Necesita el stack levantado (ver el [README principal](../README.md#1-levantar)) y entorno gráfico.

```bash
./build.sh
```

```bash
java -jar target/app.jar
```

`./build.sh` hace `mvn clean package -DskipTests` y deja `target/app.jar`. Hay que repetirlo después de cualquier `mvn clean`: `mvn test` compila pero no empaqueta, y sin el jar sale `Unable to access jarfile`.

Entrá con un usuario del grupo Administración, por ejemplo `administrativo1` / `password`.

## Qué hace

- **Identificarse**: usuario y contraseña de Flowable (§3.3). Además exige pertenecer al grupo Administración, porque *Preparar materiales* es de ese grupo (§2).
- **Consultar la bandeja**: las tareas *Preparar materiales* asignadas al usuario más las reclamables por su grupo, con el curso y la empresa de cada instancia.
- **Reclamar** una disponible. Si otro integrante la reclamó primero, el motor rechaza el reclamo y la app lo muestra.
- **Completar**, con un detalle opcional de los materiales que queda en la variable `detalleMateriales` (junto con `materialesPreparados = true`).

La app valida lo que el motor no verifica: una tarea sin reclamar no se puede completar, y una asignada solo la completa su responsable.

## Análisis de las ejecuciones (§4.3)

Las tres consultas sobre el historial del motor:

1. **Resultados de las solicitudes**: cantidad y porcentaje de cada resultado sobre el total de instancias terminadas.
2. **Preparación en paralelo**: duración promedio de las ramas de inscripciones, materiales y recursos. Excluye las preparaciones interrumpidas por cancelación.
3. **Distribución de tareas**: tareas de usuario completadas por cada administrativo y docente.

Dos formas de verlas: el botón **Análisis de ejecuciones** de la ventana, o por consola, que es la que conviene para la captura de la entrega:

```bash
java -cp target/app.jar com.materiales.app.analysis.AnalysisApp
```

El [README principal](../README.md#análisis-de-las-ejecuciones-43) tiene un ejemplo de la salida y el detalle de cómo se mide cada cosa.

> Recién levantado el stack el informe sale casi vacío: `seed-demo.sh` deja las instancias en estados intermedios y ninguna termina. Para que las dos primeras tablas tengan contenido hay que cerrar instancias, y la vía rápida es la [app de comunicaciones](../comunicaciones/README.md).

## Configuración

La conexión se cambia sin recompilar. Precedencia: propiedad de sistema, variable de entorno y por último [`materiales.properties`](src/main/resources/materiales.properties).

| Propiedad     | Variable de entorno | Default                                      |
| ------------- | ------------------- | -------------------------------------------- |
| `db.url`      | `DB_URL`            | `jdbc:postgresql://localhost:5432/flowable`  |
| `db.driver`   | `DB_DRIVER`         | `org.postgresql.Driver`                      |
| `db.username` | `DB_USERNAME`       | `flowable`                                   |
| `db.password` | `DB_PASSWORD`       | `flowable`                                   |

```bash
java -Ddb.url=jdbc:postgresql://otra-maquina:5432/flowable -jar target/app.jar
```

## Decisiones de implementación

- **Sin Spring.** Es una app de escritorio de diez clases con una dependencia; un contenedor de inyección no resolvería nada y sumaría peso. Usar el *starter* de Spring Boot de Flowable, además, la habría convertido en una segunda aplicación Flowable completa sobre la misma base, con autodespliegue de procesos y su propio async executor.
- **No crea ni migra tablas** (`databaseSchemaUpdate = false`): el dueño del esquema es la app del motor. Si las versiones no coinciden, falla al arrancar en vez de tocar la base.
- **Async executor apagado**: no compite por los jobs ni por los temporizadores del motor.
- **Event registry deshabilitado**: no lo usa, y arrancarlo dispara un chequeo de Liquibase sobre sus tablas.
- La bandeja son dos consultas (`taskAssignee` y `taskCandidateGroupIn` con los grupos que resolvió el login) en vez de `taskCandidateOrAssigned`, así los grupos candidatos son explícitos y no dependen de la resolución implícita del motor.

Compartir la base entre varios motores no es una rareza del proyecto: el contenedor `flowable-ui` hace lo mismo.

## Tests

```bash
mvn test
```

**26 tests, 0 fallos.** Levantan un motor embebido sobre H2, así que no necesitan PostgreSQL ni pantalla. Cubren el login, la bandeja, el reclamo, el completado con sus validaciones, la configuración de la conexión y las tres consultas del análisis.

Los del análisis corren contra [`analisis-test.bpmn20.xml`](src/test/resources/analisis-test.bpmn20.xml), un esqueleto que copia del modelo real los ids de las actividades, el subproceso de preparación con sus tres ramas y el de evento de cancelación, y **mueven el reloj del motor a mano**, así las duraciones salen exactas y no dependen de lo que tarde la máquina.

La interfaz Swing no tiene tests automáticos: se prueba a mano.

# Checklist --- Proyecto Gestión y Tecnologías de Procesos de Negocio

> Checklist de seguimiento del proyecto. Marcá cada ítem a medida que se
> complete.
>
> Marcado según lo implementado actualmente en el repositorio; los ítems
> sin marcar siguen pendientes.

## 1. Proceso BPMN

### Viabilidad y acuerdo

-   [x] Registrar la solicitud de capacitación.
-   [x] Evaluar la viabilidad mediante una Decision Task de Flowable.
-   [x] Consultar desde el motor la cantidad de instancias de cursos que
    todavía no finalizaron.
-   [x] Excluir del conteo la instancia que evalúa su propia viabilidad.
-   [x] Considerar las distintas versiones desplegadas del proceso.
-   [x] Continuar si hay menos de 5 cursos en ejecución.
-   [x] Comunicar la imposibilidad y finalizar si hay 5 o más cursos en
    ejecución.
-   [x] Registrar la solicitud y elaborar la propuesta.
-   [ ] Generar el PDF de la propuesta.
-   [x] Enviar la propuesta a la empresa y esperar su respuesta.
-   [x] Contemplar aceptación, rechazo y solicitud de modificaciones.
-   [x] Permitir revisar la propuesta y enviar nuevas versiones.
-   [x] Reiniciar el contador de recordatorios al enviar una nueva
    versión.
-   [x] Enviar un recordatorio después de 5 días sin respuesta.
-   [x] Limitar los recordatorios a un máximo de 2.
-   [x] Archivar la solicitud si pasan otros 5 días después del segundo
    recordatorio sin respuesta.
-   [x] Archivar la solicitud si la empresa rechaza la propuesta.

### Preparación del curso

-   [x] Implementar las tres ramas paralelas.
-   [ ] **Inscripciones**
    -   [x] Recibir la nómina enviada por la empresa.
    -   [x] Confeccionar la lista definitiva.
-   [ ] **Materiales**
    -   [x] Preparar los materiales didácticos.
-   [ ] **Recursos**
    -   [x] Evaluar las necesidades de recursos.
    -   [x] Permitir reservar un aula física.
    -   [x] Permitir solicitar un aula virtual.
    -   [x] Permitir contratar equipamiento especial.
    -   [x] Exigir que se seleccione al menos una alternativa.
    -   [x] Completar todas las acciones seleccionadas.
-   [x] Sincronizar las tres ramas antes de continuar.

### Cancelación durante la preparación

-   [x] Permitir solicitar la cancelación durante el subproceso de
    preparación.
-   [x] Interrumpir la preparación al recibir la cancelación.
-   [x] Identificar los recursos que fueron gestionados correctamente.
-   [x] Liberar el aula física si fue reservada.
-   [x] Liberar el aula virtual si fue solicitada.
-   [x] Liberar el equipamiento si fue contratado.
-   [x] Ejecutar únicamente las compensaciones correspondientes a
    recursos gestionados.
-   [x] Notificar el cierre a la empresa.
-   [x] Finalizar el proceso con resultado cancelado.

### Desarrollo y finalización

-   [x] Enviar la confirmación a la empresa al finalizar la preparación.
-   [x] Permitir al jefe docente definir quién desarrollará el curso.
-   [ ] Permitir seleccionar cualquiera de los tres docentes, incluido
    el jefe.
-   [ ] Guardar el docente seleccionado en una variable del proceso.
-   [x] Asignar la tarea de desarrollo al docente seleccionado.
-   [x] Esperar hasta la fecha programada de inicio.
-   [x] Permitir al docente registrar la finalización del dictado.
-   [x] Permitir al jefe docente elaborar el informe de resultados.
-   [ ] Generar o adjuntar el informe en PDF.
-   [x] Permitir al jefe administrativo enviar el informe a la empresa.
-   [x] Archivar el expediente.
-   [ ] Ejecutar la compensación para liberar los recursos gestionados.
-   [ ] Finalizar el proceso una vez completada la liberación.

## 2. Usuarios, grupos y asignaciones de Flowable

### Configuración

-   [ ] Crear los 5 usuarios de la institución en Flowable.
-   [x] Crear el grupo `Administración`.
-   [x] Crear el grupo `Docentes`.
-   [x] Identificar al jefe administrativo.
-   [x] Identificar al jefe del equipo docente.
-   [x] Incorporar los integrantes correspondientes a cada grupo.

### Asignaciones

-   [x] Asignar al jefe administrativo las tareas de registrar,
    elaborar, enviar y revisar propuestas.
-   [x] Asignar al jefe administrativo el archivo de solicitudes
    rechazadas o sin respuesta.
-   [x] Asignar al jefe administrativo el envío del informe final y
    archivo del expediente.
-   [x] Configurar para el grupo `Administración` la confección de la
    lista definitiva.
-   [x] Configurar para el grupo `Administración` la preparación de
    materiales.
-   [x] Configurar para el grupo `Administración` la evaluación de
    recursos.
-   [x] Configurar para el grupo `Administración` la solicitud de aula
    virtual.
-   [x] Configurar para el grupo `Administración` la contratación y
    liberación de equipamiento.
-   [x] Asignar al jefe docente la definición del docente.
-   [x] Asignar al jefe docente la elaboración del informe de
    resultados.
-   [x] Asignar el desarrollo del curso al docente seleccionado.

### Reclamo de tareas

-   [x] Configurar grupos candidatos en las tareas compartidas.
-   [x] Permitir que los integrantes reclamen las tareas disponibles.
-   [x] Exigir el reclamo antes de completar una tarea grupal.
-   [x] Evitar que las tareas asignadas directamente requieran reclamo.
-   [x] Restringir las tareas asignadas directamente al usuario
    correspondiente.

## 3. Reglas de negocio, listeners y variables

### Regla de negocio: viabilidad

-   [x] Implementar la Decision Task.
-   [x] Obtener la cantidad de instancias activas desde el motor.
-   [x] Excluir la instancia actual del conteo.
-   [x] Considerar las distintas versiones desplegadas del proceso.
-   [x] Devolver un resultado para continuar o finalizar según el límite
    de 5 cursos.
-   [x] Evitar que la cantidad de instancias sea ingresada manualmente.

### Task Listener

-   [x] Implementar un Task Listener para
    `Confeccionar lista definitiva`.
-   [x] Asociarlo al evento de creación de la tarea.
-   [x] Consultar las tareas pendientes asignadas a cada administrativo.
-   [x] Contar únicamente tareas de usuario pendientes asignadas
    directamente.
-   [x] Excluir tareas que solo tengan al grupo como candidato y todavía
    no hayan sido reclamadas.
-   [x] Asignar la nueva tarea al administrativo con menos tareas
    pendientes.
-   [x] Definir un criterio para resolver empates.

### Execution Listener

-   [x] Implementar un Execution Listener al finalizar la instancia.
-   [x] Establecer una variable de resultado en cada camino de cierre.
-   [x] Actualizar el estado del curso en la base de datos
    institucional.
-   [x] Contemplar el estado `finalizado`.
-   [x] Contemplar el estado `cancelado`.
-   [x] Contemplar el estado `rechazado`.
-   [x] Contemplar el estado `cerrado por falta de respuesta`.
-   [x] Contemplar el estado `no viable`.

### Variables y temporizadores

-   [x] Definir las variables necesarias para gestionar las propuestas.
-   [ ] Definir las variables para las respuestas de la empresa.
-   [x] Definir el contador de recordatorios.
-   [x] Definir la fecha de inicio del curso.
-   [x] Definir los recursos requeridos.
-   [x] Definir el aula asignada.
-   [x] Definir los participantes.
-   [x] Definir el docente seleccionado.
-   [x] Definir el informe de resultados.
-   [x] Definir el resultado final del proceso.
-   [x] Configurar el temporizador de inicio utilizando la variable de
    fecha.
-   [ ] Garantizar que la fecha esté definida antes de alcanzar el
    evento temporizador.

## 4. Base de datos institucional

-   [x] Crear una base de datos propia de la institución.
-   [x] Crear la estructura para almacenar las empresas solicitantes.
-   [x] Almacenar el nombre del curso.
-   [x] Almacenar la descripción del curso.
-   [ ] Almacenar el programa.
-   [x] Almacenar el presupuesto.
-   [x] Almacenar las fechas.
-   [x] Almacenar el informe de resultados.
-   [ ] Vincular cada curso con su instancia correspondiente de
    Flowable.
-   [x] Definir cómo almacenar el ID de instancia del proceso.
-   [x] Definir qué datos se mantendrán como variables del proceso.
-   [ ] Crear scripts para crear la base de datos.
-   [ ] Crear scripts para cargar datos de prueba.

## 5. Sitio web de la institución

### Solicitud pública de capacitación

-   [ ] Crear una sección pública.
-   [ ] Crear el formulario de solicitud.
-   [ ] Solicitar los datos de la empresa.
-   [ ] Solicitar los datos de la persona de contacto.
-   [ ] Solicitar la temática de capacitación.
-   [ ] Solicitar la descripción de la capacitación.
-   [ ] Solicitar la cantidad estimada de participantes.
-   [ ] Solicitar la descripción de fechas preferidas.
-   [ ] Registrar la solicitud en la base de datos.
-   [ ] Iniciar una instancia del proceso en Flowable.
-   [ ] Vincular la solicitud con el ID de instancia.

### Acceso y seguimiento de empresas

-   [ ] Implementar autenticación para empresas.
-   [ ] Implementar la actividad automática
    `Habilitar acceso de la empresa`.
-   [ ] Crear una cuenta si la empresa todavía no dispone de una.
-   [ ] Reutilizar una cuenta existente si ya está registrada.
-   [ ] Vincular el nuevo curso con la cuenta existente.
-   [ ] Permitir consultar los cursos de la empresa.
-   [ ] Mostrar el estado `en preparación`.
-   [ ] Mostrar el estado `confirmado y pendiente de inicio`.
-   [ ] Mostrar el estado `en desarrollo`.
-   [ ] Mostrar el estado `en elaboración del informe`.
-   [ ] Mostrar el estado `finalizado`.
-   [ ] Mostrar el estado `cancelado`.
-   [ ] Permitir consultar las actividades pendientes o en ejecución
    durante la preparación.
-   [ ] Contemplar las actividades de las ramas paralelas.
-   [ ] Garantizar que cada empresa consulte exclusivamente sus propios
    cursos.

### Funciones para Administración

-   [ ] Implementar el acceso mediante credenciales de Flowable.
-   [ ] Consultar las tareas asignadas al usuario.
-   [ ] Consultar las tareas disponibles para su grupo.
-   [ ] Permitir reclamar tareas grupales.
-   [ ] Permitir completar las tareas correspondientes.
-   [ ] Respetar las asignaciones directas sin requerir reclamo.
-   [ ] Implementar el formulario para registrar y elaborar la
    propuesta.
-   [ ] Generar el PDF de la propuesta.
-   [ ] Implementar la revisión de la propuesta.
-   [ ] Generar nuevas versiones del PDF.
-   [ ] Implementar el archivo de solicitudes rechazadas o sin
    respuesta.
-   [ ] Implementar la confección de la lista definitiva.
-   [ ] Implementar la pantalla de evaluación de recursos.
-   [ ] Permitir seleccionar aula física, aula virtual y equipamiento.
-   [ ] Exigir al menos una alternativa seleccionada.
-   [ ] Permitir al jefe administrativo consultar el informe de
    resultados.
-   [ ] Permitir enviar el informe a la empresa y archivar el
    expediente.

### Integración con la interfaz de Flowable

-   [x] Permitir solicitar el aula virtual desde Flowable.
-   [x] Permitir contratar equipamiento desde Flowable.
-   [x] Permitir liberar equipamiento desde Flowable.
-   [x] Mantener automáticas la reserva y liberación del aula física.
-   [x] Simular la conexión con un Web Service para el aula física.
-   [x] Permitir elaborar el informe desde Flowable o, opcionalmente,
    desde el sitio web.
-   [ ] Generar o adjuntar el informe en PDF.

### Funciones para el equipo docente

-   [ ] Permitir al jefe docente consultar sus tareas pendientes de
    definición de docente.
-   [ ] Permitir seleccionar uno de los tres integrantes del equipo
    docente.
-   [ ] Permitir que el jefe docente se seleccione a sí mismo.
-   [ ] Permitir completar la tarea de definición.
-   [ ] Permitir al docente seleccionado consultar sus tareas de
    desarrollo.
-   [ ] Permitir completar la tarea de desarrollo del curso.

## 6. Aplicaciones auxiliares

### Aplicación Java --- Preparación de materiales

-   [ ] Crear una aplicación con interfaz gráfica.
-   [ ] Configurar su ejecución local dentro de la intranet.
-   [ ] Integrar con la API Java de Flowable.
-   [ ] Configurar el acceso a la base de datos del motor Flowable.
-   [ ] Permitir la identificación del administrativo.
-   [ ] Consultar las tareas `Preparar materiales` asignadas al usuario.
-   [ ] Consultar las tareas que el usuario pueda reclamar.
-   [ ] Permitir reclamar tareas disponibles para el grupo
    `Administración`.
-   [ ] Permitir completar las tareas.

### Aplicación REST --- Simulación de comunicaciones

-   [ ] Crear una aplicación para simular las comunicaciones de la
    empresa.
-   [ ] Permitir seleccionar una solicitud.
-   [ ] Integrar mediante la API REST de Flowable.
-   [ ] Permitir aceptar una propuesta incluyendo la fecha acordada.
-   [ ] Permitir rechazar una propuesta.
-   [ ] Permitir solicitar modificaciones incluyendo observaciones.
-   [ ] Permitir enviar la nómina de participantes.
-   [ ] Permitir cancelar durante la preparación.
-   [ ] Enviar los datos necesarios a la instancia correspondiente.
-   [ ] Mostrar únicamente las comunicaciones habilitadas según el
    estado actual del proceso.
-   [ ] Implementar la negociación desde esta aplicación.
-   [ ] Implementar el envío de nómina desde esta aplicación.
-   [ ] Si el grupo tiene hasta 3 integrantes, evaluar realizar estas
    acciones por línea de comandos sin interfaz gráfica.

## 7. Pruebas y demostración

### Escenarios obligatorios

-   [ ] Aceptación de propuesta y finalización normal del curso.
-   [ ] Verificar la liberación de recursos al finalizar normalmente.
-   [ ] Solicitud de modificaciones y envío de nueva propuesta.
-   [ ] Rechazo de propuesta.
-   [ ] Cierre por falta de respuesta después de los recordatorios.
-   [ ] Imposibilidad de continuar por el límite de cursos en ejecución.
-   [ ] Cancelación durante la preparación.
-   [ ] Verificar la compensación de recursos previamente gestionados.
-   [ ] Falta de aulas disponibles y generación de la excepción
    correspondiente.
-   [ ] Asignación de tareas según los responsables definidos.
-   [ ] Reclamo de tareas grupales.
-   [ ] Finalización de tareas por los usuarios correspondientes.
-   [ ] Registro de una solicitud desde el sitio web.
-   [ ] Inicio de instancia del proceso desde el sitio web.
-   [ ] Habilitación del acceso de la empresa.
-   [ ] Consulta exclusiva de cursos por empresa.
-   [ ] Preparación de materiales mediante la API Java.
-   [ ] Simulación de comunicaciones mediante la API REST.

### Controles adicionales recomendados

-   [ ] Verificar que no se permitan más de dos recordatorios.
-   [ ] Verificar que el contador se reinicie con cada nueva propuesta.
-   [ ] Verificar que se excluya la instancia actual del conteo de
    viabilidad.
-   [ ] Verificar la sincronización de las ramas paralelas.
-   [ ] Verificar que no se ejecuten compensaciones de recursos no
    gestionados.
-   [ ] Verificar que una tarea asignada directamente no pueda ser
    reclamada por otro usuario.
-   [ ] Verificar que una tarea grupal no pueda completarse sin haber
    sido reclamada.
-   [ ] Verificar que el docente seleccionado sea quien pueda completar
    el desarrollo.
-   [ ] Verificar que los estados de la base institucional coincidan con
    el resultado del proceso.
-   [ ] Verificar que el temporizador utilice la fecha configurada
    dinámicamente.

## 8. Entrega final

-   [x] Entregar el modelo BPMN ejecutable.
-   [ ] Entregar el código del servicio REST.
-   [x] Entregar el código de los componentes incorporados al motor.
-   [x] Entregar las reglas de negocio.
-   [x] Documentar la configuración de usuarios y grupos.
-   [x] Documentar cómo ejecutar la solución.
-   [x] Elaborar un informe breve explicando las variables.
-   [x] Elaborar un informe breve explicando los listeners.
-   [x] Justificar las decisiones de implementación.
-   [ ] Entregar el código del sitio web.
-   [ ] Entregar el código de la aplicación auxiliar Java.
-   [ ] Entregar el código de la aplicación de simulación REST.
-   [ ] Documentar cómo configurar y ejecutar las tres aplicaciones
    externas.
-   [ ] Preparar los datos necesarios para la demostración.
-   [ ] Verificar todos los escenarios obligatorios antes de la defensa.

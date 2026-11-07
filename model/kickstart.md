base de datos de la institucion

tomar decision al momento de relizar los cursos, como se maneja el almacenamiento de los datos

opcion elegida (backend como proxy / BFF):
1. guardamos los datos que nos manda el cliente y se los entregamos a flowable y actualizamos el id cuando nos responde
(asignamos id interno en nuestra DB, se lo mandamos a flowable como businessKey, y flowable nos responde con el id de instancia asignado, que guardamos en la DB del backend)
-> El backend hace de proxy/intermediario: valida, guarda los datos pesados del curso en su DB (y genera PDFs si hace falta), y luego le habla a Flowable por API REST.


Objetivo:
Hacer el seed de los usuarios del sistema medianto seed.sql, 
o de hacer un script que mediante la api del motor de flowable
registre los siguientes usuarios.

Previo a eso, deberia asignarse en el bpmn del proceso, los diferentes
id de los grupos a las tareas que van a estar organizada de la siguiente
forma

grupo de administracion:

id: jefeadministrativo 
tarea: registrar propuesta, enviarla, revisarla, archivar solicitud rechazada o sin respuesta. enviar informe final y archivar expediente
restriccion: 1

id: administrativos 
tarea: confeccionar lista, preparar materiales, evaluar
recursos, solicitar aula virtual, contratar equipamiento, realizar
user task de compensacion en caso de que la empresa solicite una
cancelacion
restriccion: 2 (van a tener la responsabilidad de reclamar la tarea, es decir ambos son candidatos)

id: docente
tarea: desarrollar curso, y registrar la finalizacion del dictado
restriccion: no hay al parecer
rol: va a ser asignado por el jefe del equipo en runtime (delegado)

id: jefeequipodocente
tarea: definir el docente que desarrollara el curso, y elaborar informe de resultados


### usuarios y grupos:
Definido: Usamos el backend como proxy entre el frontend y Flowable.

1. Autenticacion:
- Personal de la institucion: ingresan con su usuario y pass de Flowable. El backend valida contra Flowable y le genera el JWT/sesion al usuario.
- Empresas: tienen cuenta propia en la DB del backend (no en Flowable).
- Flujo:
user desde frontend -> backend -> valida credenciales / JWT -> backend hace la peticion a Flowable con credenciales tecnicas -> devuelve datos enriquecidos al frontend.

2. Formularios y PDFs (propuesta, revision, informe):
Flowable NO genera PDFs. Por lo tanto el flujo es:
user desde frontend -> manda formulario al backend -> backend valida y genera el PDF (lo guarda en disco/DB) -> backend completa la tarea en Flowable mandandole las variables correspondientes y URL del PDF.

por ejemplo:
se registra una solicitud en el backend, y el backend es el encargado de hablar con flowable e iniciar la instancia.
para pedir los formularios/tareas, el frontend se los pide al backend y el backend se los pide a Flowable.


### listeners

## task listener en confeccionar lista:
hacer una consulta contando tareas pendientes del grupo de administracion
y quedandonos con el usuario con menos

## execution listener (completed y eventos de Flowable):
cuando termina el proceso (o cambia de estado clave en Flowable), el listener que corre en Flowable le pega por HTTP al backend (ej: POST /api/cursos/{id}/estado con el resultado: finalizado, cancelado, rechazado, etc.).
Asi el backend actualiza el estado en su propia DB sin tener que estar haciendo polling a Flowable.


4. aplicaciones externas

a dividir entre 3 y 2 personas del grupo, cada grupo decidira las 
tecnologias que deseen

1. backend que renderize las vistas (ssr)
opciones: { flask / springboot , ... }

2. frontend / backend 
opciones: { next.js / vite } / { springboot / fastapi / ... }


4. la institucion va a ser la encargada de procesar y de entregar
informacion sobre las consultas de las empresas

para esto podriamos tener cuentas dentro del backend, para las empresas
en las cuales se hace la solicitud
solicitud(id, user_id, process_id)
cuando la empresa pregunte algo
backend -> fetch al motor de flowable -> process_id
flowable -> backend -> empresa

4.1.3 funciones para administracion:

1. consultas a flowable (a traves del backend)
2. claims y resoluciones al motor (a traves del backend)
3. generacion de PDFs en el backend al completar la tarea (propuesta, revision e informe)

5, 6, ... . mas cosas de flowable

4.2
Aplicacion core y aplicaciones auxiliares
Auxiliares:

PREPARACION DE MATERIALES:
definir app con java fx (o el framework visual q sea)
{deducido de : local a la intranet}
que consuma el motor desde la api
objetivo: usar la api mediante codigo

SIMULACION DE COMUNICACIONES:
definir frontend/backend o backend
{deducido de : api rest}
(empresa)
Core: 

SITIO WEB INSTITUCIONAL
(usado por los administrativos y docentes para gestionar el proceso)


PARA CUBRIR ESCENARIOS HACER TESTS DESEABLEMENTE
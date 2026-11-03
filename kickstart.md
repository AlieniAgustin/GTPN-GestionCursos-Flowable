base de datos de la institucion

tomar decision al momento de relizar los cursos, como se maneja el almacenamiento de los datos

opciones: 
1. guardamos los datos que nos manda el cliente y se los entregamos a flowabley actualizamos el id cuando nos responde
(asignamos id interno, se lo mandamos a flowable, y flowable nos dice, para este curso con id 1, se le asigno el id de instancia tanto)

2. le pegamos a flowable, y el motor se encarga de darnos ya los datos
y el backend se encarga de recibir los mismo
(flowable habla con nuestro backend, y recibe todo el objeto completo)

3. ?


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
hay qeu definir que es lo mas viable, tenemos varias forams de plantearlo
1. hablar con flowable y que nos devuelva que podemos hacer, el problema es
en caso de tener que generar el pdf, si o si necesitamos pasar por el backend
1. 
por lo tanto en este caso el flujo se encargaria de preguntar por el jwt que manda el usuario, de ahi el backend tiene autenticacion de quien es, y hace la peticion directamente con las credenciales tecnicas del backend, es decir

user desde frontend -> backend -> pregunta de quien es jwt a flowable -> devuelve rol y datos -> si no tiene permisos o lo que sea para hacer eso se rechaza o bien, se acepta
2. 
y la otra es que flowable dispare que tenemos que generar el documento de pdf
es decir user desde el frontend -> resuelve tarea -> flowable registra -> nos avisan que tenemos datos para guardar en el backend y nos mandan a generar el pdf (podria ser valido de todos modos, podria ser una tarea que se tenga que procesar y cuando se termina se le muestra al usuario)




por ejemplo:

se registra una soliciutd en el backend, y el backend es el encargado de hablar con flowable

para pedir los formularios/tareas directamente se piden desde flowable


### listeners

## task listener en confeccionar lista:
hacer una consulta contando tareas pendientes del grupo de administracion
y quedandonos con el usuario con menos

## execution listener (completed)
 cuando termina el proceso, el execution larga un evento, pegandole
 al backend, indicando que curso termino, asi el backend actualiza
 sus propios datos

 podria ponerse una variable en el estado, guardando el resultado
 para que al finalizar simplemente el fin, largue el evento

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

1. consultas a flowable
2. claims y resoluciones al motor
3. podria considerarse como execution listener completes la generacion de los pdfs
4. lo mismo para propuesta (avisar al backend genera este pdf)

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
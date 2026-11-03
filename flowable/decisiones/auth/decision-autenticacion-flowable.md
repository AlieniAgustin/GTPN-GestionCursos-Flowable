# Decisión arquitectónica: autenticación y acceso a Flowable

## Estado

**Decisión adoptada:** el backend institucional actuará como BFF
(Backend for Frontend) y como fachada/proxy de la API REST de Flowable.
Flowable será el responsable de autenticar a los empleados de la
institución y de administrar sus identidades dentro del motor.

Esta decisión busca que el frontend institucional se comunique
únicamente con su backend, en lugar de combinar llamadas directas a
Flowable con llamadas al backend.

## 1. Componentes y responsabilidades

### Frontend institucional

-   Presenta el formulario de inicio de sesión.
-   Envía las credenciales al backend institucional.
-   Después del login, conserva únicamente la cookie de sesión propia
    del backend institucional.
-   Consume las operaciones expuestas por el backend: consulta de
    tareas, ejecución de tareas y consultas de solicitudes, entre otras.
-   No conoce la API interna de Flowable ni recibe su `JSESSIONID`.

### Backend institucional

-   Actúa como BFF y fachada de Flowable.
-   Recibe y procesa las peticiones del frontend.
-   Gestiona la sesión que utiliza el frontend para comunicarse con él.
-   Mantiene del lado servidor la asociación entre la sesión
    institucional y la sesión autenticada en Flowable.
-   Realiza las llamadas a Flowable en nombre del empleado autenticado.
-   Aplica las validaciones y autorizaciones propias de la aplicación
    institucional.
-   Evita que el frontend tenga que conocer qué operaciones requieren
    interacción con Flowable y cuáles requieren lógica propia del
    backend.

### Flowable

-   Mantiene los usuarios y credenciales de los empleados
    institucionales.
-   Autentica a esos empleados.
-   Gestiona los procesos, las tareas, los grupos y las asignaciones
    correspondientes.
-   Aplica los permisos configurados en el motor al ejecutar operaciones
    sobre tareas y procesos.

## 2. Flujo de inicio de sesión

1.  El empleado ingresa sus credenciales en el frontend institucional.
2.  El frontend envía las credenciales al endpoint de login del backend
    institucional, por ejemplo `POST /api/auth/login`.
3.  El backend institucional utiliza esas credenciales para autenticar
    al empleado contra Flowable.
4.  Si la autenticación es correcta, Flowable establece una sesión HTTP
    y devuelve su cookie `JSESSIONID`.
5.  El backend institucional conserva esa cookie de forma segura,
    asociada a la sesión del empleado.
6.  El backend crea su propia sesión para el frontend y devuelve una
    cookie, por ejemplo `SESSION`.
7.  El navegador conserva únicamente la cookie `SESSION`.

El `JSESSIONID` de Flowable no debe enviarse al navegador.

## 3. Flujo de una operación autenticada

Por ejemplo, para consultar las tareas del empleado:

1.  El frontend realiza `GET /api/tareas` y envía automáticamente la
    cookie `SESSION`.
2.  El backend valida la sesión institucional e identifica al empleado.
3.  El backend recupera el `JSESSIONID` de Flowable asociado a esa
    sesión.
4.  El backend consulta la API REST de Flowable enviando la cookie
    `JSESSIONID`.
5.  Flowable reconoce al empleado autenticado y responde según sus
    permisos y asignaciones.
6.  El backend adapta la respuesta si corresponde y se la devuelve al
    frontend.

Ejemplo conceptual de la petición interna del backend a Flowable:

``` http
GET /flowable-rest/service/runtime/tasks
Cookie: JSESSIONID=<sesion-de-flowable>
```

## 4. Manejo de las sesiones

Se administran dos sesiones independientes:

  -----------------------------------------------------------------------
  Sesión            Quién la utiliza  Quién la valida   Propósito
  ----------------- ----------------- ----------------- -----------------
  `SESSION`         Navegador →       Backend           Identificar al
                    backend           institucional     empleado ante
                    institucional                       nuestra API

  `JSESSIONID`      Backend           Flowable          Identificar al
                    institucional →                     empleado ante la
                    Flowable                            API REST del
                                                        motor
  -----------------------------------------------------------------------

No se debe asumir que ambas sesiones vencen al mismo tiempo.

El backend institucional debe: - Mantener la asociación entre ambas
sesiones. - Respetar el vencimiento de su propia sesión. - Detectar
cuándo Flowable rechaza la sesión o deja de considerarla válida. -
Invalidar o renovar la asociación cuando corresponda. - Gestionar el
cierre de sesión en ambos lados. - No interpretar cualquier
`403 Forbidden` como una sesión vencida: puede indicar falta de
permisos. Un `401 Unauthorized` suele indicar que la autenticación no es
válida, pero debe confirmarse el comportamiento concreto de la API
configurada.

La respuesta de Flowable es la referencia final para saber si la sesión
de Flowable continúa siendo aceptada.

## 5. Consideración técnica: configuración de seguridad de Flowable

La REST API estándar de Flowable Open Source utiliza autenticación HTTP
Basic y una configuración sin estado (`STATELESS`). Por lo tanto, el uso
de `JSESSIONID` como sesión persistente **no está disponible
automáticamente** en la configuración estándar.

Para implementar el flujo decidido será necesario personalizar la
seguridad de la aplicación que expone Flowable, de manera que:

-   El login autentique al usuario contra el mecanismo de identidad de
    Flowable.
-   La autenticación establezca un contexto de seguridad asociado a una
    sesión HTTP.
-   Las siguientes peticiones REST puedan recuperar ese contexto desde
    la sesión.
-   La sesión tenga una política de expiración y cierre definida.

No alcanza con validar las credenciales y generar manualmente una cookie
llamada `JSESSIONID`: debe ser una sesión real reconocida por la
configuración de seguridad de Flowable.

La duración de la sesión y el comportamiento de expiración deberán
definirse y probarse en esa configuración.

## 6. Operaciones de empresa e institución

Se mantiene una diferencia entre los dos tipos de actores:

### Operaciones de empleados institucionales

Los empleados sí tienen identidad en Flowable. Las operaciones que
realizan sobre tareas deben ejecutarse preservando esa identidad, para
que el motor pueda aplicar sus permisos y mantener la trazabilidad del
usuario.

El backend institucional actúa como intermediario y utiliza la sesión de
Flowable asociada al empleado.

### Solicitudes originadas por empresas

Las empresas no necesariamente tienen usuarios en Flowable. Para iniciar
procesos o registrar solicitudes en su nombre, el backend que recibe
esas solicitudes puede comunicarse con Flowable utilizando una cuenta
técnica de servicio, con privilegios mínimos.

La cuenta técnica no debe confundirse con la identidad de un empleado:
Flowable registrará esa cuenta como ejecutora de la operación, salvo que
se implemente un mecanismo adicional de atribución del solicitante.

## 7. Criterio para exponer operaciones al frontend

El frontend institucional consumirá las operaciones a través del backend
institucional de forma uniforme.

Ejemplos de endpoints propios:

-   `POST /api/auth/login`
-   `POST /api/auth/logout`
-   `GET /api/tareas`
-   `POST /api/tareas/{id}/completar`
-   `GET /api/solicitudes/{id}`

El backend decidirá cuándo debe consultar o ejecutar una operación en
Flowable y cuándo debe aplicar lógica propia. Esto evita que el frontend
tenga que conocer la distribución interna de responsabilidades y evita
mezclar llamadas directas al motor con llamadas al backend.

## 8. Decisiones y puntos pendientes

### Decisiones tomadas

-   Flowable administra las identidades de los empleados
    institucionales.
-   El backend institucional funciona como BFF/fachada frente a
    Flowable.
-   El navegador conserva la cookie de sesión del backend, no el
    `JSESSIONID` de Flowable.
-   El backend conserva y reenvía la sesión de Flowable en las llamadas
    internas.
-   Las operaciones de empleados deben preservar su identidad ante
    Flowable.
-   Las operaciones iniciadas por empresas pueden utilizar una cuenta
    técnica, ya que las empresas no tienen por qué ser usuarios del
    motor.

### Trabajo técnico pendiente

-   Verificar la versión y configuración concreta de Flowable utilizada.
-   Implementar y probar la autenticación con sesión HTTP en la REST API
    de Flowable.
-   Definir el almacenamiento seguro de las sesiones de Flowable en el
    backend institucional.
-   Definir expiración, cierre de sesión, invalidación y comportamiento
    ante reinicios.
-   Verificar cómo se propagan los errores de autenticación y
    autorización.
-   Probar que Flowable registra al empleado correcto al consultar y
    completar tareas.
-   Definir los privilegios mínimos de la cuenta técnica utilizada para
    las operaciones de empresa.

------------------------------------------------------------------------

**Resumen:** el frontend se autentica ante el backend institucional
mediante una sesión propia. El backend, a su vez, mantiene una sesión
autenticada en Flowable y la utiliza para operar en nombre del empleado.
Flowable sigue siendo responsable de autenticar a los empleados y de
aplicar sus permisos sobre procesos y tareas. Esta solución requiere
personalizar la seguridad de la REST API de Flowable; no debe asumirse
que `JSESSIONID` funciona así de fábrica.

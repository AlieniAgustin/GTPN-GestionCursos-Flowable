# ADR 002: Autenticación con Sesión HTTP y Propagación de Identidad en Flowable

> **Decisión en una frase:** Autenticar con HTTP Basic validado contra el `IdentityService` de Flowable mediante un `AuthenticationProvider` custom, persistir el `SecurityContext` en la `HttpSession` (`JSESSIONID`) y dejar que Flowable lea la identidad directamente desde Spring Security, sin filtros puente.

---

## Resumen Ejecutivo

| Aspecto | Definición adoptada |
| :--- | :--- |
| **Estado** | Aceptado (corregido tras implementación) |
| **Componente afectado** | `flowable` (módulo REST / Motor de Procesos) |
| **Mecanismo de transporte** | Cookie `JSESSIONID` (sesión servlet en Tomcat) tras handshake inicial con HTTP Basic |
| **Validación de credenciales** | `FlowableAuthenticationProvider` → `IdentityService.checkPassword(...)` |
| **Mapeo de roles** | Grupos IDM (`ACT_ID_GROUP`) traducidos a autoridades Spring (`ROLE_<GRUPO>`) |
| **Identidad en el Motor** | La lee Flowable desde `SecurityContextHolder` vía `SpringSecurityAuthenticationContext`; no se copia a ningún ThreadLocal propio |
| **Doc relacionada** | [decision-autenticacion-flowable.md](decision-autenticacion-flowable.md) |

---

## Matriz de Decisiones de Arquitectura

| Componente | Qué se decidió | Por qué no se usó la alternativa por defecto |
| :--- | :--- | :--- |
| **`SecurityConfig`** | `SessionCreationPolicy.IF_REQUIRED`, `httpBasic()` y un `HttpSessionSecurityContextRepository` compartido entre `securityContext(...)` y `httpBasic(...)`. | En Spring Security 6, `httpBasic()` no persiste el contexto en sesión por defecto (usa un repositorio de atributo de request). Sin el repositorio de sesión, el segundo request vuelve a ser anónimo. |
| **`FlowableAuthenticationProvider`** | `AuthenticationProvider` custom que valida con `IdentityService.checkPassword(...)` y mapea los grupos del usuario a `ROLE_<GRUPO>`. | El starter de Flowable registra un `FlowableUserDetailsService` que mapea privilegios (`ACT_ID_PRIV`) y no los grupos del proceso. Se prefiere validar contra IDM y construir las autoridades desde las membresías. |
| **`FlowableIdentityFilter`** | **Eliminado.** No se copia la identidad a `org.flowable.common.engine.impl.identity.Authentication`. | Flowable ya registra `SpringSecurityAuthenticationContext`, que lee el usuario desde Spring Security. Escribir con `Authentication.setAuthenticatedUserId(...)` pisa el `SecurityContext` y rompe la autorización (ver corrección abajo). |
| **Almacenamiento de Sesión** | Sesión en RAM del servidor. La contraseña no se guarda: el token se construye con `credentials = null`. | La cookie solo viaja como identificador opaco; las credenciales nunca quedan en sesión. |

---

## Flujo Operativo (Handshake y Ciclo de Vida)

```
1. Handshake inicial (Login)
   Cliente/BFF ──[ Basic Auth: user + pass ]──► Flowable
   FlowableAuthenticationProvider ──► IdentityService.checkPassword(...)
   ──► BasicAuthenticationFilter guarda SecurityContext en HttpSession
   ──► Responde Set-Cookie: JSESSIONID

2. Request autenticado
   Cliente/BFF ──[ Cookie: JSESSIONID ]──► Flowable
   Tomcat recupera HttpSession ──► SecurityContextHolderFilter carga SecurityContext
   ──► Flowable lee getAuthenticatedUserId() desde Spring Security

3. Ejecución
   Motor Flowable lee el usuario ──► Registra auditoría ACT_HI_*
   No hay ThreadLocal propio que limpiar: el SecurityContext lo administra Spring Security
```

---

## Anatomía de la Sesión

### En el Cliente / Backend BFF

- **`JSESSIONID=...`**: identificador opaco, sin usuario ni credenciales.

### En el Servidor (RAM de Tomcat)

- Atributo `"SPRING_SECURITY_CONTEXT"`:
  - `SecurityContextImpl`
    - `Authentication`: `UsernamePasswordAuthenticationToken`
      - `principal`: `String` con el username (`"docente1"`)
      - `authorities`: roles calculados (`[ROLE_DOCENTES]`)
      - `credentials`: `null`
      - `authenticated`: `true`
- Expiración: 30 minutos de inactividad por defecto.

La cookie **no contiene** el `Authentication`: solo apunta a la sesión del servidor.

---

## Corrección Post-Implementación: 401 después de un login exitoso

### Síntoma

- `checkPassword(...)` devolvía `true` y se imprimía `good`.
- El primer request respondía `401` y el segundo, con `JSESSIONID`, también `401`.
- El log mostraba:

```text
good
Set SecurityContextHolder to UsernamePasswordAuthenticationToken [Principal=docente1, ... Authenticated=true]
Stored SecurityContextImpl [...] to HttpSession
AnonymousAuthenticationFilter : Did not set SecurityContextHolder since already authenticated
    UsernamePasswordAuthenticationToken [Principal=org.flowable.common.engine.impl.identity.UserIdPrincipal@..., Authenticated=false]
AccessDeniedException: Access Denied
Responding with 403 status code
Securing GET /error -> 401
```

### Causa

`FlowableIdentityFilter` ejecutaba:

```java
Authentication.setAuthenticatedUserId(auth.getName());
```

Con Spring Security en el classpath, Flowable usa `SpringSecurityAuthenticationContext`. Su método `setPrincipal(...)` **no** guarda un ThreadLocal: reemplaza la autenticación del `SecurityContextHolder` con:

```java
new UsernamePasswordAuthenticationToken(new UserIdPrincipal(userId), null)
```

Ese constructor de dos argumentos deja el token con `authenticated = false`.

Secuencia del fallo:

1. `AuthorizationFilter` ve el token no autenticado → lanza `AccessDeniedException` → 403.
2. Spring hace dispatch interno a `/error`, que también pasa por seguridad como anónimo → **401 visible**.
3. El `finally` del filtro ejecuta `setAuthenticatedUserId(null)`, que deja sin autenticación el mismo `SecurityContextImpl` guardado en la sesión.
4. El segundo request con `JSESSIONID` carga ese contexto vacío → 401 otra vez.

### Decisión tomada

Eliminar `FlowableIdentityFilter` y su registro `.addFilterAfter(...)`. Flowable obtiene el usuario desde Spring Security mediante:

```java
SecurityContextHolder.getContext().getAuthentication().getName()
```

No hay que copiar ni limpiar nada: el ciclo de vida del `SecurityContext` lo maneja Spring Security.

### Verificación

```text
GET /me con Basic          -> 200
GET /me con JSESSIONID     -> 200
```

---

## Checklist de Verificación para Revisores

- [ ] **Sin credenciales en sesión:** la contraseña no se persiste ni viaja en atributos de sesión.
- [ ] **Sin filtros puente:** no llamar a `Authentication.setAuthenticatedUserId(...)` desde un filtro de la cadena de Spring Security.
- [ ] **Repositorio compartido:** el mismo `HttpSessionSecurityContextRepository` está configurado en `securityContext(...)` y en `httpBasic(...)`.
- [ ] **Sesión no STATELESS:** `SessionCreationPolicy.IF_REQUIRED`.
- [ ] **Mapeo de roles:** los grupos creados en `App.java` (`docentes`, `administracion`) coinciden con los requeridos por el BPMN.
- [ ] **Una sola cadena:** verificar en el arranque que no exista otra `SecurityFilterChain` con mayor precedencia.
- [ ] **Producción:** reemplazar el encoder de texto plano de IDM por uno con hash (BCrypt/Argon2) antes de desplegar.

---

## Trazabilidad y Archivos

- [`SecurityConfig.java`](file:///home/francisco/GTPN-GestionCursos-Flowable/flowable/src/main/java/com/flowable/app/security/SecurityConfig.java): cadena de filtros, sesión, rutas públicas y CORS.
- [`FlowableAuthenticationProvider.java`](file:///home/francisco/GTPN-GestionCursos-Flowable/flowable/src/main/java/com/flowable/app/security/FlowableAuthenticationProvider.java): validación contra IDM y mapeo de grupos a roles.
- [`MeController.java`](file:///home/francisco/GTPN-GestionCursos-Flowable/flowable/src/main/java/com/flowable/app/MeController.java): endpoint usado para verificar autenticación y sesión.
- [`App.java`](file:///home/francisco/GTPN-GestionCursos-Flowable/flowable/src/main/java/com/flowable/app/App.java): creación de grupos y usuarios de desarrollo.
- [`FlowableAuthService.java`](file:///home/francisco/GTPN-GestionCursos-Flowable/backend/src/main/java/com/backend/proxy/auth/FlowableAuthService.java): cliente BFF que consume el handshake y retiene la cookie.

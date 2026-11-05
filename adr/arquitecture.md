# ADR: Arquitectura de Seguridad Stateless y Backend Proxy con Cuenta Técnica

El sistema desacopla la autenticación de usuarios de la ejecución de procesos mediante un Backend-for-Frontend (BFF) con sesiones HTTP de 30 minutos y un motor Flowable completamente stateless que se comunica exclusivamente a través de una cuenta técnica centralizada.

## Flujo Rápido

1. **Autenticación institucional:** El usuario envía credenciales a `/api/auth/login`. El Backend las valida contra el endpoint `/me` de Flowable.
2. **Sesión local en Backend:** Al validar la identidad, el Backend crea una sesión HTTP estándar (`HttpSession`) con vencimiento a los 30 minutos de inactividad.
3. **Comunicación técnica con Flowable:** Las peticiones de negocio delegan la ejecución en Flowable usando la cuenta técnica (`admin:test`), propagando la identidad del usuario únicamente como parámetro de consulta o asignación.

## Decisiones Principales

| Área | Decisión | Motivo |
|---|---|---|
| **Flowable** | `SessionCreationPolicy.STATELESS` | Elimina la persistencia y sincronización de sesiones HTTP en el motor de procesos. |
| **Backend Proxy (BFF)** | Sesiones HTTP estándar (`HttpSession`) | Centraliza el control de inactividad (30 min) y aísla al cliente del motor BPMN. |
| **Integración Backend-Flowable** | Cuenta técnica única (*Service Account*) vía HTTP Basic Auth | Evita la manipulación manual de cookies `JSESSIONID` y desacopla el transporte. |
| **Autorización y Visibilidad** | Identidad propagada en llamadas REST | Flowable filtra tareas e instancias según el usuario final inyectado (`candidateOrAssigned`). |

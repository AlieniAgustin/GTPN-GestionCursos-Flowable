# [Título de la Sección o Resolución]

Documento de diseño y flujo operativo para [breve descripción del propósito, alcance o hito del proceso].

---

## 1. Contexto Operativo y Alcance

Descripción del escenario de negocio y el punto de entrada dentro del ciclo de vida del curso (ej. subproceso, recepción de evento, tarea de usuario).

### Tareas y Actores Involucrados
* **[Tarea / Evento 1]:** Tipo (`userTask`, `serviceTask`, `scriptTask`, `messageEvent`) y responsable o grupo (`administracion`, `docente`, `sistema`).
* **[Tarea / Evento 2]:** Descripción de la responsabilidad operativa y condiciones de ejecución.

### Contrato de Entrada / Variables del Proceso
```java
// DTOs de entrada o variables inyectadas en la ejecución de Flowable
public record [NombreDTO](
    // campos relevantes
) {}
```

---

## 2. Reglas de Negocio y Restricciones Técnicas

Detalle de las políticas de negocio aplicables o consideraciones técnicas/comportamiento del motor (Flowable 6.8).

### Reglas de Negocio
* **[Criterio o Condición]:** Descripción de la regla.
> **Decisión:** [Definición formal de la regla de negocio acordada].

### Consideraciones del Motor / Arquitectura (Opcional si aplica)
* **Comportamiento en Flowable:** [Particularidades transaccionales, scope de ejecución, lifecycle de tareas].
* **Riesgo o Problema Evitado:** [Consecuencias de no contemplar esta restricción].

---

## 3. Decisión de Diseño y Flujo de Ejecución

Esquema del flujo adoptado para resolver el escenario de negocio y garantizar consistencia de datos y estados.

### Diagrama de Flujo
```text
[Inicio / Disparador]
         │
         ▼
  [Tarea / Decisión]  ──► [Subproceso o Compensación]
         │
         ▼
[Punto de Control / Gateway]
         │
         ▼
  [Fin / Siguiente Fase]
```

### Reglas del Diseño Adoptado
1. **[Aspecto Clave 1]:** Justificación de la configuración BPMN (ej. `isInterrupting`, listeners, scopes).
2. **[Aspecto Clave 2]:** Gestión de variables de control y sincronización de estados.
3. **[Aspecto Clave 3]:** Puntos de pausa, intervención humana o delegación externa.

---

## 4. Integración, Persistencia y Alternativas

Impacto en el modelo de dominio, contratos con el backend o tabla de tradeoffs si hubo análisis comparativo.

### Modelo de Persistencia / Variables Clave
```java
// Estructuras de datos a persistir o actualizar en backend/base de datos
```

### Comparativa de Alternativas (si aplica)
| Enfoque | Viabilidad | Motivo de Descarte o Adopción |
|---|---|---|
| [Alternativa 1] | Inviable / Descartado | [Motivo técnico o funcional] |
| **[Enfoque Seleccionado]** | **Adoptado** | [Justificación técnica de la elección] |

---

## 5. Casos de Verificación y Testing

Definición de escenarios de prueba para validar la implementación en la suite de tests (ej. `FlowableTest` / integración).

* **Caso Nominal:** [Descripción del flujo exitoso y aserciones esperadas sobre variables y estados].
* **Caso Alternativo / Error:** [Comportamiento esperado ante cancelación, rechazo o desvío de flujo].
* **Caso de Borde / Concurrencia:** [Validación de condiciones límite, rollback o consistencia transaccional].

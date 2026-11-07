# Resolución de Viabilidad y Acuerdo

Documento de diseño y flujo operativo para el análisis de viabilidad, negociación de propuestas y persistencia de acuerdos de cursos.

---

## 1. Inicio del Proceso: Solicitud de Curso

El proceso siempre comienza con la recepción de la solicitud de la empresa mediante el DTO de entrada:

```java
public record CourseRequestDTO(
    CompanyContactDTO contact,
    String name,
    String description,
    Integer participantCount,
    List<LocalDate> possibleDates
) {}
```

Toda esta información es recibida e inyectada inmediatamente como **variables del proceso** en Flowable, quedando disponible para las etapas posteriores de evaluación y negociación.

---

## 2. Análisis de Viabilidad y Capacidad

Antes de avanzar, se evalúa la carga operativa analizando cuántos cursos se están llevando a cabo en simultáneo mediante una variable de control.

### Criterio de distinción de cursos
* **Solicitudes en vuelo:** Puede haber múltiples peticiones en proceso de negociación, pero si aún no llegaron a un acuerdo, **no** se consideran cursos en ejecución.
* **Cursos activos:** Únicamente aquellos procesos que alcanzan formalmente la **instancia de aceptación** pasan a computar como cursos activos.

### Regla de negocio
> **Decisión:** Solo se empieza a filtrar y rechazar nuevas solicitudes cuando ya existan **5 cursos efectivos concurrentes**.

---

## 3. Definición y Negociación de la Propuesta

A partir de las variables almacenadas en el proceso, el frontend expone los datos de la solicitud para que el personal administrativo pueda evaluarlos y armar la contrapropuesta:

```java
public record ProposalResponseDTO(
    String program,
    BigDecimal budget,
    List<LocalDate> availableDates
) {}
```

### Interacción administrativa (Frontend)
1. El administrativo visualiza las variables del proceso (fechas tentativas y requerimientos de la empresa).
2. Toma una decisión sobre la viabilidad académica y presupuestaria.
3. Puede seleccionar fechas sugeridas por la empresa y/o proponer, agregar o quitar días según conveniencia y disponibilidad institucional.

---

## 4. Resolución, Integración y Modelo de Persistencia

Una vez resuelta y acordada la propuesta:

1. **Sincronización con el Backend Institucional:**  
   Se consume el backend institucional para actualizar:
   * Programa académico.
   * Presupuesto final acordado.
   * Fechas definitivas.

2. **Evolución sugerida del Modelo de Datos (Backend):**  
   Para mantener la trazabilidad de la negociación y auditoría de fechas, se propone persistir en el backend:

```java
List<LocalDate> requestCompany;       // Fechas originalmente solicitadas por la empresa
List<LocalDate> responseInstitution;  // Fechas propuestas/ofertadas por la institución
List<LocalDate> agreedDays;           // Fechas definitivas (resueltas al cerrar el acuerdo)
```

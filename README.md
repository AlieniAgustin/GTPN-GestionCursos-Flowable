# Gestión de Cursos de Capacitación — GTPN

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Flowable](https://img.shields.io/badge/Flowable-7.0.0-009688?style=flat-square)](https://www.flowable.com/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.9+-C71A36?style=flat-square&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![H2 Database](https://img.shields.io/badge/H2-In--Memory%20%26%20File-003B57?style=flat-square)](https://www.h2database.com/)

Repositorio del proyecto final para la asignatura **Gestión y Tecnologías de Procesos de Negocio (GTPN)**. Contiene el modelado BPMN 2.0 optimizado para ejecución, la documentación de diseño y el proyecto Java estructurado con **Spring Boot 3** y el motor **Flowable Engine 7.0.0** sobre base de datos H2.

---

## Contenido del Repositorio

- **Modelos BPMN 2.0:**
  - `cursosNuestraVersion.bpmn`: Modelo refinado adaptado para el motor de procesos, aplicando las 7 Guías de Modelado (7PMG) y resolviendo aspectos técnicos como listeners de ejecución y eventos de mensaje.
  - `cursosOriginal.bpmn`: Diagrama conceptual base provisto por la cátedra.
- **Documentación Técnica:**
  - `informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md`: Justificación detallada de cada cambio estructural respecto al modelo original.
  - `ProyectoCursos.pdf`: Enunciado oficial de la cátedra con los requerimientos funcionales y escenarios de evaluación.
- **Módulo Flowable (`flowable/`):**
  - Aplicación Spring Boot 3 con Flowable 7.0.0, soporte REST (`spring-boot-starter-web`), validación de datos (`spring-boot-starter-validation`), Lombok y base de datos H2.

> [!NOTE]
> Para conocer el detalle de las decisiones de diseño (como la incorporación de compuertas XOR convergentes y el reemplazo de tareas de recepción por eventos intermedios), consulta el [Informe de Justificación de Cambios y Decisiones BPMN](informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md).

---

## Requisitos Previos

- **Java Development Kit (JDK) 17** o superior
- **Apache Maven 3.8+**
- **Git**

---

## Inicio Rápido

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/AlieniAgustin/GTPN-GestionCursos-Flowable.git
   cd GTPN-GestionCursos-Flowable/flowable
   ```

2. **Compilar el proyecto:**
   ```bash
   mvn clean compile
   ```

3. **Ejecutar las pruebas unitarias:**
   ```bash
   mvn test
   ```

4. **Iniciar la aplicación (modo desarrollo):**
   ```bash
   mvn spring-boot:run
   ```
   La aplicación iniciará en `http://localhost:8080`.
   * **Consola H2 (solo en `dev`):** `http://localhost:8080/h2-console`
     * **JDBC URL:** `jdbc:h2:mem:flowable_dev`
     * **Usuario:** `sa`
     * **Contraseña:** *(vacía)*

---

## Perfiles de Ejecución y Configuración


### 1. Perfiles de Spring Boot (`application-*.properties`)

* **`dev` (por defecto):**
  * Base de datos H2 en memoria (`flowable_dev`).
  * Consola web de H2 habilitada en `/h2-console` para inspeccionar tablas de Flowable (`ACT_RU_*`, `ACT_HI_*`, etc.).
* **`prod`:**
  * Base de datos H2 persistente en archivo (`./data/flowable_prod`) o integrable con PostgreSQL/MySQL.
  * Consola H2 deshabilitada por seguridad.


### 2. Perfiles de Maven (`pom.xml`)

* **Perfil `dev` (activo por defecto):**
  * Incluye `exec-maven-plugin` para levantar directamente la aplicación:
    ```bash
    mvn compile exec:java -Pdev
    ```
* **Perfil `prod`:**
  * Utiliza `maven-shade-plugin` y `spring-boot-maven-plugin` para generar el fat JAR empaquetado para distribución:
    ```bash
    mvn clean package -Pprod
    ```

---

## Estructura de Archivos

```text
├── cursosNuestraVersion.bpmn        # Modelo BPMN 2.0 refinado y preparado para Flowable
├── cursosOriginal.bpmn              # Modelo BPMN conceptual base de la cátedra
├── informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md # Justificación técnica y 7PMG
├── ProyectoCursos.pdf               # Enunciado y requerimientos de la materia
└── flowable/                        # Módulo del motor de procesos y API REST
    ├── pom.xml                      # Dependencias Spring Boot, Flowable y perfiles
    └── src/
        ├── main/
        │   ├── java/com/flowable/app/
        │   │   └── App.java         # Punto de entrada (@SpringBootApplication)
        │   └── resources/
        │       ├── application.properties      # Configuración común y perfil activo
        │       ├── application-dev.properties  # Configuración para desarrollo (H2 mem)
        │       └── application-prod.properties # Configuración para producción (H2 file)
        └── test/java/com/flowable/app/
            └── AppTest.java         # Test de contexto Spring Boot
```

---

## Equipo de Trabajo

- **Materia:** Gestión y Tecnologías de Procesos de Negocio (GTPN)
- **Profesor:** Dr. Ariel Gonzalez
- **Integrantes:**
  - Agustin Alieni
  - Agustin Cesari
  - Damian Dalio
  - Hernan Jara
  - Francisco Natale

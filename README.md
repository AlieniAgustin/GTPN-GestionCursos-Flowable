# Gestión de Cursos de Capacitación — GTPN

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Flowable](https://img.shields.io/badge/Flowable-7.0.0-009688?style=flat-square)](https://www.flowable.com/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.9+-C71A36?style=flat-square&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![H2 Database](https://img.shields.io/badge/H2-In--Memory-003B57?style=flat-square)](https://www.h2database.com/)

Repositorio del proyecto final para la asignatura **Gestión y Tecnologías de Procesos de Negocio (GTPN)**. Contiene el modelado BPMN 2.0 optimizado para ejecución, la documentación de diseño y el proyecto Java con el motor **Flowable Engine** configurado en modo Standalone sobre base de datos H2.

---

## Contenido del Repositorio

- **Modelos BPMN 2.0:**
  - `cursosNuestraVersion.bpmn`: Modelo refinado adaptado para el motor de procesos, aplicando las 7 Guías de Modelado (7PMG) y resolviendo aspectos técnicos como listeners de ejecución y eventos de mensaje.
  - `cursosOriginal.bpmn`: Diagrama conceptual base provisto por la cátedra.
- **Documentación Técnica:**
  - `informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md`: Justificación detallada de cada cambio estructural respecto al modelo original.
  - `ProyectoCursos.pdf`: Enunciado oficial de la cátedra con los requerimientos funcionales y escenarios de evaluación.
- **Módulo Flowable (`flowable/`):**
  - Proyecto Maven con Flowable 7.0.0, base de datos H2 en memoria y configuración de perfiles para desarrollo y empaquetado.

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

---

## Perfiles de Desarrollo y Ejecución (Maven Profiles)

El módulo [`flowable/pom.xml`](flowable/pom.xml) cuenta con dos perfiles para separar el flujo de trabajo en desarrollo del empaquetado final:

### 1. Perfil de Desarrollo (`dev`)

Permite ejecutar el punto de entrada principal [`com.flowable.app.App`](flowable/src/main/java/com/flowable/app/App.java) de forma directa mediante `exec-maven-plugin`, agilizando las pruebas locales sin generar archivos empaquetados:

```bash
mvn compile exec:java -Pdev
```

### 2. Perfil de Producción / Empaquetado (`prod`)

Utiliza `maven-shade-plugin` para compilar y empaquetar un *uber-jar* (fat JAR) autocontenido con todas las dependencias y el manifiesto configurado:

```bash
mvn clean package -Pprod
```

El archivo ejecutable se genera en `target/` y se ejecuta con:

```bash
java -jar target/flowable-1.0-SNAPSHOT.jar
```
 El JAR generado con `-Pprod` incluye Flowable Engine y la base de datos H2 embebida, permitiendo su ejecución sin necesidad de contar con Maven en el entorno destino.

---

## Estructura de Archivos

```text
├── cursosNuestraVersion.bpmn        # Modelo BPMN 2.0 refinado y preparado para Flowable
├── cursosOriginal.bpmn              # Modelo BPMN conceptual base de la cátedra
├── informeDeJustificacionDeCambiosyDecisionesDeModeladoBPMN.md # Justificación técnica y 7PMG
├── ProyectoCursos.pdf               # Enunciado y requerimientos de la materia
└── flowable/                        # Proyecto del motor de procesos
    ├── pom.xml                      # Dependencias y perfiles Maven (dev / prod)
    └── src/
        ├── main/java/com/flowable/app/App.java   # Bootstrap del ProcessEngine Standalone (H2)
        └── test/java/com/flowable/app/AppTest.java  # Pruebas unitarias
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

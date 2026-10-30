# Gestión de Cursos de Capacitación - GTPN

Este repositorio contiene el desarrollo del proyecto final para la asignatura **Gestión y Tecnologías de Procesos de Negocio**.

## 📋 Resumen del Proyecto

El objetivo principal es implementar y automatizar el proceso de negocio de una institución dedicada a organizar cursos de capacitación para empresas. Tomando como base un modelo **BPMN 2.0** y utilizando el motor de procesos **Flowable**, el sistema orquesta las siguientes etapas:

1. **Viabilidad y Acuerdo:** Evaluación automática de solicitudes según reglas de negocio (límite de cursos activos), registro de propuestas, envío a la empresa, iteración de modificaciones y control de tiempos (recordatorios y archivo por falta de respuesta).
2. **Preparación del Curso:** Subproceso paralelo donde se confecciona la lista de inscriptos, se preparan materiales didácticos y se gestionan los recursos necesarios (aulas físicas, virtuales o equipamiento especial).
3. **Desarrollo y Finalización:** Asignación de docentes a través de aplicaciones externas, registro del dictado, elaboración de informes de resultados y posterior liberación compensatoria de recursos.
4. **Manejo de Excepciones:** Flujos de cancelación durante la preparación y manejo de errores, con sus respectivas tareas de compensación.

El ecosistema se completará integrando el motor de Flowable con un sitio web, aplicaciones de escritorio y una app móvil mediante APIs REST y Java.

## 📂 Archivos del Repositorio

Actualmente, el repositorio cuenta con los siguientes artefactos y documentos:

- `ProyectoCursos.pdf`: Documento oficial de la cátedra con el enunciado detallado, responsabilidades, reglas de negocio y requisitos técnicos de implementación.
- `cursosOriginal.bpmn`: Modelo BPMN conceptual base provisto por la cátedra como referencia inicial.
- `cursosNuestraVersion.bpmn`: Modelo BPMN refinado y adaptado por el equipo, aplicando las guías de modelado 7PMG, buenas prácticas de BPMN 2.0 y ajustes orientados a su futura ejecución en Flowable.
- `justificacion_cambios_bpmn.md`: Informe técnico donde se detallan y justifican las decisiones de diseño y las modificaciones realizadas sobre el modelo original, junto con las consultas planteadas a la cátedra.

## 👥 Equipo de Trabajo

- **Materia:** Gestión y Tecnologías de Procesos de Negocio
- **Profesor:** Dr. Ariel Gonzalez
- **Integrantes:**
  - Agustin Alieni
  - Agustin Cesari
  - Damian Dalio
  - Hernan Jara
  - Fran Natale

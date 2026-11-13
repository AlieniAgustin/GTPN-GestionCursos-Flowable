package com.comunicaciones.app.service;

/**
 * Una solicitud sobre la que se puede actuar: la instancia del proceso más los datos que
 * la identifican para el usuario.
 *
 * @param processInstanceId instancia en el motor
 * @param businessKey       id del curso en la BD de la institución, si la solicitud entró por el BFF
 * @param courseName        nombre del curso, de las variables de la instancia
 * @param companyName       empresa solicitante, de las variables de la instancia
 * @param startTime         cuándo se inició, como lo devuelve la API REST
 */
public record CourseRequest(
        String processInstanceId,
        String businessKey,
        String courseName,
        String companyName,
        String startTime
) {

    /** Etiqueta corta para los títulos de los diálogos. */
    public String describe() {
        if (!courseName.isBlank() && !companyName.isBlank()) {
            return courseName + " — " + companyName;
        }
        if (!courseName.isBlank()) {
            return courseName;
        }
        return "Instancia " + processInstanceId;
    }
}

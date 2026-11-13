package com.flowable.app.listeners;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.dto.CourseDetailsUpdateDTO;

/**
 * Vuelca a la BD de la institución los datos que el proceso va produciendo
 * (enunciado 3.2): el programa y el presupuesto de la propuesta, la fecha de inicio
 * acordada y el informe de resultados.
 *
 * <p>El proceso los genera en etapas distintas, así que el listener se engancha en
 * varios puntos del modelo y en cada uno manda lo que ya tenga: el BFF solo pisa los
 * campos que llegan con valor, de modo que llamarlo de más es inofensivo.
 *
 * <p>Va por el motor y no por el BFF a propósito: varias de esas tareas el enunciado
 * permite completarlas desde la interfaz de Flowable, sin pasar por el sitio web. Así
 * la base queda sincronizada se complete por donde se complete.
 *
 * <p>Si el BFF no responde, la instancia sigue su curso y queda el aviso en el log: la
 * base de la institución es un registro paralelo, no una condición para avanzar.
 */
@Component("syncCourseDetails")
public class SyncCourseDetailsListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    public static final String VAR_PROGRAM = "programa";
    public static final String VAR_BUDGET = "presupuesto";
    public static final String VAR_COURSE_DATE = "fechaCurso";
    public static final String VAR_REPORT = "informe";

    private static final String DETAILS_PATH = "/api/flowable/courses/{processId}/details";

    private final RestTemplate backendRestTemplate;

    public SyncCourseDetailsListener(@Qualifier("backendRestTemplate") RestTemplate backendRestTemplate) {
        this.backendRestTemplate = backendRestTemplate;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] sync course details");

        String processInstanceId = execution.getProcessInstanceId();

        CourseDetailsUpdateDTO details = new CourseDetailsUpdateDTO(
                asText(execution.getVariable(VAR_PROGRAM)),
                asAmount(execution.getVariable(VAR_BUDGET), processInstanceId),
                asDate(execution.getVariable(VAR_COURSE_DATE)),
                asText(execution.getVariable(VAR_REPORT))
        );

        if (isEmpty(details)) {
            System.out.println("[INFO] Instancia: " + processInstanceId
                    + " | Todavía no hay datos del curso para sincronizar");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            backendRestTemplate.exchange(
                    DETAILS_PATH,
                    HttpMethod.PUT,
                    new HttpEntity<>(details, headers),
                    Void.class,
                    processInstanceId);
            System.out.println("[INFO] Instancia: " + processInstanceId
                    + " | Datos del curso sincronizados con la BD de la institución");
        } catch (Exception e) {
            System.err.println("[ERROR] Instancia: " + processInstanceId
                    + " | No se pudieron sincronizar los datos del curso: " + e.getMessage());
        }
    }

    private boolean isEmpty(CourseDetailsUpdateDTO details) {
        return details.program() == null
                && details.budget() == null
                && details.agreedDate() == null
                && details.report() == null;
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * El presupuesto puede venir como número desde un formulario o como texto desde la
     * API REST. Si no se puede interpretar, se avisa y se omite antes que abortar.
     */
    private BigDecimal asAmount(Object value, String processInstanceId) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal amount) {
            return amount;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            System.out.println("[WARN] Instancia: " + processInstanceId
                    + " | El presupuesto '" + text + "' no es un número; no se sincroniza");
            return null;
        }
    }

    /**
     * {@code fechaCurso} la deja normalizada a ISO-8601 con offset el
     * {@link ResolveCourseDateListener}; a la base va solo la fecha.
     */
    private String asDate(Object value) {
        String text = asText(value);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toLocalDate().toString();
        } catch (DateTimeParseException ignored) {
            // puede llegar como fecha sola si alguien la seteó a mano
        }
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text).toString();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}

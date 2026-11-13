package com.flowable.app.listeners;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deja definida la fecha de inicio del curso al aceptarse la propuesta (enunciado 3.5).
 *
 * <p>El timer "Fecha programada" usa {@code <timeDate>${fechaCurso}</timeDate>}, así que
 * la variable tiene que existir y ser parseable **antes** de alcanzar el evento. La fecha
 * la acuerda la empresa: el mensaje de aceptación la trae en {@code fechaCurso} (o en
 * {@code fechaAcordada}, el nombre que usa la app de comunicaciones del enunciado 4.2.2).
 *
 * <p>Este listener corre al tomar el flujo que sale de "Aceptación" y:
 * <ul>
 *   <li>normaliza lo que haya llegado a ISO-8601 con offset, que es lo que entiende el
 *       timer, acepte {@code Date}, {@code 2026-03-01}, {@code 2026-03-01T09:00} o
 *       {@code 2026-03-01T09:00-03:00};</li>
 *   <li>si no llegó nada usable, la deriva de {@code proceso.fecha-curso-offset} contado
 *       desde ahora, y lo avisa en el log. Así la instancia nunca queda trabada con
 *       {@code Unknown property used in expression: ${fechaCurso}}.</li>
 * </ul>
 */
@Component("resolveCourseDate")
public class ResolveCourseDateListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    public static final String VAR_COURSE_DATE = "fechaCurso";
    public static final String VAR_AGREED_DATE = "fechaAcordada";

    private final String defaultOffset;

    public ResolveCourseDateListener(@Value("${proceso.fecha-curso-offset:P7D}") String defaultOffset) {
        this.defaultOffset = defaultOffset;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] resolve course date");

        String processInstanceId = execution.getProcessInstanceId();

        Object agreed = execution.getVariable(VAR_COURSE_DATE);
        if (isEmpty(agreed)) {
            agreed = execution.getVariable(VAR_AGREED_DATE);
        }

        String courseDate = normalize(agreed);

        if (courseDate == null) {
            if (!isEmpty(agreed)) {
                System.out.println("[WARN] Instancia: " + processInstanceId
                        + " | La fecha acordada '" + agreed + "' no se pudo interpretar; se usa la de por defecto");
            } else {
                System.out.println("[WARN] Instancia: " + processInstanceId
                        + " | La aceptación no trajo la fecha acordada; se usa proceso.fecha-curso-offset="
                        + defaultOffset);
            }
            courseDate = OffsetDateTime.now().plus(parseOffset()).toString();
        }

        execution.setVariable(VAR_COURSE_DATE, courseDate);
        System.out.println("[INFO] Instancia: " + processInstanceId
                + " | Fecha de inicio del curso: " + courseDate);
    }

    private Duration parseOffset() {
        try {
            return Duration.parse(defaultOffset);
        } catch (DateTimeParseException e) {
            System.err.println("[ERROR] proceso.fecha-curso-offset='" + defaultOffset
                    + "' no es una duración ISO-8601 de días/horas/minutos (P7D, PT2M); se usan 7 días");
            return Duration.ofDays(7);
        }
    }

    /**
     * Lleva la fecha acordada a ISO-8601 con offset. Devuelve null si no se puede.
     */
    private String normalize(Object value) {
        if (isEmpty(value)) {
            return null;
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().toString();
        }

        String text = value.toString().trim();
        try {
            return OffsetDateTime.parse(text).toString();
        } catch (DateTimeParseException ignored) {
            // sigue con los formatos sin offset
        }
        try {
            return LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
        } catch (DateTimeParseException ignored) {
            // sigue con la fecha sola
        }
        try {
            return LocalDate.parse(text).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().toString();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private boolean isEmpty(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }
}

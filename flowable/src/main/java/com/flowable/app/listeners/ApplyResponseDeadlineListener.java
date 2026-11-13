package com.flowable.app.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fija el plazo de espera de la respuesta de la empresa (enunciado 3.5).
 *
 * <p>Corre al entrar a "Enviar propuesta". El timer del gateway usa
 * {@code <timeDuration>${plazoRespuesta}</timeDuration>}, así que el plazo es una
 * variable del proceso y no una constante del modelo: para la demo se baja a minutos
 * con {@code proceso.plazo-respuesta} (o la variable de entorno
 * {@code PROCESO_PLAZO_RESPUESTA}) sin tocar el BPMN.
 *
 * <p>Si la instancia ya trae {@code plazoRespuesta} (porque la inició una app con su
 * propio plazo), se respeta: el valor por defecto solo se aplica cuando falta.
 */
@Component("applyResponseDeadline")
public class ApplyResponseDeadlineListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    public static final String VAR_RESPONSE_DEADLINE = "plazoRespuesta";

    private final String defaultDeadline;

    public ApplyResponseDeadlineListener(@Value("${proceso.plazo-respuesta:P5D}") String defaultDeadline) {
        this.defaultDeadline = defaultDeadline;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] apply response deadline");

        Object current = execution.getVariable(VAR_RESPONSE_DEADLINE);
        boolean missing = current == null || (current instanceof String text && text.isBlank());

        if (missing) {
            execution.setVariable(VAR_RESPONSE_DEADLINE, defaultDeadline);
            System.out.println("[INFO] Instancia: " + execution.getProcessInstanceId()
                    + " | Plazo de respuesta: " + defaultDeadline + " (proceso.plazo-respuesta)");
        } else {
            System.out.println("[INFO] Instancia: " + execution.getProcessInstanceId()
                    + " | Plazo de respuesta ya definido en la instancia: " + current);
        }
    }
}

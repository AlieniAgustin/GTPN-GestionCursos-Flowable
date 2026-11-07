package com.flowable.app.listeners;

import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.stereotype.Component;

/**
 * Execution listener del fin interno de "Preparar curso" (SubFin), evento start.
 *
 * Flowable 7 no cierra un subproceso que tiene a la vez un evento de borde
 * (el error de cancelación) y un subproceso de evento (Recibir Cancelación):
 * la ejecución que espera el mensaje de cancelación queda viva y el subproceso
 * nunca termina. Este listener cierra esa espera cuando la preparación ya terminó,
 * así el proceso sigue a "Enviar confirmación".
 */
@Component("closeCancellation")
public class CloseCancellationListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] close cancellation");

        ExecutionEntity current = (ExecutionEntity) execution;
        ExecutionEntity subProcessScope = current.getParent();
        if (subProcessScope == null) {
            return;
        }

        ExecutionEntityManager executionManager = CommandContextUtil.getExecutionEntityManager();

        for (ExecutionEntity child : executionManager.findChildExecutionsByParentExecutionId(subProcessScope.getId())) {
            FlowElement element = child.getCurrentFlowElement();
            boolean waitsForCancellation = child.isEventScope()
                    && element instanceof StartEvent
                    && element.getSubProcess() instanceof EventSubProcess;

            if (waitsForCancellation) {
                executionManager.deleteExecutionAndRelatedData(child, "Preparación completa", false);
                System.out.println("[INFO] Instancia: " + execution.getProcessInstanceId()
                        + " | Preparación completa: ya no se escucha la cancelación (" + element.getId() + ")");
            }
        }
    }
}

package com.flowable.app.services;

import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.springframework.stereotype.Service;

/**
 * Plomería de ciclo de vida que no debería ver un listener.
 *
 * <p>Flowable 7 no cierra un subproceso que tiene a la vez un evento de borde y un
 * subproceso de evento: la ejecución que espera el mensaje queda viva y el subproceso
 * nunca termina. Acá se podan esas esperas huérfanas.
 */
@Service
public class ProcessLifecycleService {

    private final ProcessEngineConfigurationImpl processEngineConfiguration;

    public ProcessLifecycleService(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    /**
     * Borra las ejecuciones que esperan un evento de inicio dentro de subprocesos de
     * evento hermanos de la ejecución indicada.
     */
    public void cleanupEventSubProcessExecutions(String executionId, String reason) {
        ExecutionEntityManager executionManager = processEngineConfiguration.getExecutionEntityManager();
        ExecutionEntity current = executionManager.findById(executionId);
        if (current == null) {
            return;
        }

        ExecutionEntity subProcessScope = current.getParent();
        if (subProcessScope == null) {
            return;
        }

        for (ExecutionEntity child : executionManager.findChildExecutionsByParentExecutionId(subProcessScope.getId())) {
            FlowElement element = child.getCurrentFlowElement();
            boolean waitsForCancellation = child.isEventScope()
                    && element instanceof StartEvent
                    && element.getSubProcess() instanceof EventSubProcess;

            if (waitsForCancellation) {
                executionManager.deleteExecutionAndRelatedData(child, reason, false);
                System.out.println("[INFO] Ejecución: " + executionId
                        + " | " + reason + ": ya no se escucha la cancelación (" + element.getId() + ")");
            }
        }
    }
}

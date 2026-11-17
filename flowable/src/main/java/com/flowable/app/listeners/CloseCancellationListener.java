package com.flowable.app.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import com.flowable.app.services.ProcessLifecycleService;

/**
 * Execution listener del fin interno de "Preparar curso" (SubFin), evento start.
 *
 * <p>Delega en {@link ProcessLifecycleService} el cierre de la espera de cancelación
 * que Flowable 7 deja viva: sin eso el subproceso nunca termina y el proceso no sigue
 * a "Enviar confirmación".
 */
@Component("closeCancellation")
public class CloseCancellationListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    private final ProcessLifecycleService processLifecycleService;

    public CloseCancellationListener(ProcessLifecycleService processLifecycleService) {
        this.processLifecycleService = processLifecycleService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] close cancellation");
        processLifecycleService.cleanupEventSubProcessExecutions(execution.getId(), "Preparación completa");
    }
}

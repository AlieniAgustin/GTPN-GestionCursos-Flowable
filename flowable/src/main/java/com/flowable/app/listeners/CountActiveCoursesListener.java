package com.flowable.app.listeners;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.stereotype.Component;

@Component("countActiveCourses")
public class CountActiveCoursesListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    // Clave del proceso: incluye todas sus versiones desplegadas
    public static final String PROCESS_KEY = "Process_Curso";

    // Variable que recibe la tabla de decisión de viabilidad
    public static final String COUNT_VARIABLE = "cantidadCursosActivos";

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("***** EXECUTION LISTENER CountActiveCourses *****");

        // Se reutiliza el motor que está ejecutando el listener
        RuntimeService runtimeService =
                CommandContextUtil.getProcessEngineConfiguration().getRuntimeService();

        String ownInstanceId = execution.getProcessInstanceId();

        // Instancias no finalizadas del proceso (en acuerdo o en preparación),
        // excluyendo la que está evaluando su propia viabilidad
        long activeCourses = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .list()
                .stream()
                .filter(instance -> !instance.getId().equals(ownInstanceId))
                .count();

        execution.setVariable(COUNT_VARIABLE, activeCourses);

        System.out.println("Instancia: " + ownInstanceId
                + " | Otros cursos activos: " + activeCourses);
        System.out.println("*************************************");
    }
}

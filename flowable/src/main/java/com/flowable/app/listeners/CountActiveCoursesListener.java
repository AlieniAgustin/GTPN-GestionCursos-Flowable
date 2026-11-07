package com.flowable.app.listeners;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.stereotype.Component;

@Component("countActiveCourses")
public class CountActiveCoursesListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;
    public static final String PROCESS_KEY = "Process_Curso";
    public static final String COUNT_VARIABLE = "cantidadCursosActivos";
    public static final String ACTIVE_COURSE_VARIABLE = "cursoActivo";

    @Override
    public void notify(DelegateExecution execution) {
        RuntimeService runtimeService =
                CommandContextUtil.getProcessEngineConfiguration().getRuntimeService();
              System.out.println("[LISTENER] active courses counter");

        String ownInstanceId = execution.getProcessInstanceId();

        long activeCourses = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .variableValueEquals(ACTIVE_COURSE_VARIABLE, true)
                .count();

        execution.setVariable(COUNT_VARIABLE, activeCourses);

      System.out.println("[INFO] Instancia: " + ownInstanceId + " | Otros cursos activos: " + activeCourses);
    }
}

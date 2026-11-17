package com.flowable.app.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import com.flowable.app.services.CourseProcessService;

@Component("countActiveCourses")
public class CountActiveCoursesListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;
    public static final String COUNT_VARIABLE = "cantidadCursosActivos";

    private final CourseProcessService courseProcessService;

    public CountActiveCoursesListener(CourseProcessService courseProcessService) {
        this.courseProcessService = courseProcessService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] active courses counter");

        long activeCourses = courseProcessService.countActiveCourses();
        execution.setVariable(COUNT_VARIABLE, activeCourses);

        System.out.println("[INFO] Instancia: " + execution.getProcessInstanceId()
                + " | Otros cursos activos: " + activeCourses);
    }
}

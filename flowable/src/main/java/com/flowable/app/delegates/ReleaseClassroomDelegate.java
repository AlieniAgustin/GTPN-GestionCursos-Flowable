package com.flowable.app.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.flowable.app.services.ClassroomService;

/**
 * Compensación "Liberar aula". Devuelve el aula reservada al web service simulado.
 */
@Component("releaseClassroom")
public class ReleaseClassroomDelegate implements JavaDelegate {

    private final ClassroomService classroomService;

    public ReleaseClassroomDelegate(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String classroom = (String) execution.getVariable(ReserveClassroomDelegate.CLASSROOM_VARIABLE);
        classroomService.release(classroom);
        execution.setVariable("aulaLiberada", true);
        System.out.println("Aula liberada: " + classroom);
    }
}

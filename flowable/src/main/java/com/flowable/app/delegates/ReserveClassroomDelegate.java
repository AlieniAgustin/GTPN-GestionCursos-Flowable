package com.flowable.app.delegates;

import java.util.Optional;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.flowable.app.services.ClassroomService;

/**
 * Service task "Reservar aula". Pide un aula al web service simulado.
 * Si no hay aulas libres lanza el error de BPMN errSinAulas, que captura
 * el evento de borde de la tarea.
 */
@Component("reserveClassroom")
public class ReserveClassroomDelegate implements JavaDelegate {

    public static final String NO_CLASSROOM_ERROR = "errSinAulas";
    public static final String CLASSROOM_VARIABLE = "aulaAsignada";

    private final ClassroomService classroomService;

    public ReserveClassroomDelegate(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Optional<String> classroom = classroomService.reserve(execution.getProcessInstanceId());

        if (classroom.isEmpty()) {
            System.out.println("Reservar aula: no hay aulas disponibles (instancia "
                    + execution.getProcessInstanceId() + ")");
            throw new BpmnError(NO_CLASSROOM_ERROR, "No hay aulas disponibles");
        }

        execution.setVariable(CLASSROOM_VARIABLE, classroom.get());
        System.out.println("Aula reservada: " + classroom.get());
    }
}

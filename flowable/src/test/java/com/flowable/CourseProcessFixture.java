package com.flowable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * Test fixture driver to navigate key milestones in the Course Management BPMN process.
 */
@Component
public class CourseProcessFixture {

    public static final String MESSAGE_COURSE_REQUEST = "Solicitud de curso";
    public static final String MESSAGE_ACCEPTANCE = "Aceptación";
    public static final String MESSAGE_REJECTION = "Rechazo";
    public static final String MESSAGE_MODIFICATIONS = "Solicitud de modificaciones";
    public static final String MESSAGE_PARTICIPANTS = "Nómina de participantes";

    public static final String TASK_REGISTER_PROPOSAL = "registraryelaborarpropuesta";
    public static final String TASK_ARCHIVE_REQUEST = "archivarsolicitud";
    public static final String GATEWAY_WAITING_FOR_RESPONSE = "esperarrespuesta";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ManagementService managementService;

    public CourseProcessFixture(RuntimeService runtimeService, TaskService taskService,
                                ManagementService managementService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.managementService = managementService;
    }

    /**
     * Starts a new process instance triggered by the course request message.
     */
    public ProcessInstance startProcessInstance() {
        return startProcessInstance(Collections.emptyMap());
    }

    /**
     * Starts a new process instance triggered by the course request message with given variables.
     */
    public ProcessInstance startProcessInstance(Map<String, Object> variables) {
        return runtimeService.startProcessInstanceByMessage(MESSAGE_COURSE_REQUEST, variables);
    }

    /**
     * Finds and completes the proposal registration task for the given process instance.
     */
    public Task completeProposal(String processInstanceId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(TASK_REGISTER_PROPOSAL)
                .singleResult();

        if (task != null) {
            if (variables != null && !variables.isEmpty()) {
                taskService.complete(task.getId(), variables);
            } else {
                taskService.complete(task.getId());
            }
        }
        return task;
    }

    /**
     * Triggers the proposal acceptance intermediate message event.
     */
    public Execution triggerAcceptance(String processInstanceId) {
        return triggerAcceptance(processInstanceId, Collections.emptyMap());
    }

    /**
     * Triggers the proposal acceptance intermediate message event carrying the data the
     * company sends with it, such as the agreed course date ('fechaCurso').
     */
    public Execution triggerAcceptance(String processInstanceId, Map<String, Object> variables) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName(MESSAGE_ACCEPTANCE)
                .singleResult();

        if (execution != null) {
            if (variables != null && !variables.isEmpty()) {
                runtimeService.messageEventReceived(MESSAGE_ACCEPTANCE, execution.getId(), variables);
            } else {
                runtimeService.messageEventReceived(MESSAGE_ACCEPTANCE, execution.getId());
            }
        }
        return execution;
    }

    /**
     * Triggers the proposal rejection intermediate message event.
     */
    public Execution triggerRejection(String processInstanceId) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName(MESSAGE_REJECTION)
                .singleResult();

        if (execution != null) {
            runtimeService.messageEventReceived(MESSAGE_REJECTION, execution.getId());
        }
        return execution;
    }

    /**
     * Triggers the proposal modifications intermediate message event.
     */
    public Execution triggerModifications(String processInstanceId) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName(MESSAGE_MODIFICATIONS)
                .singleResult();

        if (execution != null) {
            runtimeService.messageEventReceived(MESSAGE_MODIFICATIONS, execution.getId());
        }
        return execution;
    }

    /**
     * Advances the process to the proposal submission stage and completes it with the given proposal variables,
     * leaving the instance waiting at the event-based gateway ('esperarrespuesta').
     *
     * @param proposalVariables variables to provide when completing the proposal task (e.g. "propuesta", details)
     * @return the ProcessInstance currently waiting at the event gateway
     */
    public ProcessInstance advanceToWaitingForProposalResponse(Map<String, Object> proposalVariables) {
        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("viable", true);
        return advanceToWaitingForProposalResponse(initialVariables, proposalVariables);
    }

    /**
     * Starts a request with initial variables, completes the proposal with the supplied variables map,
     * and leaves the instance waiting at the event-based gateway ('esperarrespuesta').
     *
     * @param initialVariables variables to start the process with
     * @param proposalVariables variables to attach to the proposal
     * @return the ProcessInstance currently waiting at the event gateway
     */
    public ProcessInstance advanceToWaitingForProposalResponse(Map<String, Object> initialVariables,
                                                              Map<String, Object> proposalVariables) {
        ProcessInstance instance = startProcessInstance(initialVariables);
        completeProposal(instance.getId(), proposalVariables);
        return instance;
    }

    /**
     * Composite milestone: starts a request, completes proposal and triggers acceptance,
     * advancing the instance until it reaches preparation status.
     */
    public ProcessInstance startCourseInPreparation() {
        Map<String, Object> defaultProposalVars = new HashMap<>();
        defaultProposalVars.put("propuesta", "Propuesta Inicial");
        ProcessInstance processInstance = advanceToWaitingForProposalResponse(defaultProposalVars);
        triggerAcceptance(processInstance.getId());
        return processInstance;
    }

    /**
     * Triggers the participant list message ('Nómina de participantes') inside the preparation subprocess.
     */
    public Execution triggerParticipantList(String processInstanceId) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName(MESSAGE_PARTICIPANTS)
                .singleResult();

        if (execution != null) {
            runtimeService.messageEventReceived(MESSAGE_PARTICIPANTS, execution.getId());
        }
        return execution;
    }

    /**
     * Fires the pending timer of the instance right away (the async executor is off in tests).
     * Returns the fired timer, or null if the instance was not waiting on a timer.
     */
    public Job fireTimer(String processInstanceId) {
        Job timer = managementService.createTimerJobQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (timer != null) {
            Job executable = managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(executable.getId());
        }
        return timer;
    }

    /**
     * Finds the pending task with the given BPMN id and completes it with the given variables.
     * Returns the completed task, or null if it did not exist.
     */
    public Task completeTask(String processInstanceId, String taskKey, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskKey)
                .singleResult();

        if (task != null) {
            taskService.complete(task.getId(), variables);
        }
        return task;
    }
}

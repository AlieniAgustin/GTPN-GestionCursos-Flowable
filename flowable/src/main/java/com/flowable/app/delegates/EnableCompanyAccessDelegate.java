package com.flowable.app.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.flowable.app.client.BackendClient;
import com.flowable.app.dto.CompanyAccessResponseDTO;

@Component("enableCompanyAccess")
public class EnableCompanyAccessDelegate implements JavaDelegate {

    public static final String VAR_ENABLED = "empresaAccesoHabilitado";
    public static final String VAR_USERNAME = "empresaUsuario";
    public static final String VAR_CREATED = "empresaAccesoCreado";

    private final BackendClient backendClient;

    public EnableCompanyAccessDelegate(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("[DELEGATE] enable company access");

        String processInstanceId = execution.getProcessInstanceId();

        CompanyAccessResponseDTO access = backendClient.enableCompanyAccess(processInstanceId);

        if (access == null || access.username() == null) {
            execution.setVariable(VAR_ENABLED, false);
            System.out.println("[WARN] Instancia: " + processInstanceId
                    + " | El backend no devolvió una cuenta para la empresa: el acceso queda sin habilitar");
            return;
        }

        execution.setVariable(VAR_ENABLED, true);
        execution.setVariable(VAR_USERNAME, access.username());
        execution.setVariable(VAR_CREATED, access.created());

        if (access.created()) {
            System.out.println("[INFO] Instancia: " + processInstanceId
                    + " | Cuenta creada para la empresa: usuario " + access.username());
        } else {
            System.out.println("[INFO] Instancia: " + processInstanceId
                    + " | Se reutiliza el usuario existente " + access.username());
        }
    }
}

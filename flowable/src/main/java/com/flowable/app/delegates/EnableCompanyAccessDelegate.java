package com.flowable.app.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.dto.CompanyAccessResponseDTO;

/**
 * Actividad automática "Habilitar acceso de la empresa" (enunciado 4.1.2).
 *
 * <p>Corre después de que la empresa aceptó la propuesta y antes de arrancar la
 * preparación. Le pide al BFF la cuenta de la empresa dueña del curso: la crea si
 * todavía no tiene una, o reutiliza la que quedó de una contratación anterior. El curso
 * nuevo queda vinculado a esa cuenta a través de su empresa en la BD de la institución.
 *
 * <p>El usuario queda en la variable {@code empresaUsuario} y, cuando la cuenta se
 * acaba de crear, la contraseña en {@code empresaPasswordTemporal}: el enunciado permite
 * simular la comunicación de las credenciales, así que alcanza con leerlas de las
 * variables de la instancia o del log del motor.
 *
 * <p>Si el BFF no responde, la instancia sigue igual con
 * {@code empresaAccesoHabilitado = false} en vez de quedar trabada; el log avisa.
 */
@Component("enableCompanyAccess")
public class EnableCompanyAccessDelegate implements JavaDelegate {

    public static final String VAR_ENABLED = "empresaAccesoHabilitado";
    public static final String VAR_USERNAME = "empresaUsuario";
    public static final String VAR_PASSWORD = "empresaPasswordTemporal";
    public static final String VAR_CREATED = "empresaAccesoCreado";
    public static final String VAR_COMPANY_ID = "empresaId";

    private static final String ACCESS_PATH = "/api/flowable/courses/{processId}/company-access";

    private final RestTemplate backendRestTemplate;

    public EnableCompanyAccessDelegate(@Qualifier("backendRestTemplate") RestTemplate backendRestTemplate) {
        this.backendRestTemplate = backendRestTemplate;
    }

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("[DELEGATE] enable company access");

        String processInstanceId = execution.getProcessInstanceId();

        try {
            ResponseEntity<CompanyAccessResponseDTO> response = backendRestTemplate.postForEntity(
                    ACCESS_PATH, null, CompanyAccessResponseDTO.class, processInstanceId);
            CompanyAccessResponseDTO access = response != null ? response.getBody() : null;

            if (access == null || access.username() == null) {
                execution.setVariable(VAR_ENABLED, false);
                System.out.println("[WARN] Instancia: " + processInstanceId
                        + " | El backend no devolvió una cuenta para la empresa: el acceso queda sin habilitar");
                return;
            }

            execution.setVariable(VAR_ENABLED, true);
            execution.setVariable(VAR_USERNAME, access.username());
            execution.setVariable(VAR_CREATED, access.created());
            if (access.companyId() != null) {
                execution.setVariable(VAR_COMPANY_ID, access.companyId());
            }

            if (access.created()) {
                // Comunicación simulada de las credenciales (enunciado 4.1.2)
                execution.setVariable(VAR_PASSWORD, access.temporaryPassword());
                System.out.println("[INFO] Instancia: " + processInstanceId
                        + " | Cuenta creada para '" + access.companyName() + "': usuario " + access.username());
            } else {
                System.out.println("[INFO] Instancia: " + processInstanceId
                        + " | '" + access.companyName() + "' ya tenía cuenta: se reutiliza el usuario "
                        + access.username());
            }
        } catch (Exception e) {
            execution.setVariable(VAR_ENABLED, false);
            System.err.println("[ERROR] Instancia: " + processInstanceId
                    + " | No se pudo habilitar el acceso de la empresa: " + e.getMessage());
        }
    }
}

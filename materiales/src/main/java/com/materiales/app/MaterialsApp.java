package com.materiales.app;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.flowable.engine.ProcessEngine;

import com.materiales.app.analysis.ExecutionAnalysisService;
import com.materiales.app.engine.EngineConfig;
import com.materiales.app.engine.EngineFactory;
import com.materiales.app.service.MaterialsTaskService;
import com.materiales.app.service.StaffAuthenticator;
import com.materiales.app.service.StaffUser;
import com.materiales.app.ui.LoginDialog;
import com.materiales.app.ui.MaterialsWindow;

/**
 * App auxiliar de preparación de materiales (enunciado 4.2.1).
 *
 * <p>De escritorio, local a la intranet y con alcance a la base del motor: arma un
 * {@link ProcessEngine} embebido y trabaja con la <b>API Java</b> de Flowable
 * ({@code IdentityService}, {@code TaskService}), sin pasar por la API REST ni por el BFF.
 *
 * <p>Incluye además el análisis de las ejecuciones del enunciado 4.3, en el botón
 * "Análisis de ejecuciones"; por consola está en
 * {@link com.materiales.app.analysis.AnalysisApp}.
 *
 * <pre>
 * java -jar target/app.jar
 * java -Ddb.url=jdbc:postgresql://localhost:5432/flowable -jar target/app.jar
 * </pre>
 */
public final class MaterialsApp {

    private MaterialsApp() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("La app necesita entorno gráfico: es de escritorio, local a la intranet.");
            System.exit(1);
        }

        EngineConfig config = EngineConfig.load();
        System.out.println("Conectando a la base del motor: " + config.describe());

        ProcessEngine processEngine;
        try {
            processEngine = EngineFactory.create(config);
        } catch (RuntimeException e) {
            // Típico: la base no está levantada, o el esquema es de otra versión del motor
            String message = "No se pudo conectar al motor (" + config.describe() + "): " + e.getMessage();
            System.err.println(message);
            JOptionPane.showMessageDialog(null, message, "Preparación de materiales",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(processEngine::close));

        StaffAuthenticator authenticator = new StaffAuthenticator(processEngine.getIdentityService());
        MaterialsTaskService materialsTaskService =
                new MaterialsTaskService(processEngine.getTaskService());
        ExecutionAnalysisService analysisService = new ExecutionAnalysisService(
                processEngine.getHistoryService(), processEngine.getIdentityService());

        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();

            StaffUser user = new LoginDialog(null, authenticator).showAndGetUser();
            if (user == null) {
                System.exit(0);
                return;
            }
            new MaterialsWindow(materialsTaskService, analysisService, user).setVisible(true);
        });
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedOperationException
                | javax.swing.UnsupportedLookAndFeelException e) {
            // Con el look & feel por defecto alcanza
        }
    }
}

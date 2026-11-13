package com.materiales.app.analysis;

import org.flowable.engine.ProcessEngine;

import com.materiales.app.engine.EngineConfig;
import com.materiales.app.engine.EngineFactory;

/**
 * Análisis de las ejecuciones por consola (enunciado 4.3).
 *
 * <p>El enunciado permite incorporar estas consultas a la app de preparación de materiales
 * o ejecutarlas por consola. Están las dos: la ventana de la app tiene un botón que muestra
 * este mismo informe, y acá está la versión de terminal, que es la que conviene para la
 * captura de resultados.
 *
 * <pre>
 * java -cp target/app.jar com.materiales.app.analysis.AnalysisApp
 * java -Ddb.url=jdbc:postgresql://localhost:5432/flowable -cp target/app.jar com.materiales.app.analysis.AnalysisApp
 * </pre>
 */
public final class AnalysisApp {

    private AnalysisApp() {
    }

    public static void main(String[] args) {
        EngineConfig config = EngineConfig.load();

        ProcessEngine processEngine;
        try {
            processEngine = EngineFactory.create(config);
        } catch (RuntimeException e) {
            System.err.println("No se pudo conectar al motor (" + config.describe() + "): " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            ExecutionAnalysisService analysis = new ExecutionAnalysisService(
                    processEngine.getHistoryService(), processEngine.getIdentityService());

            System.out.println();
            System.out.println("=== Análisis de las ejecuciones del proceso (§4.3) ===");
            System.out.println("Motor: " + config.describe());
            System.out.println("Proceso: " + ExecutionAnalysisService.PROCESS_KEY);
            System.out.println();
            System.out.println(AnalysisFormatter.format(
                    analysis.analyzeResults(),
                    analysis.analyzePreparation(),
                    analysis.analyzeTaskDistribution()));
        } finally {
            processEngine.close();
        }
    }
}

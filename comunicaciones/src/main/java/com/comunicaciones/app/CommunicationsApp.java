package com.comunicaciones.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.comunicaciones.app.client.FlowableRestClient;
import com.comunicaciones.app.client.RestConfig;
import com.comunicaciones.app.service.CommunicationsService;
import com.comunicaciones.app.ui.CommunicationsWindow;

/**
 * App auxiliar de simulación de las comunicaciones de la empresa (enunciado 4.2.2).
 *
 * <p>Permite elegir una solicitud y enviarle aceptación (con la fecha acordada), rechazo,
 * solicitud de modificaciones (con las observaciones), la nómina de participantes o la
 * cancelación. Todo por la <b>API REST</b> del motor: no usa la API Java ni el BFF.
 *
 * <p>Las comunicaciones que se ofrecen son solo las habilitadas en el estado actual de la
 * instancia, y eso no se deduce con reglas propias: se le pregunta al motor qué ejecuciones
 * están suscriptas a cada mensaje.
 *
 * <pre>
 * java -jar target/app.jar
 * java -Dflowable.base-url=http://localhost:8080/process-api -jar target/app.jar
 * </pre>
 */
public final class CommunicationsApp {

    private CommunicationsApp() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("La app necesita entorno gráfico.");
            System.exit(1);
        }

        RestConfig config = RestConfig.load();
        System.out.println("API REST del motor: " + config.describe());

        CommunicationsService communicationsService =
                new CommunicationsService(new FlowableRestClient(config));

        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            new CommunicationsWindow(communicationsService, config.describe()).setVisible(true);
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

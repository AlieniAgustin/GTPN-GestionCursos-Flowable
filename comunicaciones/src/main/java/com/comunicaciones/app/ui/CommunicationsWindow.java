package com.comunicaciones.app.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import com.comunicaciones.app.service.Communication;
import com.comunicaciones.app.service.CommunicationsService;
import com.comunicaciones.app.service.CourseRequest;
import com.comunicaciones.app.service.EnabledCommunication;

/**
 * Ventana única: arriba las solicitudes, abajo las comunicaciones habilitadas para la que
 * esté seleccionada (enunciado 4.2.2).
 */
public class CommunicationsWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CommunicationsService communicationsService;
    private final RequestTableModel tableModel = new RequestTableModel();
    private final JTable table = new JTable(tableModel);
    private final JPanel actionsPanel = new JPanel();
    private final JLabel actionsTitle = new JLabel("Seleccioná una solicitud");
    private final JLabel statusLabel = new JLabel(" ");

    public CommunicationsWindow(CommunicationsService communicationsService, String connectionSummary) {
        super("Comunicaciones de la empresa — simulador");
        this.communicationsService = communicationsService;

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                reloadEnabledCommunications();
            }
        });

        JLabel header = new JLabel("Motor: " + connectionSummary);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton refreshButton = new JButton("Actualizar");
        refreshButton.addActionListener(event -> reloadRequests());

        JPanel north = new JPanel(new BorderLayout());
        north.add(header, BorderLayout.CENTER);
        north.add(refreshButton, BorderLayout.EAST);

        actionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        actionsTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createTitledBorder("Comunicaciones habilitadas"));
        actions.add(actionsTitle);
        actions.add(actionsPanel);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel south = new JPanel(new BorderLayout());
        south.add(actions, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 460));
        pack();
        setLocationRelativeTo(null);

        reloadRequests();
    }

    /** Vuelve a pedirle al motor las instancias en curso. */
    public final void reloadRequests() {
        List<CourseRequest> requests = call(communicationsService::findRequests, "No se pudieron listar las solicitudes");
        if (requests == null) {
            return;
        }
        tableModel.setRequests(requests);
        statusLabel.setText(requests.isEmpty()
                ? "No hay solicitudes en curso. Generá alguna con ./scripts/seed-demo.sh"
                : requests.size() + " solicitud(es) en curso");
        showNoSelection();
    }

    private void reloadEnabledCommunications() {
        CourseRequest request = selectedRequest();
        if (request == null) {
            showNoSelection();
            return;
        }

        List<EnabledCommunication> enabled = call(
                () -> communicationsService.findEnabled(request.processInstanceId()),
                "No se pudieron consultar las comunicaciones habilitadas");
        if (enabled == null) {
            return;
        }

        actionsPanel.removeAll();
        if (enabled.isEmpty()) {
            actionsTitle.setText(request.describe()
                    + ": en este estado la instancia no espera ninguna comunicación de la empresa");
        } else {
            actionsTitle.setText(request.describe() + ": esperando " + enabled.size() + " comunicación(es)");
            for (EnabledCommunication target : enabled) {
                JButton button = new JButton(target.communication().label());
                button.setToolTipText("Mensaje '" + target.communication().messageName()
                        + "' a la actividad '" + target.activityId() + "'");
                button.addActionListener(event -> sendCommunication(request, target));
                actionsPanel.add(button);
            }
        }
        actionsPanel.revalidate();
        actionsPanel.repaint();
    }

    private void sendCommunication(CourseRequest request, EnabledCommunication target) {
        Communication communication = target.communication();

        String input = null;
        if (communication.needsInput()) {
            input = askFor(communication, request);
            if (input == null) {
                return;
            }
        }

        try {
            communicationsService.send(target, input);
            statusLabel.setText("Enviado: " + communication.messageName() + " → " + request.describe());
        } catch (IllegalArgumentException e) {
            // Dato inválido: no se llegó a llamar al motor
            JOptionPane.showMessageDialog(this, e.getMessage(), "Revisá el dato",
                    JOptionPane.WARNING_MESSAGE);
            return;
        } catch (RuntimeException e) {
            showError("No se pudo enviar " + communication.messageName(), e);
            return;
        }

        // El estado cambió: las comunicaciones habilitadas ya no son las mismas
        reloadRequests();
    }

    /** Pide el dato de la comunicación; devuelve null si el usuario cancela. */
    private String askFor(Communication communication, CourseRequest request) {
        String title = communication.label() + " — " + request.describe();

        if (communication.inputKind() == Communication.InputKind.LIST) {
            JTextArea area = new JTextArea(6, 28);
            JPanel panel = new JPanel(new BorderLayout(0, 4));
            panel.add(new JLabel(communication.prompt()), BorderLayout.NORTH);
            panel.add(new JScrollPane(area), BorderLayout.CENTER);

            int option = JOptionPane.showConfirmDialog(this, panel, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            return option == JOptionPane.OK_OPTION ? area.getText() : null;
        }

        return JOptionPane.showInputDialog(this, communication.prompt(), title,
                JOptionPane.PLAIN_MESSAGE);
    }

    private void showNoSelection() {
        actionsPanel.removeAll();
        actionsTitle.setText("Seleccioná una solicitud");
        actionsPanel.revalidate();
        actionsPanel.repaint();
    }

    private CourseRequest selectedRequest() {
        return tableModel.requestAt(table.getSelectedRow());
    }

    /** Llama al motor con el cursor de espera; devuelve null si falló. */
    private <T> T call(Supplier<T> operation, String errorTitle) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            return operation.get();
        } catch (RuntimeException e) {
            showError(errorTitle, e);
            return null;
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void showError(String title, Exception error) {
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}

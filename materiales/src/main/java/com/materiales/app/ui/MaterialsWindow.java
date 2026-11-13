package com.materiales.app.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
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

import com.materiales.app.analysis.AnalysisFormatter;
import com.materiales.app.analysis.ExecutionAnalysisService;
import com.materiales.app.service.MaterialTask;
import com.materiales.app.service.MaterialsTaskService;
import com.materiales.app.service.StaffUser;

/**
 * Ventana principal: la bandeja de <i>Preparar materiales</i> del administrativo, con los
 * botones para reclamar y completar (enunciado 4.2.1).
 */
public class MaterialsWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final MaterialsTaskService materialsTaskService;
    private final ExecutionAnalysisService analysisService;
    private final StaffUser user;
    private final MaterialTaskTableModel tableModel;
    private final JTable table;
    private final JButton claimButton = new JButton("Reclamar");
    private final JButton completeButton = new JButton("Completar");
    private final JLabel statusLabel = new JLabel(" ");

    public MaterialsWindow(MaterialsTaskService materialsTaskService,
                           ExecutionAnalysisService analysisService,
                           StaffUser user) {
        super("Preparación de materiales — " + user.displayName());
        this.materialsTaskService = materialsTaskService;
        this.analysisService = analysisService;
        this.user = user;
        this.tableModel = new MaterialTaskTableModel(user.id());
        this.table = new JTable(tableModel);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> refreshButtons());

        JLabel header = new JLabel(user.displayName() + " (" + user.id() + ") — grupos: "
                + String.join(", ", user.groups()));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton refreshButton = new JButton("Actualizar");
        refreshButton.addActionListener(event -> reload());

        // Enunciado 4.3: las consultas pueden incorporarse a esta app
        JButton analysisButton = new JButton("Análisis de ejecuciones");
        analysisButton.addActionListener(event -> showAnalysis());

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerButtons.add(analysisButton);
        headerButtons.add(refreshButton);

        JPanel north = new JPanel(new BorderLayout());
        north.add(header, BorderLayout.CENTER);
        north.add(headerButtons, BorderLayout.EAST);

        claimButton.addActionListener(event -> claimSelected());
        completeButton.addActionListener(event -> completeSelected());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(claimButton);
        actions.add(completeButton);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.CENTER);
        south.add(actions, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 360));
        pack();
        setLocationRelativeTo(null);

        reload();
    }

    /** Vuelve a consultar la bandeja en el motor. */
    public final void reload() {
        try {
            List<MaterialTask> tasks = materialsTaskService.findInbox(user);
            tableModel.setTasks(tasks);
            statusLabel.setText(tasks.isEmpty()
                    ? "No hay tareas 'Preparar materiales' asignadas ni reclamables"
                    : tasks.size() + " tarea(s) en la bandeja");
        } catch (RuntimeException e) {
            showError("No se pudo consultar la bandeja", e);
        }
        refreshButtons();
    }

    private void claimSelected() {
        MaterialTask task = selectedTask();
        if (task == null) {
            return;
        }
        try {
            materialsTaskService.claim(task.id(), user);
            statusLabel.setText("Tarea reclamada");
        } catch (RuntimeException e) {
            // Típico: otro integrante del grupo la reclamó primero
            showError("No se pudo reclamar la tarea", e);
        }
        reload();
    }

    private void completeSelected() {
        MaterialTask task = selectedTask();
        if (task == null) {
            return;
        }
        String detail = JOptionPane.showInputDialog(this,
                "Detalle de los materiales preparados (opcional):",
                "Completar " + task.name(), JOptionPane.PLAIN_MESSAGE);
        if (detail == null) {
            return;
        }
        try {
            materialsTaskService.complete(task.id(), user, detail);
            statusLabel.setText("Tarea completada");
        } catch (RuntimeException e) {
            showError("No se pudo completar la tarea", e);
        }
        reload();
    }

    /** Muestra las tres consultas del enunciado 4.3 sobre el historial del motor. */
    private void showAnalysis() {
        String report;
        try {
            report = AnalysisFormatter.format(
                    analysisService.analyzeResults(),
                    analysisService.analyzePreparation(),
                    analysisService.analyzeTaskDistribution());
        } catch (RuntimeException e) {
            showError("No se pudo analizar el historial", e);
            return;
        }

        JTextArea area = new JTextArea(report, 28, 72);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);

        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Análisis de las ejecuciones (§4.3)", JOptionPane.PLAIN_MESSAGE);
    }

    private MaterialTask selectedTask() {
        return tableModel.taskAt(table.getSelectedRow());
    }

    private void refreshButtons() {
        MaterialTask task = selectedTask();
        claimButton.setEnabled(task != null && task.claimable());
        completeButton.setEnabled(task != null && task.assignedTo(user.id()));
    }

    private void showError(String title, Exception error) {
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}

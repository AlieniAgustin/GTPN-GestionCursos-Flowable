package com.materiales.app.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.materiales.app.service.MaterialTask;

/**
 * Tabla de la bandeja: una fila por tarea <i>Preparar materiales</i>.
 */
public class MaterialTaskTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Tarea", "Curso", "Empresa", "Estado", "Creada"};

    private final transient List<MaterialTask> tasks = new ArrayList<>();
    private final transient SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final String userId;

    public MaterialTaskTableModel(String userId) {
        this.userId = userId;
    }

    public void setTasks(List<MaterialTask> newTasks) {
        tasks.clear();
        tasks.addAll(newTasks);
        fireTableDataChanged();
    }

    public MaterialTask taskAt(int rowIndex) {
        return rowIndex < 0 || rowIndex >= tasks.size() ? null : tasks.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return tasks.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MaterialTask task = tasks.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> task.name();
            case 1 -> task.courseName();
            case 2 -> task.companyName();
            case 3 -> status(task);
            case 4 -> task.createTime() == null ? "" : dateFormat.format(task.createTime());
            default -> "";
        };
    }

    private String status(MaterialTask task) {
        if (task.claimable()) {
            return "Disponible para el grupo";
        }
        return task.assignedTo(userId) ? "Asignada a mí" : "Asignada a " + task.assignee();
    }
}

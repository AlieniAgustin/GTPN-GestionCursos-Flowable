package com.comunicaciones.app.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.comunicaciones.app.service.CourseRequest;

/**
 * Tabla de solicitudes: una fila por instancia en curso.
 */
public class RequestTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Curso", "Empresa", "Curso en BD", "Instancia", "Iniciada"};

    private final transient List<CourseRequest> requests = new ArrayList<>();

    public void setRequests(List<CourseRequest> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        fireTableDataChanged();
    }

    public CourseRequest requestAt(int rowIndex) {
        return rowIndex < 0 || rowIndex >= requests.size() ? null : requests.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return requests.size();
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
        CourseRequest request = requests.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> request.courseName();
            case 1 -> request.companyName();
            case 2 -> request.businessKey();
            case 3 -> request.processInstanceId();
            case 4 -> shortTime(request.startTime());
            default -> "";
        };
    }

    /** La API devuelve ISO-8601 con milisegundos y offset; alcanza con fecha y hora. */
    private String shortTime(String startTime) {
        if (startTime == null || startTime.length() < 16) {
            return startTime == null ? "" : startTime;
        }
        return startTime.substring(0, 10) + " " + startTime.substring(11, 16);
    }
}

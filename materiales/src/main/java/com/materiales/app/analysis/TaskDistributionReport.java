package com.materiales.app.analysis;

import java.util.List;

/**
 * Enunciado 4.3.3: cantidad de tareas de usuario completadas por cada administrativo y
 * docente. No se cuentan las pendientes ni las interrumpidas por una cancelación.
 *
 * @param completedTasks total de tareas completadas
 * @param tallies        una fila por integrante de la institución, incluso con cero
 */
public record TaskDistributionReport(long completedTasks, List<Tally> tallies) {

    /**
     * @param userId         usuario de Flowable
     * @param group          grupo al que pertenece
     * @param completedTasks tareas de usuario que completó
     */
    public record Tally(String userId, String group, long completedTasks) {
    }

    public TaskDistributionReport(long completedTasks, List<Tally> tallies) {
        this.completedTasks = completedTasks;
        this.tallies = List.copyOf(tallies);
    }
}

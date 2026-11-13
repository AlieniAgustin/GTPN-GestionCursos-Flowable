package com.materiales.app.analysis;

import java.util.List;

/**
 * Enunciado 4.3.1: cantidad y porcentaje de solicitudes que terminaron con cada resultado.
 * Los porcentajes se calculan sobre el total de instancias <b>terminadas</b>.
 *
 * @param finishedInstances total de instancias terminadas
 * @param tallies           una fila por resultado posible, en el orden del enunciado
 */
public record ResultsReport(long finishedInstances, List<Tally> tallies) {

    /**
     * @param label      cómo nombra el enunciado a ese resultado
     * @param result     valor de la variable `resultadoCurso`, o null para las instancias sin resultado
     * @param count      instancias terminadas con ese resultado
     * @param percentage porcentaje sobre el total de terminadas
     */
    public record Tally(String label, String result, long count, double percentage) {
    }

    public ResultsReport(long finishedInstances, List<Tally> tallies) {
        this.finishedInstances = finishedInstances;
        this.tallies = List.copyOf(tallies);
    }
}

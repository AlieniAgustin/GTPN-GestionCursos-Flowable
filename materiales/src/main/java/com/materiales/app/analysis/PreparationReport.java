package com.materiales.app.analysis;

import java.time.Duration;
import java.util.List;

/**
 * Enunciado 4.3.2: duración promedio de cada rama de la preparación, medida desde su
 * activación (el gateway paralelo) hasta su finalización. Solo se toman las preparaciones
 * que terminaron normalmente: las interrumpidas por cancelación quedan afuera.
 *
 * @param measuredPreparations preparaciones que llegaron al fin del subproceso
 * @param cancelledPreparations preparaciones interrumpidas por cancelación, excluidas
 * @param preparationsInProgress preparaciones todavía en curso, excluidas
 * @param instancesWithoutPreparation instancias que nunca entraron en preparación (no viables, rechazadas, sin respuesta)
 * @param branches una fila por rama
 */
public record PreparationReport(
        long measuredPreparations,
        long cancelledPreparations,
        long preparationsInProgress,
        long instancesWithoutPreparation,
        List<Branch> branches
) {

    /**
     * @param label    nombre de la rama
     * @param samples  preparaciones en las que se pudo medir
     * @param average  duración promedio, o null si no hubo muestras
     */
    public record Branch(String label, long samples, Duration average) {
    }

    public PreparationReport(long measuredPreparations, long cancelledPreparations,
                             long preparationsInProgress, long instancesWithoutPreparation,
                             List<Branch> branches) {
        this.measuredPreparations = measuredPreparations;
        this.cancelledPreparations = cancelledPreparations;
        this.preparationsInProgress = preparationsInProgress;
        this.instancesWithoutPreparation = instancesWithoutPreparation;
        this.branches = List.copyOf(branches);
    }
}

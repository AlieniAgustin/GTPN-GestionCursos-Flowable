package com.materiales.app.analysis;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El formato es parte de la entrega: el enunciado 4.3 pide mostrar tablas y entregar una
 * captura de los resultados.
 */
class AnalysisFormatterTest {

    @Test
    @DisplayName("Format: the results table shows counts, percentages and a total")
    void testResultsTable() {
        ResultsReport report = new ResultsReport(4, List.of(
                new ResultsReport.Tally("Curso finalizado", "FINALIZADO", 3, 75.0),
                new ResultsReport.Tally("Cancelación", "CANCELADO", 1, 25.0)));

        String text = AnalysisFormatter.formatResults(report);

        assertTrue(text.contains("Instancias terminadas: 4"), text);
        assertTrue(text.contains("Curso finalizado"), text);
        assertTrue(text.contains("75.0%"), text);
        assertTrue(text.contains("Total"), text);
        assertTrue(text.contains("100.0%"), text);
    }

    @Test
    @DisplayName("Format: with no finished instances it says so instead of printing an empty table")
    void testResultsWithoutData() {
        String text = AnalysisFormatter.formatResults(new ResultsReport(0, List.of()));

        assertTrue(text.contains("Todavía no hay instancias terminadas"), text);
        assertFalse(text.contains("Porcentaje"), text);
    }

    @Test
    @DisplayName("Format: the preparation table reports what was excluded")
    void testPreparationTable() {
        PreparationReport report = new PreparationReport(5, 2, 1, 3, List.of(
                new PreparationReport.Branch("Inscripciones", 5, Duration.ofMinutes(72)),
                new PreparationReport.Branch("Materiales", 5, Duration.ofMinutes(35)),
                new PreparationReport.Branch("Recursos", 5, Duration.ofHours(2))));

        String text = AnalysisFormatter.formatPreparation(report);

        assertTrue(text.contains("Preparaciones completadas normalmente: 5"), text);
        assertTrue(text.contains("2 por cancelación, 1 en curso"), text);
        assertTrue(text.contains("nunca entraron en preparación: 3"), text);
        assertTrue(text.contains("1 h 12 min"), text);
        assertTrue(text.contains("2 h 0 min"), text);
    }

    @Test
    @DisplayName("Format: with nothing to measure the preparation table is not printed")
    void testPreparationWithoutData() {
        String text = AnalysisFormatter.formatPreparation(
                new PreparationReport(0, 0, 0, 0, List.of()));

        assertTrue(text.contains("Todavía no hay preparaciones completadas"), text);
    }

    @Test
    @DisplayName("Format: the distribution table lists every member, zeros included")
    void testDistributionTable() {
        TaskDistributionReport report = new TaskDistributionReport(7, List.of(
                new TaskDistributionReport.Tally("administrativo1", "Administración", 5),
                new TaskDistributionReport.Tally("jefedocente", "Docentes", 2),
                new TaskDistributionReport.Tally("docente2", "Docentes", 0)));

        String text = AnalysisFormatter.formatTaskDistribution(report);

        assertTrue(text.contains("Tareas completadas: 7"), text);
        assertTrue(text.contains("administrativo1"), text);
        assertTrue(text.contains("docente2"), text);
    }

    @Test
    @DisplayName("Format: durations are rendered in readable units")
    void testHumanizedDurations() {
        assertEquals("2 h 5 min", AnalysisFormatter.humanize(Duration.ofMinutes(125)));
        assertEquals("35 min 10 s", AnalysisFormatter.humanize(Duration.ofSeconds(2110)));
        assertEquals("45 s", AnalysisFormatter.humanize(Duration.ofSeconds(45)));
        assertEquals("800 ms", AnalysisFormatter.humanize(Duration.ofMillis(800)));
        assertEquals("—", AnalysisFormatter.humanize(null), "Sin muestras no hay promedio");
    }
}

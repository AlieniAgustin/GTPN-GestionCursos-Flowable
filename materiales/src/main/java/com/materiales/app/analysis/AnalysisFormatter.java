package com.materiales.app.analysis;

import java.time.Duration;
import java.util.Locale;

/**
 * Arma las tres tablas del enunciado 4.3 como texto plano, para la consola y para la
 * ventana de la app (misma salida en los dos lados).
 */
public final class AnalysisFormatter {

    private AnalysisFormatter() {
    }

    public static String format(ResultsReport results, PreparationReport preparation,
                                TaskDistributionReport distribution) {
        return formatResults(results)
                + System.lineSeparator()
                + formatPreparation(preparation)
                + System.lineSeparator()
                + formatTaskDistribution(distribution);
    }

    /** 4.3.1 Resultados de las solicitudes. */
    public static String formatResults(ResultsReport report) {
        StringBuilder out = new StringBuilder();
        out.append("1) Resultados de las solicitudes (§4.3.1)").append(nl());
        out.append("   Instancias terminadas: ").append(report.finishedInstances()).append(nl());
        out.append(nl());

        if (report.finishedInstances() == 0) {
            out.append("   Todavía no hay instancias terminadas.").append(nl());
            return out.toString();
        }

        out.append(row("Resultado", "Cantidad", "Porcentaje"));
        out.append(separator());
        for (ResultsReport.Tally tally : report.tallies()) {
            out.append(row(tally.label(),
                    String.valueOf(tally.count()),
                    percent(tally.percentage())));
        }
        out.append(separator());
        out.append(row("Total", String.valueOf(report.finishedInstances()), percent(100.0)));
        return out.toString();
    }

    /** 4.3.2 Análisis de la preparación paralela. */
    public static String formatPreparation(PreparationReport report) {
        StringBuilder out = new StringBuilder();
        out.append("2) Preparación en paralelo (§4.3.2)").append(nl());
        out.append("   Preparaciones completadas normalmente: ")
                .append(report.measuredPreparations()).append(nl());
        out.append("   Excluidas: ").append(report.cancelledPreparations())
                .append(" por cancelación, ").append(report.preparationsInProgress())
                .append(" en curso").append(nl());
        out.append("   Instancias que nunca entraron en preparación: ")
                .append(report.instancesWithoutPreparation()).append(nl());
        out.append(nl());

        if (report.measuredPreparations() == 0) {
            out.append("   Todavía no hay preparaciones completadas para medir.").append(nl());
            return out.toString();
        }

        out.append(row("Rama", "Muestras", "Promedio"));
        out.append(separator());
        for (PreparationReport.Branch branch : report.branches()) {
            out.append(row(branch.label(),
                    String.valueOf(branch.samples()),
                    humanize(branch.average())));
        }
        return out.toString();
    }

    /** 4.3.3 Distribución de tareas. */
    public static String formatTaskDistribution(TaskDistributionReport report) {
        StringBuilder out = new StringBuilder();
        out.append("3) Distribución de tareas de usuario (§4.3.3)").append(nl());
        out.append("   Tareas completadas: ").append(report.completedTasks()).append(nl());
        out.append(nl());
        out.append(distributionRow("Usuario", "Grupo", "Completadas"));
        out.append(distributionSeparator());
        for (TaskDistributionReport.Tally tally : report.tallies()) {
            out.append(distributionRow(tally.userId(), tally.group(),
                    String.valueOf(tally.completedTasks())));
        }
        return out.toString();
    }

    /** Duración en algo legible: "2 h 5 min", "35 min 10 s", "800 ms". */
    static String humanize(Duration duration) {
        if (duration == null) {
            return "—";
        }
        long seconds = duration.getSeconds();
        if (seconds >= 3600) {
            return (seconds / 3600) + " h " + ((seconds % 3600) / 60) + " min";
        }
        if (seconds >= 60) {
            return (seconds / 60) + " min " + (seconds % 60) + " s";
        }
        if (seconds > 0) {
            return seconds + " s";
        }
        return duration.toMillis() + " ms";
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static String row(String first, String second, String third) {
        return String.format("   %-34s %10s %14s%n", first, second, third);
    }

    private static String separator() {
        return String.format("   %-34s %10s %14s%n",
                "-".repeat(34), "-".repeat(10), "-".repeat(14));
    }

    /** La distribución necesita más lugar para el grupo y menos para el nombre. */
    private static String distributionRow(String user, String group, String count) {
        return String.format("   %-22s %-20s %14s%n", user, group, count);
    }

    private static String distributionSeparator() {
        return String.format("   %-22s %-20s %14s%n",
                "-".repeat(22), "-".repeat(20), "-".repeat(14));
    }

    private static String nl() {
        return System.lineSeparator();
    }
}

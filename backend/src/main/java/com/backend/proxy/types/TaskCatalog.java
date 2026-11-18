package com.backend.proxy.types;

import java.util.Map;
import java.util.Set;

/**
 * Qué hace falta para completar cada tarea de usuario del modelo.
 *
 * <p>Algunas se completan con solo confirmarlas —el enunciado no pide capturar nada— y
 * otras necesitan una pantalla propia porque tienen que producir variables del proceso
 * (la propuesta con su programa y presupuesto, la selección de recursos, el docente que
 * dicta el curso, el informe).
 */
public final class TaskCatalog {

    /**
     * Tareas que se completan con una confirmación, junto con las variables que dejan.
     */
    private static final Map<String, Map<String, Object>> DIRECTAS = Map.of(
            "archivarsolicitud", Map.of(),
            "confeccionarlistadefinitiva", Map.of("listaDefinitiva", "Lista confirmada"),
            "prepararmateriales", Map.of(),
            "solicitaraulavirtual", Map.of(),
            "contratarequipamiento", Map.of(),
            "liberarequipamiento", Map.of(),
            "gestionarfaltadeaulas", Map.of(),
            "confirmarliberacionrecursos", Map.of(),
            "desarrollarcurso", Map.of("cursoDesarrollado", true)
    );

    /**
     * Tareas con pantalla propia. Las que todavía no tienen su pantalla en el sitio
     * (ver {@link #FORMULARIOS_EN_SITIO}) la bandeja las deriva a Flowable UI.
     */
    private static final Set<String> CON_FORMULARIO = Set.of(
            "registraryelaborarpropuesta",
            "revisarpropuesta",
            "evaluarrecursos",
            "definirdocentequedesarrollaelcurso",
            "elaborarinformederesultados",
            "enviarinformeaempresayarchivarexpediente"
    );

    /**
     * Tareas con formulario que ya tienen su pantalla en el sitio, con el tramo de la URL
     * ({@code /tareas/{id}/<tramo>}) donde se completan.
     */
    private static final Map<String, String> FORMULARIOS_EN_SITIO = Map.of(
            "registraryelaborarpropuesta", "elaborar-propuesta",
            "revisarpropuesta", "revisar-propuesta",
            "evaluarrecursos", "evaluar-recursos",
            "definirdocentequedesarrollaelcurso", "definir-docente",
            "elaborarinformederesultados", "elaborar-informe",
            "enviarinformeaempresayarchivarexpediente", "enviar-informe"
    );

    public static boolean seCompletaDirecto(String taskDefinitionKey) {
        return DIRECTAS.containsKey(taskDefinitionKey);
    }

    public static boolean necesitaFormulario(String taskDefinitionKey) {
        return CON_FORMULARIO.contains(taskDefinitionKey);
    }

    /**
     * Tramo de la URL de la pantalla del sitio que completa la tarea, o {@code null} si
     * la tarea todavía no tiene pantalla propia.
     */
    public static String formularioEnSitio(String taskDefinitionKey) {
        return FORMULARIOS_EN_SITIO.get(taskDefinitionKey);
    }

    /**
     * Variables que deja una tarea de confirmación directa.
     */
    public static Map<String, Object> variablesDe(String taskDefinitionKey) {
        return DIRECTAS.getOrDefault(taskDefinitionKey, Map.of());
    }

    private TaskCatalog() {
    }
}

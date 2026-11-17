package com.backend.proxy.web.form;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Formulario público de solicitud de capacitación (enunciado 4.1.1).
 *
 * <p>Es plano a propósito: un formulario HTML no manda objetos anidados ni listas de
 * fechas tipadas. Las fechas llegan como texto separado por comas y se parsean acá, para
 * poder devolver un mensaje entendible en vez de un error de binding de Spring.
 */
@Getter
@Setter
public class SolicitudForm {

    @NotBlank(message = "Indicá la razón social de la empresa")
    private String companyName;

    @NotBlank(message = "Indicá un email de contacto")
    @Email(message = "El email no tiene un formato válido")
    private String companyEmail;

    @Pattern(
        regexp = "^\\+?[0-9]{10,15}$",
        message = "El teléfono debe tener entre 10 y 15 dígitos, opcionalmente con + adelante"
    )
    private String companyPhone;

    @NotBlank(message = "Indicá la temática de la capacitación")
    @Size(max = 50, message = "La temática no puede superar los 50 caracteres")
    private String name;

    @NotBlank(message = "Describí la capacitación que necesitan")
    @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
    private String description;

    @NotNull(message = "Indicá cuántas personas participarían")
    @Min(value = 1, message = "Tiene que haber al menos un participante")
    private Integer participantCount;

    @NotBlank(message = "Indicá al menos una fecha posible")
    private String preferredDates;

    /**
     * Interpreta el texto de fechas. Devuelve la lista parseada o lanza
     * {@link DateFormatException} con la primera fecha que no se entendió, para que el
     * controlador la muestre sobre el campo.
     */
    public List<LocalDate> parsedDates() {
        List<LocalDate> dates = new ArrayList<>();

        for (String chunk : preferredDates.split(",")) {
            String text = chunk.trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                dates.add(LocalDate.parse(text));
            } catch (DateTimeParseException e) {
                throw new DateFormatException(text);
            }
        }

        if (dates.isEmpty()) {
            throw new DateFormatException(preferredDates.trim());
        }
        if (dates.size() > 10) {
            throw new DateFormatException("más de 10 fechas");
        }
        return dates;
    }

    /** Una fecha del formulario que no se pudo interpretar. */
    public static class DateFormatException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DateFormatException(String value) {
            super(value);
        }
    }
}

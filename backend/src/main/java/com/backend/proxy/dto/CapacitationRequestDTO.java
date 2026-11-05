package com.backend.proxy.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CapacitationRequestDTO(

    ContactoEmpresaDTO contacto,

    @NotBlank(message = "El nombre es obligatorio")
    @Max(value = 50, message = "El nombre excede los caracteres permitidos")
    String nombre,

    @NotBlank
    @Max(value = 200, message = "La descripcion excede los caracteres permitidos")
    String descripcion,
    
    @Digits(integer = 10, fraction = 0, message = "La cantidad de personas tiene que ser un numero entero")
    Integer cantidadParticipantes,
    
    @NotEmpty 
    @Size(max = 10, message = "No puede planificarse en fechas pasadas, ni se aceptan mas de 10 fechas")
    List<@NotNull @Future LocalDate> fechasPosibles
) {}


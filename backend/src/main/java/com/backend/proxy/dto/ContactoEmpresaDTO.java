package com.backend.proxy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContactoEmpresaDTO(

    @NotBlank
    String nombre,

    @NotBlank
    @Email
    String email,

    @Pattern(regexp = "^\\+?[0-9]{10,15}$",
    message = "El teléfono debe contener entre 10 y 15 dígitos, opcionalmente precedidos por +. Ejemplo: +543581234567")
    String telefono
){}
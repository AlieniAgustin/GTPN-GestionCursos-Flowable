package com.backend.proxy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompanyContactDTO(

    @NotBlank
    String name,

    @NotBlank
    @Email
    String email,

    @Pattern(
        regexp = "^\\+?[0-9]{10,15}$",
        message = "The phone number must contain between 10 and 15 digits, optionally prefixed with +. Example: +543581234567"
    )
    String phone
) {}

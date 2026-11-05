package com.backend.proxy.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequestDTO(

    @NotNull
    @Valid
    CompanyContactDTO contact,

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    String name,

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    String description,

    @Digits(integer = 10, fraction = 0, message = "The number of participants must be an integer")
    Integer participantCount,

    @NotEmpty(message = "Possible dates cannot be empty")
    @Size(max = 10, message = "Cannot provide more than 10 dates")
    List<@NotNull @Future(message = "Dates must be in the future") LocalDate> possibleDates
) {}

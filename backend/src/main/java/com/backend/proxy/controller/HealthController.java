package com.backend.proxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Liveness probe used by the Docker healthcheck. Public by SecurityConfig.
 */
@Tag(name = "Health", description = "Verificación de salud del servicio")
@RestController
public class HealthController {

    @Operation(summary = "Liveness probe", description = "Retorna UP si la aplicación está en ejecución")
    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}

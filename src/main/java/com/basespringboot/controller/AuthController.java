package com.basespringboot.controller;

import com.basespringboot.config.OpenApiConfig;
import com.basespringboot.dto.LoginRequest;
import com.basespringboot.dto.SignupRequest;
import com.basespringboot.dto.TokenResponse;
import com.basespringboot.exception.ApiError;
import com.basespringboot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.AUTH_TAG)
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Créer un compte",
            description = "Un nouveau joueur commence avec 0 cookie, 1 cookie par clic et aucune production passive.")
    @ApiResponse(responseCode = "201", description = "Compte créé et jeton JWT délivré.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Les données envoyées sont invalides.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ce pseudo est déjà utilisé.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter",
            description = "Le jeton JWT est valable une heure.")
    @ApiResponse(responseCode = "200", description = "Connexion réussie et jeton JWT délivré.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Les données envoyées sont invalides.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Pseudo ou mot de passe incorrect.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}

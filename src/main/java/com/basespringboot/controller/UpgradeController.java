package com.basespringboot.controller;

import com.basespringboot.config.OpenApiConfig;
import com.basespringboot.dto.UpgradeRequest;
import com.basespringboot.dto.UpgradeResponse;
import com.basespringboot.exception.ApiError;
import com.basespringboot.service.UpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/upgrades")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.UPGRADE_TAG)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "401", description = "Jeton JWT absent, invalide ou expiré.", content = @Content)
public class UpgradeController {

    private final UpgradeService upgradeService;

    @PostMapping
    @Operation(summary = "Ajouter une amélioration au catalogue",
            description = "Le prix doit être positif et l’amélioration doit accorder au moins un bonus.")
    @ApiResponse(responseCode = "201", description = "Amélioration créée.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Les données envoyées sont invalides.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Le nom de l’amélioration est déjà utilisé.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<UpgradeResponse> create(@Valid @RequestBody UpgradeRequest request) {
        UpgradeResponse upgrade = upgradeService.create(request);
        return ResponseEntity.created(URI.create("/api/upgrades/" + upgrade.id())).body(upgrade);
    }

    @GetMapping
    @Operation(summary = "Consulter le catalogue des améliorations")
    @ApiResponse(responseCode = "200", description = "Catalogue des améliorations.", useReturnTypeSchema = true)
    public List<UpgradeResponse> findAll() {
        return upgradeService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une amélioration")
    @ApiResponse(responseCode = "200", description = "Détail de l’amélioration.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Amélioration introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public UpgradeResponse findById(@PathVariable Long id) {
        return upgradeService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une amélioration du catalogue")
    @ApiResponse(responseCode = "200", description = "Amélioration mise à jour.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Les données envoyées sont invalides.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Amélioration introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Le nom de l’amélioration est déjà utilisé.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public UpgradeResponse update(@PathVariable Long id, @Valid @RequestBody UpgradeRequest request) {
        return upgradeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirer une amélioration du catalogue",
            description = "Seules les améliorations jamais achetées peuvent être supprimées.")
    @ApiResponse(responseCode = "204", description = "Amélioration supprimée.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Amélioration introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "L’amélioration figure dans l’historique des achats.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        upgradeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.basespringboot.controller;

import com.basespringboot.config.OpenApiConfig;
import com.basespringboot.dto.PlayerResponse;
import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.dto.PurchaseResponse;
import com.basespringboot.dto.WeatherRequest;
import com.basespringboot.exception.ApiError;
import com.basespringboot.service.PlayerService;
import com.basespringboot.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "401", description = "Jeton JWT absent, invalide ou expiré.", content = @Content)
public class PlayerController {

    private final PlayerService playerService;
    private final WeatherService weatherService;

    @GetMapping
    @Operation(tags = OpenApiConfig.PLAYER_TAG, summary = "Consulter mon profil",
            description = "Le joueur est identifié par son jeton JWT.")
    @ApiResponse(responseCode = "200", description = "Profil du joueur connecté.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse findMine(@AuthenticationPrincipal Jwt jwt) {
        return playerService.findById(((Number) jwt.getClaim("playerId")).longValue());
    }

    @GetMapping("/{id}")
    @Operation(tags = OpenApiConfig.PLAYER_TAG, summary = "Consulter un profil par identifiant")
    @ApiResponse(responseCode = "200", description = "Profil du joueur.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(tags = OpenApiConfig.PLAYER_TAG, summary = "Modifier mon pseudo")
    @ApiResponse(responseCode = "200", description = "Profil mis à jour.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Les données envoyées sont invalides.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ce pseudo est déjà utilisé.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse update(@PathVariable Long id, @Valid @RequestBody PlayerRequest request, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(tags = OpenApiConfig.PLAYER_TAG, summary = "Supprimer mon compte",
            description = "Supprime aussi l’historique des achats du joueur.")
    @ApiResponse(responseCode = "204", description = "Compte supprimé.", content = @Content)
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        playerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/click")
    @Operation(tags = OpenApiConfig.GAME_TAG, summary = "Cuire des cookies en cliquant",
            description = "Ajoute la cadence par clic, bonus météo actif compris.")
    @ApiResponse(responseCode = "200", description = "Solde et production mis à jour.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse click(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.click(id);
    }

    @PostMapping("/{id}/collect")
    @Operation(tags = OpenApiConfig.GAME_TAG, summary = "Collecter la production passive",
            description = "Crédite l’équivalent d’une seconde de production. Le jeu appelle cette route pendant la partie.")
    @ApiResponse(responseCode = "200", description = "Production passive créditée.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse collectPassiveCookies(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.collectPassiveCookies(id);
    }

    @PutMapping("/{id}/weather")
    @Operation(tags = OpenApiConfig.GAME_TAG, summary = "Activer le bonus météo",
            description = "Recherche la ville sur Open-Meteo. Le bonus s’applique aux clics et à la production passive pendant une heure.")
    @ApiResponse(responseCode = "200", description = "Météo et cadences de production mises à jour.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Renseigne une ville de 80 caractères maximum.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Ville ou joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "503", description = "Service météo momentanément indisponible.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse updateWeather(@PathVariable Long id, @Valid @RequestBody WeatherRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.updateWeather(id, weatherService.lookup(request.city()));
    }

    @PostMapping("/{playerId}/upgrades/{upgradeId}/purchase")
    @Operation(tags = OpenApiConfig.GAME_TAG, summary = "Acheter une amélioration",
            description = "Débite le prix en cookies, applique le bonus et enregistre l’achat dans l’historique.")
    @ApiResponse(responseCode = "200", description = "Achat enregistré et profil mis à jour.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur ou amélioration introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Le solde est insuffisant pour acheter cette amélioration.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PlayerResponse purchaseUpgrade(@PathVariable Long playerId, @PathVariable Long upgradeId, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(playerId, jwt);
        return playerService.purchaseUpgrade(playerId, upgradeId);
    }

    @GetMapping("/{id}/purchases")
    @Operation(tags = OpenApiConfig.GAME_TAG, summary = "Consulter mon historique d’achats",
            description = "Les achats sont triés du plus récent au plus ancien.")
    @ApiResponse(responseCode = "200", description = "Historique des achats.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "Le profil appartient à un autre joueur.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Joueur introuvable.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public List<PurchaseResponse> purchaseHistory(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.purchaseHistory(id);
    }

    @GetMapping("/leaderboard")
    @Operation(tags = OpenApiConfig.GAME_TAG, summary = "Consulter le classement",
            description = "Affiche les dix joueurs ayant le solde de cookies le plus élevé.")
    @ApiResponse(responseCode = "200", description = "Classement des joueurs.", useReturnTypeSchema = true)
    public List<PlayerResponse> leaderboard() {
        return playerService.leaderboard();
    }

    private void requireOwner(Long playerId, Jwt jwt) {
        if (!playerId.equals(((Number) jwt.getClaim("playerId")).longValue())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your own player account");
        }
    }
}

package com.basespringboot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/** Defines the shared API documentation and the JWT scheme used by Swagger UI. */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "Fournée infinie — API Cookie Clicker",
                version = "1.0",
                description = """
                        Lance ta fournée, achète des améliorations et gagne des cookies grâce à la météo.

                        Crée un compte ou connecte-toi dans **Authentification**, puis clique sur **Authorize**
                        et colle la valeur de `accessToken` pour tester les routes protégées.
                        """
        ),
        tags = {
                @Tag(name = OpenApiConfig.AUTH_TAG, description = "Créer un compte et obtenir un jeton JWT."),
                @Tag(name = OpenApiConfig.PLAYER_TAG, description = "Consulter, modifier ou supprimer son profil."),
                @Tag(name = OpenApiConfig.GAME_TAG, description = "Produire des cookies, acheter, choisir une ville et consulter le classement."),
                @Tag(name = OpenApiConfig.UPGRADE_TAG, description = "Consulter et gérer le catalogue des améliorations.")
        }
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Colle la valeur de accessToken renvoyée par l’inscription ou la connexion."
)
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";
    public static final String AUTH_TAG = "Authentification";
    public static final String PLAYER_TAG = "Joueurs";
    public static final String GAME_TAG = "Partie et météo";
    public static final String UPGRADE_TAG = "Améliorations";
}

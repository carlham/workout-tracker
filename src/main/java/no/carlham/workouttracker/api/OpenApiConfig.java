package no.carlham.workouttracker.api;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API documentation, served at /swagger-ui.html.
 * The "Authorize" button there takes an access token from POST /api/auth/login.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Workout Tracker API",
        version = "v1",
        description = "Plan workouts from preset or custom exercises and log sessions. "
            + "Log in with POST /api/auth/login, then click Authorize and paste the access token."),
    security = @SecurityRequirement(name = "bearerAuth"),
    // Listed in the order you'd use them; controllers refer to these by name with @Tag
    tags = {
        @Tag(name = "Auth", description = "Register, log in and refresh tokens"),
        @Tag(name = "Users", description = "The logged-in user"),
        @Tag(name = "Exercises", description = "Preset exercises and your own"),
        @Tag(name = "Muscle groups", description = "Muscle groups exercises are linked to"),
        @Tag(name = "Templates", description = "Planned workouts, private or shared"),
        @Tag(name = "Sessions", description = "Logged workouts and their sets")
    })
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}

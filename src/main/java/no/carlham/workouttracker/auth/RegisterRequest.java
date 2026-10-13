package no.carlham.workouttracker.auth;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password) {

    // BCrypt accepts at most 72 bytes and Spring's encoder throws beyond that. @Size counts characters,
    // and letters like æ, ø and å take two bytes each, so check the byte length as well.
    @AssertTrue(message = "password must be at most 72 bytes")
    public boolean isPasswordWithinLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}

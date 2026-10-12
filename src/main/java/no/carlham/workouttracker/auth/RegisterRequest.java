package no.carlham.workouttracker.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        // BCrypt ignores everything after 72 bytes, so longer passwords would be silently truncated
        @NotBlank @Size(min = 8, max = 72) String password) {
}

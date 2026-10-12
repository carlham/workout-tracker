package no.carlham.workouttracker.auth;

/**
 * Returned on login and refresh. The refresh token is not in the body; it is set as an HttpOnly cookie.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}

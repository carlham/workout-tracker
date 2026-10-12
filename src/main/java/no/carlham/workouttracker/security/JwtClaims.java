package no.carlham.workouttracker.security;

/**
 * Names of the custom claims we put in access tokens.
 * The user id goes in the standard "sub" (subject) claim.
 */
public final class JwtClaims {
    public static final String USERNAME = "username";
    public static final String ROLE = "role";

    private JwtClaims() {
    }
}

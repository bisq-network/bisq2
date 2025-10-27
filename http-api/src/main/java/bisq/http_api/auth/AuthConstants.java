package bisq.http_api.auth;

public final class AuthConstants {
    private AuthConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    public static final String AUTH_HEADER = "auth-token";
}

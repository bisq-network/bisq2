package bisq.api.validator;

import bisq.api.access.filter.authz.AuthorizationException;
import bisq.api.access.filter.authz.UriValidator;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UriValidatorTest {

    /* ---------- URI sanitizer (path) ---------- */

    @Test
    void nullUriRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(NullPointerException.class,
                () -> validator.validate((URI) null));
    }

    @Test
    void emptyPathRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate(""));
    }

    @Test
    void pathMustStartWithSlash() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("foo/bar"));
    }

    @Test
    void dotDotTraversalInPathRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/../bar"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("../foo"));
    }

    @Test
    void encodedTraversalStillRejected() {
        UriValidator validator =
                new UriValidator();

        // URI decoding turns %2e%2e into ..
        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/%2e%2e/bar"));
    }

    @Test
    void unsafeCharactersInPathRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/<script>"));
    }

    @Test
    void pathLengthLimitEnforced() {
        UriValidator validator =
                new UriValidator();

        String longPath = "/" + "a".repeat(2000);

        assertThrows(AuthorizationException.class,
                () -> validator.validate(longPath));
    }

    @Test
    void validPathAccepted() {
        UriValidator validator =
                new UriValidator();

        assertDoesNotThrow(() -> validator.validate("/foo/bar"));
    }

    /* ---------- URI sanitizer (query) ---------- */

    @Test
    void validQueryAccepted() {
        UriValidator validator =
                new UriValidator();

        assertDoesNotThrow(() ->
                validator.validate("/foo/bar?x=1&y=test"));
    }

    @Test
    void unsafeCharactersInQueryRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/bar?x=<script>"));
    }

    @Test
    void dotDotInQueryRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/bar?path=../secret"));
    }

    @Test
    void validPathWithSpacesAccepted() {
        UriValidator validator =
                new UriValidator();

        // In practice, URIs arrive already parsed by the HTTP server.
        // Spaces in paths are decoded by URI.getPath(), so test via URI object.
        assertDoesNotThrow(() -> validator.validate(URI.create("/foo/my%20account%20name")));
    }

    @Test
    void queryWithSpecialCharactersAccepted() {
        UriValidator validator =
                new UriValidator();

        // Query params with special characters (parentheses, spaces) should be allowed.
        // URI.getQuery() decodes percent-encoded values.
        assertDoesNotThrow(() ->
                validator.validate(URI.create("/foo/bar?accountName=My%20Bank%20(EUR)")));
    }

    @Test
    void queryWithControlCharactersRejected() {
        UriValidator validator =
                new UriValidator();

        // Control characters in query should be rejected.
        // Build URI directly to bypass URI.create encoding restrictions.
        assertThrows(AuthorizationException.class,
                () -> validator.validate(URI.create("/foo/bar?x=value%00bad")));
    }

    /* ---------- Raw URI parsing ---------- */

    @Test
    void malformedUriRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("http://:bad_path"));
    }
}


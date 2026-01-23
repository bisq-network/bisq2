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

    /* ---------- Raw URI parsing ---------- */

    @Test
    void malformedUriRejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("http://:bad_path"));
    }
}


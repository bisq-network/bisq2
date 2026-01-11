package bisq.api.validator;

import bisq.api.access.filter.authz.AuthorizationException;
import bisq.api.access.filter.authz.HttpEndpointValidator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpEndpointValidatorTest {

    /* ---------- URI sanitizer (path) ---------- */

    @Test
    void nullUriRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(NullPointerException.class,
                () -> validator.validate((URI) null));
    }

    @Test
    void emptyPathRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate(""));
    }

    @Test
    void pathMustStartWithSlash() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("foo/bar"));
    }

    @Test
    void dotDotTraversalInPathRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/../bar"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("../foo"));
    }

    @Test
    void encodedTraversalStillRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        // URI decoding turns %2e%2e into ..
        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/%2e%2e/bar"));
    }

    @Test
    void unsafeCharactersInPathRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/<script>"));
    }

    @Test
    void pathLengthLimitEnforced() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        String longPath = "/" + "a".repeat(2000);

        assertThrows(AuthorizationException.class,
                () -> validator.validate(longPath));
    }

    @Test
    void validPathAccepted() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertDoesNotThrow(() -> validator.validate("/foo/bar"));
    }

    /* ---------- URI sanitizer (query) ---------- */

    @Test
    void validQueryAccepted() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertDoesNotThrow(() ->
                validator.validate("/foo/bar?x=1&y=test"));
    }

    @Test
    void unsafeCharactersInQueryRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/bar?x=<script>"));
    }

    @Test
    void dotDotInQueryRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/bar?path=../secret"));
    }

    /* ---------- Allowlist semantics ---------- */

    @Test
    void allowlistAbsentAllowsAll() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertDoesNotThrow(() ->
                validator.validate("/any/path"));
    }

    @Test
    void allowlistPresentButEmptyDeniesAll() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.of(List.of()), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo"));
    }

    @Test
    void allowlistExactMatchAllowed() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.of(List.of("/foo/bar")), List.of());

        assertDoesNotThrow(() ->
                validator.validate("/foo/bar"));
    }

    @Test
    void allowlistBlocksNonMatching() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.of(List.of("/foo/bar")), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/baz"));
    }

    @Test
    void allowlistRegexMatches() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.of(List.of("/api/v1/.*")), List.of());

        assertDoesNotThrow(() ->
                validator.validate("/api/v1/resource"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/api/v2/resource"));
    }

    /* ---------- Denylist semantics ---------- */

    @Test
    void denylistBlocksExactMatch() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of("/blocked"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/blocked"));
    }

    @Test
    void denylistRegexBlocks() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of("/api/private/.*"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/api/private/data"));

        assertDoesNotThrow(() ->
                validator.validate("/api/public/data"));
    }

    /* ---------- Allowlist + Denylist interaction ---------- */

    @Test
    void denylistOverridesAllowlist() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(
                        Optional.of(List.of("/api/.*")),
                        List.of("/api/admin/.*"));

        assertDoesNotThrow(() ->
                validator.validate("/api/public"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/api/admin/config"));
    }

    /* ---------- Raw URI parsing ---------- */

    @Test
    void malformedUriRejected() {
        HttpEndpointValidator validator =
                new HttpEndpointValidator(Optional.empty(), List.of());

        assertThrows(AuthorizationException.class,
                () -> validator.validate("http://:bad_path"));
    }
}


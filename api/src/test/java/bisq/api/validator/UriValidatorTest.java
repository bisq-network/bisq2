package bisq.api.validator;

import bisq.api.access.filter.authz.AuthorizationException;
import bisq.api.access.filter.authz.UriValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UriValidatorTest {

    /* ---------- URI sanitizer (path) ---------- */

    @Test
    @DisplayName("null uri rejected")
    void null_uri_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(NullPointerException.class,
                () -> validator.validate((URI) null));
    }

    @Test
    @DisplayName("empty path rejected")
    void empty_path_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate(""));
    }

    @Test
    @DisplayName("path must start with slash")
    void path_must_start_with_slash() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("foo/bar"));
    }

    @Test
    @DisplayName("dot dot traversal in path rejected")
    void dot_dot_traversal_in_path_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/../bar"));

        assertThrows(AuthorizationException.class,
                () -> validator.validate("../foo"));
    }

    @Test
    @DisplayName("encoded traversal still rejected")
    void encoded_traversal_still_rejected() {
        UriValidator validator =
                new UriValidator();

        // URI decoding turns %2e%2e into ..
        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/%2e%2e/bar"));
    }

    @Test
    @DisplayName("unsafe characters in path rejected")
    void unsafe_characters_in_path_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/<script>"));
    }

    @Test
    @DisplayName("path length limit enforced")
    void path_length_limit_enforced() {
        UriValidator validator =
                new UriValidator();

        String longPath = "/" + "a".repeat(2000);

        assertThrows(AuthorizationException.class,
                () -> validator.validate(longPath));
    }

    @Test
    @DisplayName("valid path accepted")
    void valid_path_accepted() {
        UriValidator validator =
                new UriValidator();

        assertDoesNotThrow(() -> validator.validate("/foo/bar"));
    }

    /* ---------- URI sanitizer (query) ---------- */

    @Test
    @DisplayName("valid query accepted")
    void valid_query_accepted() {
        UriValidator validator =
                new UriValidator();

        assertDoesNotThrow(() ->
                validator.validate("/foo/bar?x=1&y=test"));
    }

    @Test
    @DisplayName("unsafe characters in query rejected")
    void unsafe_characters_in_query_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/bar?x=<script>"));
    }

    @Test
    @DisplayName("dot dot in query rejected")
    void dot_dot_in_query_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("/foo/bar?path=../secret"));
    }

    @Test
    @DisplayName("valid path with spaces accepted")
    void valid_path_with_spaces_accepted() {
        UriValidator validator =
                new UriValidator();

        // In practice, URIs arrive already parsed by the HTTP server.
        // Spaces in paths are decoded by URI.getPath(), so test via URI object.
        assertDoesNotThrow(() -> validator.validate(URI.create("/foo/my%20account%20name")));
    }

    @Test
    @DisplayName("query with special characters accepted")
    void query_with_special_characters_accepted() {
        UriValidator validator =
                new UriValidator();

        // Query params with special characters (parentheses, spaces) should be allowed.
        // URI.getQuery() decodes percent-encoded values.
        assertDoesNotThrow(() ->
                validator.validate(URI.create("/foo/bar?accountName=My%20Bank%20(EUR)")));
    }

    @Test
    @DisplayName("query with control characters rejected")
    void query_with_control_characters_rejected() {
        UriValidator validator =
                new UriValidator();

        // Control characters in query should be rejected.
        // Build URI directly to bypass URI.create encoding restrictions.
        assertThrows(AuthorizationException.class,
                () -> validator.validate(URI.create("/foo/bar?x=value%00bad")));
    }

    /* ---------- Raw URI parsing ---------- */

    @Test
    @DisplayName("malformed uri rejected")
    void malformed_uri_rejected() {
        UriValidator validator =
                new UriValidator();

        assertThrows(AuthorizationException.class,
                () -> validator.validate("http://:bad_path"));
    }
}


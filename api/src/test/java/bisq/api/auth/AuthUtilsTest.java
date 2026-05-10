package bisq.api.auth;

import bisq.api.access.filter.authn.AuthUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthUtilsTest {

    @Test
    @DisplayName("normalize path and query simple root path")
    void normalize_path_and_query_simple_root_path() {
        URI uri = URI.create("/");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/", result);
    }

    @Test
    @DisplayName("normalize path and query simple path")
    void normalize_path_and_query_simple_path() {
        URI uri = URI.create("/api/users");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    @DisplayName("normalize path and query path with trailing slash")
    void normalize_path_and_query_path_with_trailing_slash() {
        URI uri = URI.create("/api/users/");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    @DisplayName("normalize path and query path with query")
    void normalize_path_and_query_path_with_query() {
        URI uri = URI.create("/api/users?limit=10&offset=0");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users?limit=10&offset=0", result);
    }

    @Test
    @DisplayName("normalize path and query path with trailing slash and query")
    void normalize_path_and_query_path_with_trailing_slash_and_query() {
        URI uri = URI.create("/api/users/?limit=10&offset=0");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users?limit=10&offset=0", result);
    }

    @Test
    @DisplayName("normalize path and query path with empty query")
    void normalize_path_and_query_path_with_empty_query() {
        URI uri = URI.create("/api/users?");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    @DisplayName("normalize path and query null path")
    void normalize_path_and_query_null_path() {
        // Create URI with scheme only (no path)
        URI uri = URI.create("http://localhost");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/", result);
    }
    
    @Test
    @DisplayName("normalize path and query path with multiple query params")
    void normalize_path_and_query_path_with_multiple_query_params() {
        URI uri = URI.create("/search?q=test&category=books&sort=asc&page=2");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/search?q=test&category=books&sort=asc&page=2", result);
    }

    @Test
    @DisplayName("normalize path and query encoded characters in path")
    void normalize_path_and_query_encoded_characters_in_path() {
        URI uri = URI.create("/api/users/john%20doe");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users/john%20doe", result);
    }

    @Test
    @DisplayName("normalize path and query encoded characters in query")
    void normalize_path_and_query_encoded_characters_in_query() {
        URI uri = URI.create("/search?q=hello%20world&name=john%2Bdoe");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/search?q=hello%20world&name=john%2Bdoe", result);
    }

    @Test
    @DisplayName("normalize path and query path with encoded trailing slash")
    void normalize_path_and_query_path_with_encoded_trailing_slash() {
        URI uri = URI.create("/api/users%2F");
        String result = AuthUtils.normalizePathAndQuery(uri);
        // Encoded slash should not be treated as path separator
        assertEquals("/api/users%2F", result);
    }

    @Test
    @DisplayName("normalize path and query query with special characters")
    void normalize_path_and_query_query_with_special_characters() {
        URI uri = URI.create("/api/data?filter=value&special=%26%3D%3F");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/data?filter=value&special=%26%3D%3F", result);
    }

    @Test
    @DisplayName("normalize path and query empty string path")
    void normalize_path_and_query_empty_string_path() {
        URI uri = URI.create("");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/", result);
    }

    @Test
    @DisplayName("normalize path and query path with fragment")
    void normalize_path_and_query_path_with_fragment() {
        // Fragments should not be included in the result (getRawQuery doesn't include fragment)
        URI uri = URI.create("/api/users#section");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    @DisplayName("normalize path and query path with query and fragment")
    void normalize_path_and_query_path_with_query_and_fragment() {
        // Fragments should not be included in the result
        URI uri = URI.create("/api/users?limit=10#section");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users?limit=10", result);
    }

    @Test
    @DisplayName("normalize path and query single character path")
    void normalize_path_and_query_single_character_path() {
        URI uri = URI.create("/a");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/a", result);
    }

    @Test
    @DisplayName("normalize path and query single character path with trailing slash")
    void normalize_path_and_query_single_character_path_with_trailing_slash() {
        URI uri = URI.create("/a/");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/a", result);
    }

    @Test
    @DisplayName("normalize path and query multiple consecutive slashes")
    void normalize_path_and_query_multiple_consecutive_slashes() {
        URI uri = URI.create("/api//users///data");
        String result = AuthUtils.normalizePathAndQuery(uri);
        // Should preserve the path as-is (not normalize multiple slashes)
        assertEquals("/api//users///data", result);
    }

    @Test
    @DisplayName("normalize path and query path with dots")
    void normalize_path_and_query_path_with_dots() {
        URI uri = URI.create("/api/./users/../data");
        String result = AuthUtils.normalizePathAndQuery(uri);
        // Should preserve the path as-is (URI normalization happens elsewhere)
        assertEquals("/api/./users/../data", result);
    }
}

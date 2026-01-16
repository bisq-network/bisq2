package bisq.api.auth;

import bisq.api.access.filter.authn.AuthUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthUtilsTest {

    @Test
    void testNormalizePathAndQuery_simpleRootPath() {
        URI uri = URI.create("/");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/", result);
    }

    @Test
    void testNormalizePathAndQuery_simplePath() {
        URI uri = URI.create("/api/users");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithTrailingSlash() {
        URI uri = URI.create("/api/users/");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithQuery() {
        URI uri = URI.create("/api/users?limit=10&offset=0");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users?limit=10&offset=0", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithTrailingSlashAndQuery() {
        URI uri = URI.create("/api/users/?limit=10&offset=0");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users?limit=10&offset=0", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithEmptyQuery() {
        URI uri = URI.create("/api/users?");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    void testNormalizePathAndQuery_nullPath() {
        // Create URI with scheme only (no path)
        URI uri = URI.create("http://localhost");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/", result);
    }
    
    @Test
    void testNormalizePathAndQuery_pathWithMultipleQueryParams() {
        URI uri = URI.create("/search?q=test&category=books&sort=asc&page=2");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/search?q=test&category=books&sort=asc&page=2", result);
    }

    @Test
    void testNormalizePathAndQuery_encodedCharactersInPath() {
        URI uri = URI.create("/api/users/john%20doe");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users/john%20doe", result);
    }

    @Test
    void testNormalizePathAndQuery_encodedCharactersInQuery() {
        URI uri = URI.create("/search?q=hello%20world&name=john%2Bdoe");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/search?q=hello%20world&name=john%2Bdoe", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithEncodedTrailingSlash() {
        URI uri = URI.create("/api/users%2F");
        String result = AuthUtils.normalizePathAndQuery(uri);
        // Encoded slash should not be treated as path separator
        assertEquals("/api/users%2F", result);
    }

    @Test
    void testNormalizePathAndQuery_queryWithSpecialCharacters() {
        URI uri = URI.create("/api/data?filter=value&special=%26%3D%3F");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/data?filter=value&special=%26%3D%3F", result);
    }

    @Test
    void testNormalizePathAndQuery_emptyStringPath() {
        URI uri = URI.create("");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithFragment() {
        // Fragments should not be included in the result (getRawQuery doesn't include fragment)
        URI uri = URI.create("/api/users#section");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithQueryAndFragment() {
        // Fragments should not be included in the result
        URI uri = URI.create("/api/users?limit=10#section");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/api/users?limit=10", result);
    }

    @Test
    void testNormalizePathAndQuery_singleCharacterPath() {
        URI uri = URI.create("/a");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/a", result);
    }

    @Test
    void testNormalizePathAndQuery_singleCharacterPathWithTrailingSlash() {
        URI uri = URI.create("/a/");
        String result = AuthUtils.normalizePathAndQuery(uri);
        assertEquals("/a", result);
    }

    @Test
    void testNormalizePathAndQuery_multipleConsecutiveSlashes() {
        URI uri = URI.create("/api//users///data");
        String result = AuthUtils.normalizePathAndQuery(uri);
        // Should preserve the path as-is (not normalize multiple slashes)
        assertEquals("/api//users///data", result);
    }

    @Test
    void testNormalizePathAndQuery_pathWithDots() {
        URI uri = URI.create("/api/./users/../data");
        String result = AuthUtils.normalizePathAndQuery(uri);
        // Should preserve the path as-is (URI normalization happens elsewhere)
        assertEquals("/api/./users/../data", result);
    }
}

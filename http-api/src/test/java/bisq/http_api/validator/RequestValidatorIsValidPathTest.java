package bisq.http_api.validator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidatorIsValidPathTest {

    @Test
    void testNullPath() {
        RequestValidator validator = new RequestValidator(List.of(), List.of());
        assertFalse(validator.isValidPath(null));
    }

    @Test
    void testMalformedUri() {
        RequestValidator validator = new RequestValidator(List.of(), List.of());
        assertFalse(validator.isValidPath("http://:bad_path"));
    }

    @Test
    void testEmptyPath() {
        RequestValidator validator = new RequestValidator(List.of(), List.of());
        assertFalse(validator.isValidPath(""));
    }

    @Test
    void testDotDotInPath() {
        RequestValidator validator = new RequestValidator(List.of(), List.of());
        assertFalse(validator.isValidPath("../foo/../bar"));
    }

    @Test
    void testValidPath() {
        RequestValidator validator = new RequestValidator(List.of(), List.of());
        assertTrue(validator.isValidPath("/foo/bar"));
    }

    @Test
    void testEncodedTraversalPath() {
        RequestValidator validator = new RequestValidator(List.of(), List.of());
        assertFalse(validator.isValidPath("/foo/%2e%2e/"));
    }

    @Test
    void testWhitelistAllows() {
        RequestValidator validator = new RequestValidator(List.of("/foo/bar"), List.of());
        assertTrue(validator.isValidPath("/foo/bar"));
    }

    @Test
    void testWhitelistBlocks() {
        RequestValidator validator = new RequestValidator(List.of("/foo/bar"), List.of());
        assertFalse(validator.isValidPath("/baz"));
    }

    @Test
    void testBlacklistBlocks() {
        RequestValidator validator = new RequestValidator(List.of(), List.of("/blocked"));
        assertFalse(validator.isValidPath("/blocked"));
    }

    @Test
    void testWhitelistRegexMatches() {
        // Accepts /api/v1/resource and /api/v1/other, but not /api/v2/resource
        RequestValidator validator = new RequestValidator(List.of("/api/v1/.*"), List.of());
        assertTrue(validator.isValidPath("/api/v1/resource"));
        assertTrue(validator.isValidPath("/api/v1/other"));
        assertFalse(validator.isValidPath("/api/v2/resource"));
    }

    @Test
    void testBlacklistRegexBlocks() {
        // Blocks /api/private/* endpoints
        RequestValidator validator = new RequestValidator(List.of(), List.of("/api/private/.*"));
        assertFalse(validator.isValidPath("/api/private/data"));
        assertFalse(validator.isValidPath("/api/private/foo"));
        assertTrue(validator.isValidPath("/api/public/data"));
    }

    @Test
    void testWhitelistAndBlacklistCombined() {
        // Whitelist /api/v1/*, but blacklist /api/v1/secret
        RequestValidator validator = new RequestValidator(List.of("/api/v1/.*"), List.of("/api/v1/secret"));
        assertTrue(validator.isValidPath("/api/v1/resource"));
        assertFalse(validator.isValidPath("/api/v1/secret"));
    }

    @Test
    void testWhitelistRegexExactMatch() {
        // Only allow /api/v1/exact
        RequestValidator validator = new RequestValidator(List.of("/api/v1/exact"), List.of());
        assertTrue(validator.isValidPath("/api/v1/exact"));
        assertFalse(validator.isValidPath("/api/v1/exactly"));
    }

    @Test
    void testBlacklistRegexExactMatch() {
        // Only block /api/v1/blocked
        RequestValidator validator = new RequestValidator(List.of(), List.of("/api/v1/random"));
        assertFalse(validator.isValidPath("/api/v1/random"));
        assertTrue(validator.isValidPath("/api/v1/other"));
        assertTrue(validator.isValidPath("/api/v1/rando"));
    }

    @Test
    void testBlacklistAllowsOther() {
        RequestValidator validator = new RequestValidator(List.of(), List.of("/blocked"));
        assertTrue(validator.isValidPath("/allowed"));
    }
}

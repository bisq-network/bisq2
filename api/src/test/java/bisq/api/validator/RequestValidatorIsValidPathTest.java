package bisq.api.validator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class RequestValidatorIsValidPathTest {

    @Test
    void testNullPath() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri(null));
    }

    @Test
    void testMalformedUri() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri("http://:bad_path"));
    }

    @Test
    void testEmptyPath() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri(""));
    }

    @Test
    void testDotDotInPath() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri("../foo/bar"));
        assertFalse(validator.isValidUri("/foo/../bar"));
        // also check for query traversal attacks
        assertFalse(validator.isValidUri("/foo/bar?query=../../attack"));
    }

    @Test
    void testEncodedTraversalPath() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri("/foo/%2e%2e/"));
    }

    @Test
    void testValidPath() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertTrue(validator.isValidUri("/foo/bar"));
    }

    /* ---------- Allowlist semantics ---------- */

    @Test
    void testAllowlistAbsentAllowsAll() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertTrue(validator.isValidUri("/any/path"));
        assertTrue(validator.isValidUri("/another/path"));
    }

    @Test
    void testAllowlistPresentButEmptyDeniesAll() {
        RequestValidator validator = new RequestValidator(Optional.of(List.of()), List.of());
        assertFalse(validator.isValidUri("/foo"));
        assertFalse(validator.isValidUri("/api/v1/resource"));
    }

    @Test
    void testWhitelistAllowsExactMatch() {
        RequestValidator validator = new RequestValidator(Optional.of(List.of("/foo/bar")), List.of());
        assertTrue(validator.isValidUri("/foo/bar"));
    }

    @Test
    void testWhitelistBlocksNonMatching() {
        RequestValidator validator = new RequestValidator(Optional.of(List.of("/foo/bar")), List.of());
        assertFalse(validator.isValidUri("/baz"));
    }

    @Test
    void testWhitelistRegexMatches() {
        // Accepts /api/v1/resource and /api/v1/other, but not /api/v2/resource
        RequestValidator validator = new RequestValidator(Optional.of(List.of("/api/v1/.*")), List.of());
        assertTrue(validator.isValidUri("/api/v1/resource"));
        assertTrue(validator.isValidUri("/api/v1/other"));
        assertFalse(validator.isValidUri("/api/v2/resource"));
    }

    @Test
    void testWhitelistRegexExactMatch() {
        // Only allow /api/v1/exact
        RequestValidator validator = new RequestValidator(Optional.of(List.of("/api/v1/exact")), List.of());
        assertTrue(validator.isValidUri("/api/v1/exact"));
        assertFalse(validator.isValidUri("/api/v1/exactly"));
    }

    /* ---------- Blacklist semantics ---------- */

    @Test
    void testBlacklistBlocksExactMatch() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of("/blocked"));
        assertFalse(validator.isValidUri("/blocked"));
    }

    @Test
    void testBlacklistRegexBlocks() {
        // Blocks /api/private/* endpoints
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of("/api/private/.*"));
        assertFalse(validator.isValidUri("/api/private/data"));
        assertFalse(validator.isValidUri("/api/private/foo"));
        assertTrue(validator.isValidUri("/api/public/data"));
    }

    @Test
    void testBlacklistAllowsOther() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of("/blocked"));
        assertTrue(validator.isValidUri("/allowed"));
    }

    /* ---------- Allowlist + Blacklist interaction ---------- */

    @Test
    void testWhitelistAndBlacklistCombined() {
        // Whitelist /api/v1/*, but blacklist /api/v1/secret
        RequestValidator validator =
                new RequestValidator(Optional.of(List.of("/api/v1/.*")), List.of("/api/v1/secret"));

        assertTrue(validator.isValidUri("/api/v1/resource"));
        assertFalse(validator.isValidUri("/api/v1/secret"));
    }

    @Test
    void testBlacklistStillAppliesWhenAllowlistAbsent() {
        RequestValidator validator =
                new RequestValidator(Optional.empty(), List.of("/blocked"));

        assertFalse(validator.isValidUri("/blocked"));
        assertTrue(validator.isValidUri("/allowed"));
    }

    @Test
    void testBlacklistOverridesAllowlist() {
        RequestValidator validator =
                new RequestValidator(Optional.of(List.of("/api/.*")), List.of("/api/admin/.*"));

        assertTrue(validator.isValidUri("/api/public"));
        assertFalse(validator.isValidUri("/api/admin/config"));
    }

    /* ---------- Query handling ---------- */

    @Test
    void testValidQueryAllowed() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertTrue(validator.isValidUri("/foo/bar?x=1&y=test"));
    }

    @Test
    void testUnsafeCharactersInQueryRejected() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri("/foo/bar?x=<script>"));
    }

    @Test
    void testDotDotInQueryRejected() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        assertFalse(validator.isValidUri("/foo/bar?path=../secret"));
    }

    /* ---------- Length boundary ---------- */

    @Test
    void testPathLengthLimit() {
        RequestValidator validator = new RequestValidator(Optional.empty(), List.of());
        String longPath = "/" + "a".repeat(2000);
        assertFalse(validator.isValidUri(longPath));
    }
}


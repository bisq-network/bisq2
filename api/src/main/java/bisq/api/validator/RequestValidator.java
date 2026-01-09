package bisq.api.validator;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class RequestValidator {
    // Only allow alphanumeric characters and some special characters like /, -, _, .
    // ~, and : chars are used in i2p addresses
    // Path traversal is blocked separately
    private static final Pattern DEFAULT_SAFE_PATH = Pattern.compile("^[a-zA-Z0-9/_\\-,.:=~]*$");
    private static final Pattern DEFAULT_SAFE_QUERY = Pattern.compile("^[a-zA-Z0-9/_\\-,.:?&=~]*$");

    // Optional.empty()  -> allow all
    // Optional.of(empty)-> deny all
    private final Optional<Set<Pattern>> allowlist;
    private final Set<Pattern> denyList;
    private final Pattern safePath;
    private final Pattern safeQuery;

    public RequestValidator(Optional<List<String>> allowEndpoints, List<String> denyEndpoints) {
        this(allowEndpoints, denyEndpoints, DEFAULT_SAFE_PATH, DEFAULT_SAFE_QUERY);
    }

    public RequestValidator(Optional<List<String>> whitelistPatterns,
                            List<String> blacklistPatterns,
                            Pattern safePath,
                            Pattern safeQuery) {
        allowlist = whitelistPatterns
                .map(list -> list.stream()
                        .map(Pattern::compile)
                        .collect(java.util.stream.Collectors.toSet()));
        denyList = blacklistPatterns.stream()
                .map(Pattern::compile)
                .collect(java.util.stream.Collectors.toSet());
        this.safePath = safePath;
        this.safeQuery = safeQuery;
    }

    public boolean isValidUri(String uri) {
        try {
            return hasValidComponents(new URI(uri));
        } catch (Exception e) {
            log.info("Failed to parse string uri into URI: {}", uri);
            return false;
        }
    }

    public boolean hasValidComponents(URI uri) {
        String decodedPath = uri.getPath();
        if (decodedPath == null) {
            log.info("Invalid decoded path was null.");
            return false;
        }
        if (!decodedPath.startsWith("/")) {
            log.info("Invalid path: must start with '/'. path: {}", decodedPath);
            return false;
        }
        if (!safePath.matcher(decodedPath).matches()) {
            log.info("Invalid decoded path contained unsafe characters. path: {}", decodedPath);
            return false;
        }
        if (decodedPath.contains("..")) {
            log.info("Invalid decoded path contained unsafe relative path. path: {}", decodedPath);
            return false;
        }
        if (decodedPath.length() >= 2000) {
            log.info("Invalid decoded path length was over 2000 characters: {}...", decodedPath.substring(0, 60));
            return false;
        }

        if (allowlist.isPresent()) {
            Set<Pattern> patterns = allowlist.get();

            // Explicit allowlist present:
            // - empty → deny all
            // - non-empty → must match at least one
            if (patterns.isEmpty()) {
                log.info("Access denied: allowlist present but empty. path: {}", decodedPath);
                return false;
            }

            if (patterns.stream().noneMatch(p -> p.matcher(decodedPath).matches())) {
                log.info("Accessed decoded path was not whitelisted: {}", decodedPath);
                return false;
            }
        }

        if (!denyList.isEmpty() && denyList.stream().anyMatch(p -> p.matcher(decodedPath).matches())) {
            log.info("Accessed decoded path was blacklisted: {}", decodedPath);
            return false;
        }
        String decodedQuery = uri.getQuery();
        if (decodedQuery != null) {
            if (!safeQuery.matcher(decodedQuery).matches()) {
                log.info("Invalid decoded query contained unsafe characters. query: {}", decodedQuery);
                return false;
            }
            if (decodedQuery.contains("..")) {
                log.info("Invalid decoded query contained unsafe relative path. query: {}", decodedQuery);
                return false;
            }
        }
        return true;
    }
}

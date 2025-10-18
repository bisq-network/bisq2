package bisq.http_api.validator;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class RequestValidator {
    // Only allow alphanumeric characters and some special characters like /, -, _, .
    // Path traversal is blocked separately
    private static final Pattern DEFAULT_SAFE_PATH = Pattern.compile("^[a-zA-Z0-9/_\\-,.:]*$");
    private static final Pattern DEFAULT_SAFE_QUERY = Pattern.compile("^[a-zA-Z0-9/_\\-,.:?&=]*$");

    private final Set<Pattern> endpointWhitelistPatterns;
    private final Set<Pattern> endpointBlacklistPatterns;
    private final Pattern safePath;
    private final Pattern safeQuery;

    public RequestValidator(List<String> whitelistPatterns, List<String> blacklistPatterns) {
        this(whitelistPatterns, blacklistPatterns, DEFAULT_SAFE_PATH, DEFAULT_SAFE_QUERY);
    }

    public RequestValidator(List<String> whitelistPatterns,
                            List<String> blacklistPatterns,
                            Pattern safePath,
                            Pattern safeQuery) {
        endpointWhitelistPatterns = whitelistPatterns.stream()
                .map(Pattern::compile)
                .collect(java.util.stream.Collectors.toSet());
        endpointBlacklistPatterns = blacklistPatterns.stream()
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
        if (!endpointWhitelistPatterns.isEmpty() && endpointWhitelistPatterns.stream().noneMatch(p -> p.matcher(decodedPath).matches())) {
            log.info("Accessed decoded path was not whitelisted: {}", decodedPath);
            return false;
        }
        if (!endpointBlacklistPatterns.isEmpty() && endpointBlacklistPatterns.stream().anyMatch(p -> p.matcher(decodedPath).matches())) {
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

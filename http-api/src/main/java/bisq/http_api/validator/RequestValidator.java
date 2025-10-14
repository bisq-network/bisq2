package bisq.http_api.validator;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
class RequestValidator {
    // Only allow alphanumeric characters and some special characters like /, -, _
    // Does not allow dots to avoid path traversal attacks (example: '../')
    private static final Pattern SAFE_PATH = Pattern.compile("^[a-zA-Z0-9/_\\-?=,&.:]*$");
    private final Set<Pattern> endpointWhitelistPatterns;
    private final Set<Pattern> endpointBlacklistPatterns;

    public RequestValidator(List<String> whitelistPatterns, List<String> blacklistPatterns) {
        endpointWhitelistPatterns = whitelistPatterns.stream()
                .map(Pattern::compile)
                .collect(java.util.stream.Collectors.toSet());
        endpointBlacklistPatterns = blacklistPatterns.stream()
                .map(Pattern::compile)
                .collect(java.util.stream.Collectors.toSet());
    }

    public boolean isValidPath(String path) {
        try {
            return hasValidPath(new URI(path));
        } catch (Exception e) {
            log.info("Failed to parse path into URI: {}", path);
            return false;
        }
    }

    public boolean hasValidPath(URI uri) {
        String decodedPath = uri.normalize().getPath();
        if (decodedPath == null) {
            log.info("Invalid decoded path was null.");
            return false;
        }
        if (!SAFE_PATH.matcher(decodedPath).matches()) {
            log.info("Invalid decoded path contained unsafe characters. path: {}", decodedPath);
            return false;
        }
        if (decodedPath.contains("..")) {
            log.info("Invalid decoded path contained unsafe relative path. path: {}", decodedPath);
            return false;
        }
        if (decodedPath.isEmpty()) {
            log.info("Invalid decoded path was empty");
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

        return true;
    }
}

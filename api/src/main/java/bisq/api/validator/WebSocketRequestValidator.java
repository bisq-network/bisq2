package bisq.api.validator;

import bisq.api.web_socket.rest_api_proxy.WebSocketRestApiRequest;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WebSocketRequestValidator {
    private static final Set<String> METHOD_WHITELIST = Set.of("GET", "POST", "DELETE", "PUT", "PATCH");
    private final RequestValidator requestValidator;

    public WebSocketRequestValidator(Optional<List<String>> allowEndpoints, List<String> denyEndpoints) {
        this.requestValidator = new RequestValidator(allowEndpoints, denyEndpoints);
    }

    @Nullable
    public String validateRequest(WebSocketRestApiRequest request) {
        String path = request.getPath();
        if (!this.requestValidator.isValidUri(path)) {
            return String.format("Invalid path: '%s'", path);
        }

        if (!METHOD_WHITELIST.contains(request.getMethod())) {
            return String.format("Method not supported. Supported methods are: %s.", METHOD_WHITELIST);

        }

        return null;
    }
}

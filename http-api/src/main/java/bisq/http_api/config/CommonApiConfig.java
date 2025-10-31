package bisq.http_api.config;

import lombok.Getter;

import java.util.List;

@Getter
public abstract class CommonApiConfig {
    public static final String REST_API_BASE_PATH = "/api/v1";

    private final boolean enabled;
    private final String protocol;
    private final String host;
    private final int port;
    private final boolean localhostOnly;
    private final List<String> whiteListEndPoints;
    private final List<String> blackListEndPoints;
    private final List<String> supportedAuth;
    private final String password;
    private final String restApiBaseAddress;
    private final String restApiBaseUrl;

    public CommonApiConfig(boolean enabled,
                           String protocol,
                           String host,
                           int port,
                           boolean localhostOnly,
                           List<String> whiteListEndPoints,
                           List<String> blackListEndPoints,
                           List<String> supportedAuth,
                           String password) {
        this.enabled = enabled;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.localhostOnly = localhostOnly;
        this.whiteListEndPoints = whiteListEndPoints;
        this.blackListEndPoints = blackListEndPoints;
        this.supportedAuth = supportedAuth;
        this.password = password;

        restApiBaseAddress = protocol + host + ":" + port;
        restApiBaseUrl = restApiBaseAddress + REST_API_BASE_PATH;
    }
}
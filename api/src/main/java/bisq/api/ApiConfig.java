/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api;

import bisq.api.access.permissions.Permission;
import bisq.api.access.transport.ApiAccessTransportType;
import com.typesafe.config.Config;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
public final class ApiConfig {
    public static final String REST_API_BASE_PATH = "/api/v1";

    private final ApiAccessTransportType apiAccessTransportType;
    private final boolean writePairingQrCodeToDisk;
    // api.server.*
    private final boolean restEnabled;
    private final boolean websocketEnabled;

    // api.server.bind.*
    private final String bindHost;
    private final int bindPort;

    //  api.server.tor
    private final int onionServicePort;
    private final boolean torClientAuthRequired;

    // api.server.tls.*
    private final boolean tlsRequired;
    private final String tlsKeyStorePassword;
    private final List<String> tlsKeyStoreSan;

    // api.server.security.*
    private final boolean supportSessionHandling;
    private final boolean authorizationRequired;

    // api.server.security.session.*
    private final int sessionTtlInMinutes;

    // TODO This should move to typesafe config
    private final Set<Permission> grantedPermissions = Set.of(Permission.values());

    public ApiConfig(
            ApiAccessTransportType apiAccessTransportType,
            boolean writePairingQrCodeToDisk,
            boolean restEnabled,
            boolean websocketEnabled,
            String bindHost,
            int bindPort,
            int onionServicePort,
            boolean supportSessionHandling,
            boolean authorizationRequired,
            boolean tlsRequired,
            String tlsKeyStorePassword,
            List<String> tlsKeyStoreSan,
            boolean torClientAuthRequired,
            int sessionTtlInMinutes
    ) {
        this.apiAccessTransportType = apiAccessTransportType;
        this.writePairingQrCodeToDisk = writePairingQrCodeToDisk;
        this.restEnabled = restEnabled;
        this.websocketEnabled = websocketEnabled;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.onionServicePort = onionServicePort;
        this.supportSessionHandling = supportSessionHandling;
        this.authorizationRequired = authorizationRequired;
        this.tlsRequired = tlsRequired;
        this.tlsKeyStorePassword = tlsKeyStorePassword;
        this.tlsKeyStoreSan = tlsKeyStoreSan;
        this.torClientAuthRequired = torClientAuthRequired;
        this.sessionTtlInMinutes = sessionTtlInMinutes;
    }

    public static ApiConfig from(Config config) {
        ApiAccessTransportType apiAccessTransportType = ApiAccessTransportType.valueOf(
                config.getString("accessTransportType").toUpperCase());

        Config serverConfig = config.getConfig("server");
        Config bindConfig = serverConfig.getConfig("bind");
        Config torConfig = serverConfig.getConfig("tor");
        Config securityConfig = serverConfig.getConfig("security");

        Config tlsConfig = serverConfig.getConfig("tls");
        Config keystoreConfig = tlsConfig.getConfig("keystore");
        Config certificateConfig = tlsConfig.getConfig("certificate");

        Config sessionSecurity = securityConfig.getConfig("session");

        return new ApiConfig(
                apiAccessTransportType,

                config.getBoolean("writePairingQrCodeToDisk"),

                serverConfig.getBoolean("restEnabled"),
                serverConfig.getBoolean("websocketEnabled"),

                bindConfig.getString("host"),
                bindConfig.getInt("port"),

                torConfig.getInt("onionServicePort"),

                securityConfig.getBoolean("supportSessionHandling"),
                securityConfig.getBoolean("authorizationRequired"),

                tlsConfig.getBoolean("required"),
                keystoreConfig.getString("password"),
                certificateConfig.getStringList("san"),

                torConfig.getBoolean("clientAuthRequired"),

                sessionSecurity.getInt("ttlInMinutes")
        );
    }

    public String getRestServerUrl() {
        return getRestProtocol() + "://" + bindHost + ":" + bindPort;
    }

    public String getRestServerApiBasePath() {
        return getRestServerUrl() + REST_API_BASE_PATH;
    }

    public String getWebSocketServerUrl() {
        return getWebSocketProtocol() + "://" + bindHost + ":" + bindPort;
    }

    public String getRestProtocol() {
        return tlsRequired ? "https" : "http";
    }

    public String getWebSocketProtocol() {
        return tlsRequired ? "wss" : "ws";
    }

    public boolean isEnabled() {
        return websocketEnabled || restEnabled;
    }

    public boolean useTor() {
        return apiAccessTransportType == ApiAccessTransportType.TOR;
    }
}


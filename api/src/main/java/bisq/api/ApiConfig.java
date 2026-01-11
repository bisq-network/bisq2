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

import bisq.api.access.transport.ApiAccessTransportType;
import com.typesafe.config.Config;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public final class ApiConfig {
    public static final String REST_API_BASE_PATH = "/api/v1";

    private final ApiAccessTransportType apiAccessTransportType;

    // api.server.*
    private final boolean restEnabled;
    private final boolean websocketEnabled;

    // api.server.bind.*
    private final String bindHost;
    private final int bindPort;

    // api.server.security.*
    private final boolean authRequired;
    private final boolean tlsRequired;
    private final boolean torClientAuthRequired;

    // api.server.security.rest.*
    private final Optional<List<String>> restAllowEndpoints;
    private final List<String> restDenyEndpoints;

    // api.server.security.websocket.*
    private final Optional<List<String>> websocketAllowEndpoints;
    private final List<String> websocketDenyEndpoints;

    public ApiConfig(
            ApiAccessTransportType apiAccessTransportType,
            boolean restEnabled,
            boolean websocketEnabled,
            String bindHost,
            int bindPort,
            boolean authRequired,
            boolean tlsRequired,
            boolean torClientAuthRequired,
            Optional<List<String>> restAllowEndpoints,
            List<String> restDenyEndpoints,
            Optional<List<String>> websocketAllowEndpoints,
            List<String> websocketDenyEndpoints
    ) {
        this.apiAccessTransportType = apiAccessTransportType;
        this.restEnabled = restEnabled;
        this.websocketEnabled = websocketEnabled;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.authRequired = authRequired;
        this.tlsRequired = tlsRequired;
        this.torClientAuthRequired = torClientAuthRequired;
        this.restAllowEndpoints = restAllowEndpoints;
        this.restDenyEndpoints = restDenyEndpoints;
        this.websocketAllowEndpoints = websocketAllowEndpoints;
        this.websocketDenyEndpoints = websocketDenyEndpoints;
    }

    public static ApiConfig from(Config config) {
        ApiAccessTransportType apiAccessTransportType = ApiAccessTransportType.valueOf(
                config.getString("accessTransportType").toUpperCase());

        Config serverConfig = config.getConfig("server");
        Config bindConfig = serverConfig.getConfig("bind");
        Config securityConfig = serverConfig.getConfig("security");

        Config restSecurity = securityConfig.getConfig("rest");
        Config websocketSecurity = securityConfig.getConfig("websocket");

        // use Optional instead of null
        Optional<List<String>> restAllow = restSecurity.hasPath("allowEndpoints")
                ? Optional.of(restSecurity.getStringList("allowEndpoints"))
                : Optional.empty();
        List<String> restDeny = restSecurity.hasPath("denyEndpoints")
                ? restSecurity.getStringList("denyEndpoints")
                : Collections.emptyList();

        Optional<List<String>> wsAllow = websocketSecurity.hasPath("allowEndpoints")
                ? Optional.of(websocketSecurity.getStringList("allowEndpoints"))
                : Optional.empty();
        List<String> wsDeny = websocketSecurity.hasPath("denyEndpoints")
                ? websocketSecurity.getStringList("denyEndpoints")
                : Collections.emptyList();

        return new ApiConfig(
                apiAccessTransportType,

                serverConfig.getBoolean("restEnabled"),
                serverConfig.getBoolean("websocketEnabled"),

                bindConfig.getString("host"),
                bindConfig.getInt("port"),

                securityConfig.getBoolean("authRequired"),
                securityConfig.getBoolean("tlsRequired"),
                securityConfig.getBoolean("torClientAuthRequired"),

                restAllow,
                restDeny,
                wsAllow,
                wsDeny
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


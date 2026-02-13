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

package bisq.http_api.config;

import lombok.Getter;

import java.util.List;

@Getter
public abstract class CommonApiConfig {
    public static final String REST_API_BASE_PATH = "/api/v1";

    private final boolean enabled;
    private final String protocol;
    private final String host;
    private final String bindHost;
    private final int port;
    private final boolean localhostOnly;
    private final List<String> whiteListEndPoints;
    private final List<String> blackListEndPoints;
    private final List<String> supportedAuth;
    private final String password;
    private final String restApiBaseAddress;
    private final String restApiBaseUrl;
    private final String restApiBindUrl;
    private final boolean publishOnionService;
    private final boolean tlsRequired;
    private final String tlsKeyStorePassword;
    private final List<String> tlsKeyStoreSan;

    public CommonApiConfig(boolean enabled,
                           String protocol,
                           String host,
                           int port,
                           boolean localhostOnly,
                           List<String> whiteListEndPoints,
                           List<String> blackListEndPoints,
                           List<String> supportedAuth,
                           String password,
                           boolean publishOnionService,
                           boolean tlsRequired,
                           String tlsKeyStorePassword,
                           List<String> tlsKeyStoreSan) {
        this.enabled = enabled;
        // Auto-correct protocol when TLS is required to prevent misconfiguration
        this.protocol = tlsRequired && protocol.startsWith("http://") ? "https://" : protocol;
        this.host = host;
        // SECURITY: When localhostOnly=false, we bind to 0.0.0.0 (all network interfaces) to allow
        // LAN mobile devices to connect. This exposes the API beyond localhost, including LAN, VPN,
        // and potentially public WiFi interfaces.
        //
        // This is acceptable because:
        // 1. The default is localhostOnly=true in all shipped configs (opt-in only)
        // 2. Pairing requires a QR code with a single-use, time-limited code (5-min TTL)
        // 3. All API access after pairing requires session token authentication
        // 4. TLS with certificate fingerprint pinning prevents MITM when enabled
        // 5. Granular permissions limit what paired clients can do
        //
        // Users enabling localhostOnly=false SHOULD also enable tlsRequired=true to prevent
        // cleartext session token interception on shared networks.
        //
        // Binding to a specific LAN IP was considered but rejected: it's fragile with DHCP,
        // multiple interfaces, and network changes. 0.0.0.0 is standard for LAN services.
        this.bindHost = localhostOnly ? host : "0.0.0.0";
        this.port = port;
        this.localhostOnly = localhostOnly;
        this.whiteListEndPoints = whiteListEndPoints;
        this.blackListEndPoints = blackListEndPoints;
        this.supportedAuth = supportedAuth;
        this.password = password;
        this.publishOnionService = publishOnionService;
        this.tlsRequired = tlsRequired;
        this.tlsKeyStorePassword = tlsKeyStorePassword;
        this.tlsKeyStoreSan = tlsKeyStoreSan;

        restApiBaseAddress = protocol + host + ":" + port;
        restApiBaseUrl = restApiBaseAddress + REST_API_BASE_PATH;
        restApiBindUrl = protocol + bindHost + ":" + port + REST_API_BASE_PATH;
    }
}
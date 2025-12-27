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
    private final int port;
    private final boolean localhostOnly;
    private final List<String> whiteListEndPoints;
    private final List<String> blackListEndPoints;
    private final List<String> supportedAuth;
    private final String password;
    private final String restApiBaseAddress;
    private final String restApiBaseUrl;
    private final boolean publishOnionService;

    public CommonApiConfig(boolean enabled,
                           String protocol,
                           String host,
                           int port,
                           boolean localhostOnly,
                           List<String> whiteListEndPoints,
                           List<String> blackListEndPoints,
                           List<String> supportedAuth,
                           String password,
                           boolean publishOnionService) {
        this.enabled = enabled;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.localhostOnly = localhostOnly;
        this.whiteListEndPoints = whiteListEndPoints;
        this.blackListEndPoints = blackListEndPoints;
        this.supportedAuth = supportedAuth;
        this.password = password;
        this.publishOnionService = publishOnionService;

        restApiBaseAddress = protocol + host + ":" + port;
        restApiBaseUrl = restApiBaseAddress + REST_API_BASE_PATH;
    }
}
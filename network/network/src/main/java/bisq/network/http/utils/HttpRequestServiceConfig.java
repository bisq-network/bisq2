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

package bisq.network.http.utils;

import bisq.common.network.TransportType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
public class HttpRequestServiceConfig {
    public static HttpRequestServiceConfig from(com.typesafe.config.Config typesafeConfig) {
        long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
        Set<HttpRequestUrlProvider> providers = typesafeConfig.getConfigList("providers").stream()
                .map(config -> {
                    String url = config.getString("url");
                    String operator = config.getString("operator");
                    TransportType transportType = getTransportTypeFromUrl(url);
                    return new HttpRequestUrlProvider(url, operator, transportType);
                })
                .collect(Collectors.toUnmodifiableSet());

        Set<HttpRequestUrlProvider> fallbackProviders = typesafeConfig.getConfigList("fallbackProviders").stream()
                .map(config -> {
                    String url = config.getString("url");
                    String operator = config.getString("operator");
                    TransportType transportType = getTransportTypeFromUrl(url);
                    return new HttpRequestUrlProvider(url, operator, transportType);
                })
                .collect(Collectors.toUnmodifiableSet());
        return new HttpRequestServiceConfig(timeoutInSeconds, providers, fallbackProviders);
    }

    public static TransportType getTransportTypeFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                if (host.endsWith(".i2p")) {
                    return TransportType.I2P;
                } else if (host.endsWith(".onion")) {
                    return TransportType.TOR;
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse URL for transport type detection: {}", url);
        }
        return TransportType.CLEAR;
    }


    private final Set<? extends HttpRequestUrlProvider> providers;
    private final Set<? extends HttpRequestUrlProvider> fallbackProviders;
    private final long timeoutInSeconds;

    public HttpRequestServiceConfig(long timeoutInSeconds,
                                    Set<? extends HttpRequestUrlProvider> providers,
                                    Set<? extends HttpRequestUrlProvider> fallbackProviders) {
        this.timeoutInSeconds = timeoutInSeconds;
        this.providers = providers;
        this.fallbackProviders = fallbackProviders;
    }
}

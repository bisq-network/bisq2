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

package bisq.network.http;

import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.network.http.utils.Socks5ProxyProvider;
import bisq.network.p2p.node.transport.I2PTransportService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates HTTP clients for each transport, using cached I2P proxy selection with TTL-based health checks.
 */
@Slf4j
public class HttpClientsByTransport {
    private final AtomicInteger counter = new AtomicInteger();
    private final Map<TransportType, TransportConfig> configByTransportType;

    public HttpClientsByTransport(Map<TransportType, TransportConfig> configByTransportType) {
        this.configByTransportType = configByTransportType;
    }

    public BaseHttpClient getHttpClient(String url,
                                        String userAgent,
                                        TransportType transportType) {
        return getHttpClient(url, userAgent, transportType, Optional.empty(), Optional.empty());
    }

    public BaseHttpClient getHttpClient(String url,
                                        String userAgent,
                                        TransportType transportType,
                                        Optional<Socks5Proxy> socksProxy,
                                        Optional<String> socks5ProxyAddress) {
        return switch (transportType) {
            case TOR -> {
                Socks5ProxyProvider socks5ProxyProvider = socks5ProxyAddress
                        .map(Socks5ProxyProvider::new)
                        .orElseGet(() -> socksProxy.map(Socks5ProxyProvider::new)
                                .orElseThrow(() -> new RuntimeException("No socks5ProxyAddress provided and no Tor socksProxy available.")));
                yield new TorHttpClient(url, userAgent, socks5ProxyProvider);
            }

            case I2P -> {
                I2PTransportService.Config config = (I2PTransportService.Config) configByTransportType.get(TransportType.I2P);
                Address address = new ClearnetAddress(config.getHttpProxyHost(), config.getHttpProxyPort());
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address.getHost(), address.getPort()));
                yield new ClearNetHttpClient(url, userAgent, proxy);
            }

            case CLEAR -> new ClearNetHttpClient(url, userAgent);
        };
    }
}

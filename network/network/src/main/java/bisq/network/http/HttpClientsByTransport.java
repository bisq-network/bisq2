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

import bisq.common.network.TransportType;
import bisq.network.http.utils.Socks5ProxyProvider;
import bisq.network.p2p.node.transport.I2PTransportService;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates HTTP clients for each transport, using cached I2P proxy selection with TTL-based health checks.
 */
public class HttpClientsByTransport {
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final List<I2PTransportService.ProxyEndpoint> proxyList;
    private final AtomicInteger counter = new AtomicInteger();

    private static final class CacheEntry {
        final I2PTransportService.ProxyEndpoint endpoint;
        final Instant expiry;

        CacheEntry(I2PTransportService.ProxyEndpoint ep, Instant expiry) {
            this.endpoint = ep;
            this.expiry = expiry;
        }
    }

    private volatile CacheEntry cache = null;

    /**
     * @param i2pConfig loaded from NetworkServiceConfig for TransportType.I2P
     */
    public HttpClientsByTransport(I2PTransportService.Config i2pConfig) {
        this.proxyList = i2pConfig == null ? List.of() : List.copyOf(i2pConfig.getProxyList());
        if (proxyList.isEmpty() && i2pConfig != null) {
            throw new IllegalArgumentException("I2P proxyList must not be empty");
        }
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
                        .orElse(socksProxy.map(Socks5ProxyProvider::new)
                                .orElseThrow(() -> new RuntimeException("No socks5ProxyAddress provided and no Tor socksProxy available.")));
                yield new TorHttpClient(url, userAgent, socks5ProxyProvider);
                // If we have a socks5ProxyAddress defined in options we use that as proxy
            }

            case I2P -> {
                // Get a healthy proxy endpoint with TTL caching
                I2PTransportService.ProxyEndpoint ep = getHealthyEndpoint();
                Proxy proxy = new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress(ep.getHost(), ep.getPort()));
                yield new ClearNetHttpClient(url, userAgent, proxy);
            }

            case CLEAR -> new ClearNetHttpClient(url, userAgent);
        };
    }

    private I2PTransportService.ProxyEndpoint getHealthyEndpoint() {
        Instant now = Instant.now();
        CacheEntry entry = cache;
        if (entry != null && now.isBefore(entry.expiry)) {
            return entry.endpoint;
        }
        // TTL expired or not set: probe next endpoints in round-robin order
        int size = proxyList.size();
        for (int i = 0; i < size; i++) {
            int idx = Math.floorMod(counter.getAndIncrement(), size);
            I2PTransportService.ProxyEndpoint candidate = proxyList.get(idx);
            if (isReachable(candidate, 500)) {
                cache = new CacheEntry(candidate, now.plus(CACHE_TTL));
                return candidate;
            }
        }
        throw new RuntimeException("No reachable I2P proxy available");
    }

    private boolean isReachable(I2PTransportService.ProxyEndpoint ep, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ep.getHost(), ep.getPort()), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

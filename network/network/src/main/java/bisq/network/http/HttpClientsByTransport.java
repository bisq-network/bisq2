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
import java.util.List;
import java.util.Optional;

public class HttpClientsByTransport {
    private final I2PTransportService.Config i2pConfig;

    public HttpClientsByTransport(I2PTransportService.Config i2pConfig) {
        this.i2pConfig = i2pConfig;
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
                // Try each configured HTTP proxy endpoint for I2P
                Proxy proxy = selectWorkingProxy(i2pConfig.getProxyList());
                yield new ClearNetHttpClient(url, userAgent, proxy);
            }
            case CLEAR -> new ClearNetHttpClient(url, userAgent);
        };
    }

    private Proxy selectWorkingProxy(List<I2PTransportService.ProxyEndpoint> endpoints) {
        for (I2PTransportService.ProxyEndpoint ep : endpoints) {
            Proxy p = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(ep.getHost(), ep.getPort()));
            if (isReachable(p, 500)) {
                return p;
            }
        }
        throw new RuntimeException("No reachable I2P proxy available");
    }

    private boolean isReachable(Proxy proxy, int timeoutMs) {
        try (Socket s = new Socket()) {
            InetSocketAddress addr = (InetSocketAddress) proxy.address();
            s.connect(addr, timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

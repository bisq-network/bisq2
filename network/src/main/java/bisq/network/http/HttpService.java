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

import bisq.network.http.common.BaseHttpClient;
import bisq.network.http.common.ClearNetHttpClient;
import bisq.network.http.common.Socks5ProxyProvider;
import bisq.network.http.common.TorHttpClient;
import bisq.network.p2p.node.transport.Transport;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HttpService {
    public HttpService() {
    }

    public BaseHttpClient getHttpClient(String url,
                                        String userAgent,
                                        Transport.Type transportType,
                                        Optional<Socks5Proxy> socksProxy,
                                        Optional<String> socks5ProxyAddress) {
        switch (transportType) {
            case TOR:
                // If we have a socks5ProxyAddress defined in options we use that as proxy
                Socks5ProxyProvider socks5ProxyProvider = socks5ProxyAddress
                        .map(Socks5ProxyProvider::new)
                        .orElse(socksProxy.map(Socks5ProxyProvider::new)
                                .orElseThrow(() -> new RuntimeException("No socks5ProxyAddress provided and no Tor socksProxy available.")));
                return new TorHttpClient(url, userAgent, socks5ProxyProvider);
            case I2P:
                // The I2P router exposes a local HTTP proxy on port 4444 for I2P destinations
                // Note: only works with external I2P router (embedded one doesn't provide this proxy by default)
                return new ClearNetHttpClient(url, userAgent,
                        new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 4444))
                );
            case CLEAR:
                return new ClearNetHttpClient(url, userAgent);
            default:
                throw new IllegalArgumentException("Providers network type not recognized. " + transportType);
        }
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }
}
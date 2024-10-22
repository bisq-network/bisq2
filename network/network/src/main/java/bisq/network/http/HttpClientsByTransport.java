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
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Optional;

public class HttpClientsByTransport {
    public HttpClientsByTransport() {
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
            case I2P ->
                // The I2P router exposes a local HTTP proxy on port 4444 for I2P destinations
                // Note: only works with external I2P router (embedded one doesn't provide this proxy by default)
                    new ClearNetHttpClient(url, userAgent,
                            new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 4444))
                    );
            case CLEAR -> new ClearNetHttpClient(url, userAgent);
        };
    }
}
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

package network.misq.network.http;

import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import network.misq.network.http.common.BaseHttpClient;
import network.misq.network.http.common.ClearNetHttpClient;
import network.misq.network.http.common.Socks5ProxyProvider;
import network.misq.network.http.common.TorHttpClient;
import network.misq.network.p2p.node.transport.Transport;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HttpService {
    public CompletableFuture<BaseHttpClient> getHttpClient(String url,
                                                           String userAgent,
                                                           Transport.Type transportType,
                                                           Optional<Socks5Proxy> socksProxy,
                                                           Optional<String> socks5ProxyAddress) {
        return CompletableFuture.supplyAsync(() -> {
            switch (transportType) {
                case TOR:
                    // If we have a socks5ProxyAddress defined in options we use that as proxy
                    Socks5ProxyProvider socks5ProxyProvider = socks5ProxyAddress
                            .map(Socks5ProxyProvider::new)
                            .orElse(socksProxy.map(Socks5ProxyProvider::new)
                                    .orElseThrow(() -> new RuntimeException("No socks5ProxyAddress provided and no Tor socksProxy available.")));
                    return new TorHttpClient(url, userAgent, socks5ProxyProvider);
                case I2P:
                    // TODO We need to figure out how to get a proxy from i2p or require tor in any case
                    throw new IllegalArgumentException("I2P providers not supported yet.");
                case CLEAR_NET:
                    return new ClearNetHttpClient(url, userAgent);
                default:
                    throw new IllegalArgumentException("Providers network type not recognized. " + transportType);
            }
        });
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }
}
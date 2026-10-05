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

package bisq.network.tor;

import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

public class TorSocksProxyFactory {
    private final int socksPort;

    public TorSocksProxyFactory(int socksPort) {
        this.socksPort = socksPort;
    }

    public Socket getSocket() throws IOException {
        Proxy proxy = getProxy();
        return new Socket(proxy);
    }

    public Socks5Proxy getSocks5Proxy() throws IOException {
        Socks5Proxy socks5Proxy = new Socks5Proxy("127.0.0.1", socksPort);
        socks5Proxy.resolveAddrLocally(false);
        return socks5Proxy;
    }

    private Proxy getProxy() throws IOException {
        Socks5Proxy socks5Proxy = getSocks5Proxy();
        InetSocketAddress socketAddress = new InetSocketAddress(socks5Proxy.getInetAddress(), socks5Proxy.getPort());
        return new Proxy(Proxy.Type.SOCKS, socketAddress);
    }
}

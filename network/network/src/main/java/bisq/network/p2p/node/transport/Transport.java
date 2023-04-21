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

package bisq.network.p2p.node.transport;

import bisq.network.p2p.node.Address;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Transport {
    /**
     * We do not use a protobuf enum for Type as it is used as key in a protobuf map and that does not support enums.
     */
    enum Type {
        TOR,
        I2P,
        CLEAR;

        public static Type from(Address address) {
            if (address.isClearNetAddress()) {
                return Type.CLEAR;
            } else if (address.isTorAddress()) {
                return Type.TOR;
            } else if (address.isI2pAddress()) {
                return Type.I2P;
            } else {
                throw new IllegalArgumentException("Could not resolve transportType from address " + address);
            }
        }
    }

    interface Config {
        String getBaseDir();

        int getSocketTimeout();
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    final class ServerSocketChannelResult {
        private final String nodeId;
        private final ServerSocketChannel serverSocketChannel;
        private final Address address;

        public ServerSocketChannelResult(String nodeId, ServerSocketChannel serverSocketChannel, Address address) {
            this.nodeId = nodeId;
            this.serverSocketChannel = serverSocketChannel;
            this.address = address;
        }
    }

    boolean initialize();

    CompletableFuture<ServerSocketChannelResult> getServerSocketChannel(int port, String nodeId);

    default Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.empty();
    }

    Optional<Address> getServerAddress(String serverId);

    boolean isAddressAvailable(Address address);

    CompletableFuture<Void> shutdown();
}

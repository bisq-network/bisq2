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

package bisq.network.i2p;

import bisq.network.i2p.exceptions.CannotConnectToI2pDestination;
import bisq.network.i2p.exceptions.I2pDestinationOffline;
import bisq.network.i2p.exceptions.InvalidI2pDestination;
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PException;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.function.Consumer;

@Slf4j
public class I2pClient {
    private final I2PSocketManager i2pSocketManager;

    public I2pClient(I2PSocketManager i2pSocketManager) {
        this.i2pSocketManager = i2pSocketManager;
    }

    public void connect(String base64Destination, Consumer<I2PSocket> socketConsumer) {
        Destination destination = createDestination(base64Destination);
        @SuppressWarnings("resource")
        I2PSocket socket = connect(destination);
        new Thread(() -> socketConsumer.accept(socket)).start();
    }

    private Destination createDestination(String base64Destination) {
        try {
            return new Destination(base64Destination);
        } catch (DataFormatException e) {
            throw new InvalidI2pDestination(e);
        }
    }

    private I2PSocket connect(Destination destination) {
        try {
            return i2pSocketManager.connect(destination);
        } catch (I2PException | ConnectException | InterruptedIOException e) {
            log.warn("Couldn't connect to destination \"{}\"", destination, e);
            throw new CannotConnectToI2pDestination(destination);
        } catch (NoRouteToHostException ex) {
            log.warn("I2P destination \"{}\" is offline.", destination);
            throw new I2pDestinationOffline(destination);
        }
    }
}

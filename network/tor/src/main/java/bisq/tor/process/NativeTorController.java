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

package bisq.tor.process;

import net.freehaven.tor.control.PasswordDigest;
import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class NativeTorController {

    private Optional<TorControlConnection> torControlConnection = Optional.empty();

    public void connect(int controlPort, PasswordDigest controlConnectionSecret) throws IOException {
        var controlSocket = new Socket("127.0.0.1", controlPort);
        var controlConnection = new TorControlConnection(controlSocket);
        controlConnection.launchThread(true);
        controlConnection.authenticate(controlConnectionSecret.getSecret());
        torControlConnection = Optional.of(controlConnection);
    }

    public void takeOwnership() throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controlConnection.takeOwnership();
    }

    public void shutdown() throws IOException {
        TorControlConnection controlConnection = torControlConnection.orElseThrow();
        controlConnection.shutdownTor("SHUTDOWN");
    }
}

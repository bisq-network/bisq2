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

package network.misq.common.util;

import com.google.common.net.InetAddresses;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

public class NetworkUtils {
    public static int findFreeSystemPort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException ignored) {
            return new Random().nextInt(10000) + 50000;
        }
    }

    public static boolean isClearNetAddress(String host) {
        return InetAddresses.isInetAddress(host);
    }

    public static boolean isTorAddress(String host) {
        return host.endsWith(".onion");
    }

    public static boolean isI2pAddress(String host) {
        //TODO
        return !isClearNetAddress(host) && !isTorAddress(host);
    }
}

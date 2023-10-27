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

package bisq.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class NetworkUtils {
    private static final Set<Integer> USED = new CopyOnWriteArraySet<>();

    public static int findFreeSystemPort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            if (USED.contains(port)) {
                try {
                    log.warn("We had already used port {}. We try again after a short break.", port);
                    Thread.sleep(20);
                } catch (InterruptedException ignore) {
                }
                return findFreeSystemPort();
            } else {
                USED.add(port);
                return port;
            }
        } catch (IOException ignored) {
            return new Random().nextInt(10000) + 50000;
        }
    }

    public static int selectRandomPort() {
        return new Random().nextInt(65_536); // 2^16
    }

    public static boolean isPortInUse(String host, int port) {
        try (var tmp = new Socket(host, port)) {
            // Successful connection means the port is taken
            tmp.close();
            return true;
        } catch (IOException e) {
            // Could not connect
            return false;
        }
    }
}

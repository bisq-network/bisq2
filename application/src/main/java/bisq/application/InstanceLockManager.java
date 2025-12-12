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

package bisq.application;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;

@Slf4j
public class InstanceLockManager {
    private ServerSocket lockSocket;

    public synchronized void acquireLock(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (lockSocket != null) {
            throw new IllegalStateException("Lock already acquired");
        }
        try {
            lockSocket = new ServerSocket(port);
            lockSocket.setReuseAddress(false);
        } catch (IOException e) {
            String errorMessage = "Another Bisq instance is already running (port " + port + " already in use).";
            log.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    public synchronized void releaseLock() {
        try {
            if (lockSocket != null) {
                lockSocket.close();
                lockSocket = null;
            }
        } catch (IOException e) {
            log.error("Failed to close lockSocket", e);
        }
    }
}

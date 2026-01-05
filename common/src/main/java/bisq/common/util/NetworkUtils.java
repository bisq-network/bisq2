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

import bisq.common.data.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;

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
                } catch (InterruptedException e) {
                    log.warn("Thread got interrupted at findFreeSystemPort method", e);
                    Thread.currentThread().interrupt(); // Restore interrupted state
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

    public static List<NetworkInterface> findNetworkInterfaces() {
        return getNetworkInterfaceHostAddressPairs().stream()
                .map(Pair::getFirst)
                .distinct()
                .toList();
    }

    public static List<Pair<NetworkInterface, String>> getNetworkInterfaceHostAddressPairs() {
        List<Pair<NetworkInterface, String>> networkInterfaceHostAddressPairs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Ignore down, loopback and virtual interfaces
                if (!networkInterface.isUp() ||
                        networkInterface.isLoopback() ||
                        networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    // isSiteLocalAddress ensures that we return only LAN addresses (10.x.x.x, 192.168.x.x, 172.16.x.x–172.31.x.x)
                    if (inetAddress instanceof Inet4Address && inetAddress.isSiteLocalAddress()) {
                        // return Optional.of(inetAddress.getHostAddress());
                        networkInterfaceHostAddressPairs.add(new Pair<>(networkInterface, inetAddress.getHostAddress()));
                    }
                }
            }
        } catch (SocketException socketException) {
            log.error("Could not access network interfaces", socketException);
        }

        if (networkInterfaceHostAddressPairs.isEmpty()) {
            // Not expected but possible (machine is not connected to any Ethernet, Wi-Fi, or VPN,
            // sandboxes or restricted environments like Android emulator, Docker,...)
            log.warn("No IPv4 LAN addresses found");
        }

        return networkInterfaceHostAddressPairs;
    }

    public static Optional<String> findLANHostAddress(Optional<NetworkInterface> preferredNetworkInterface) {
        List<Pair<NetworkInterface, String>> networkInterfaceHostAddressPairs = getNetworkInterfaceHostAddressPairs();
        if (preferredNetworkInterface.isPresent()) {
            Optional<String> preferred = networkInterfaceHostAddressPairs.stream()
                    .filter(pair ->
                            pair.getFirst().equals(preferredNetworkInterface.get()))
                    .map(Pair::getSecond)
                    .findFirst();
            if (preferred.isPresent()) {
                return preferred;
            } else {
                log.warn("Preferred network interface not found. Falling back to auto-selected LAN address.");
            }
        }
        return networkInterfaceHostAddressPairs.stream()
                .map(Pair::getSecond)
                .distinct()
                .min(comparingInt((String s) -> {
                    if (s.startsWith("192.168.")) return 0;
                    if (s.startsWith("10.")) return 1;
                    return 2;
                }).thenComparing(naturalOrder()));
    }
}

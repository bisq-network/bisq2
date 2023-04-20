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

package bisq.tor;

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.TorControlConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class TorServerSocketChannel {

    private final String hsDirPath;
    private final TorController torController;
    @Getter
    private Optional<ServerSocketChannel> localServerSocketChannel = Optional.empty();
    private Optional<OnionAddress> onionAddress = Optional.empty();

    public TorServerSocketChannel(String torDirPath, TorController torController) {
        this.hsDirPath = torDirPath + File.separator + Constants.HS_DIR;
        this.torController = torController;
    }

    public CompletableFuture<OnionAddress> start(int hiddenServicePort, String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int localPort = NetworkUtils.findFreeSystemPort();
                localServerSocketChannel = Optional.of(
                        createAndBindLocalServerSocketChannel(localPort)
                );

                return bind(hiddenServicePort, NetworkUtils.findFreeSystemPort(), id);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Couldn't start tor server socket channel.", e);
            }
        });
    }

    public void shutdown() {
        log.info("Close onionAddress={}", onionAddress);
        onionAddress.ifPresent(onionAddress -> {
            torController.removeHiddenServiceReadyListener(onionAddress.getServiceId());
            try {
                torController.destroyHiddenService(onionAddress.getServiceId());
            } catch (IOException ignore) {
            }
        });

        localServerSocketChannel.ifPresent(serverSocketChannel -> {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.error("Couldn't shutdown local tor server socket channel.", e);
            }
        });
    }

    private ServerSocketChannel createAndBindLocalServerSocketChannel(int port) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress socketAddress = new InetSocketAddress(
                InetAddress.getLocalHost(),
                port
        );
        serverSocketChannel.socket().bind(socketAddress);
        return serverSocketChannel;
    }

    private OnionAddress bind(int hiddenServicePort, int localPort, String id) throws IOException, InterruptedException {
        long ts = System.currentTimeMillis();
        File dir = new File(hsDirPath, id);
        File hostNameFile = new File(dir.getCanonicalPath(), Constants.HOSTNAME);
        File privKeyFile = new File(dir.getCanonicalPath(), Constants.PRIV_KEY);
        FileUtils.makeDirs(dir);

        TorControlConnection.CreateHiddenServiceResult result;
        if (privKeyFile.exists()) {
            String privateKey = FileUtils.readFromFile(privKeyFile);
            result = torController.createHiddenService(hiddenServicePort, localPort, privateKey);
        } else {
            result = torController.createHiddenService(hiddenServicePort, localPort);
        }

        if (!hostNameFile.exists()) {
            FileUtils.makeFile(hostNameFile);
        }
        String serviceId = result.serviceID;

        OnionAddress onionAddress = new OnionAddress(serviceId + ".onion", hiddenServicePort);
        FileUtils.writeToFile(onionAddress.getHost(), hostNameFile);
        this.onionAddress = Optional.of(onionAddress);

        if (!privKeyFile.exists()) {
            FileUtils.makeFile(privKeyFile);
        }
        FileUtils.writeToFile(result.privateKey, privKeyFile);

        log.debug("Start publishing hidden service {}", onionAddress);
        CountDownLatch latch = new CountDownLatch(1);
        torController.addHiddenServiceReadyListener(serviceId, () -> {
            log.info(">> TorServerSocket ready. Took {} ms", System.currentTimeMillis() - ts);
            latch.countDown();
        });

        latch.await();
        torController.removeHiddenServiceReadyListener(serviceId);
        return onionAddress;
    }

    public Optional<OnionAddress> getOnionAddress() {
        return onionAddress;
    }
}

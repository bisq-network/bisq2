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

package network.misq.tor;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.TorControlConnection;
import network.misq.common.util.FileUtils;
import network.misq.common.util.NetworkUtils;
import network.misq.common.util.ThreadingUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static network.misq.tor.Constants.*;

@Slf4j
public class TorServerSocket extends ServerSocket {

    private final String hsDirPath;
    private final TorController torController;

    private Optional<OnionAddress> onionAddress = Optional.empty();
    @Nullable
    private ExecutorService executor;

    public TorServerSocket(String torDirPath, TorController torController) throws IOException {
        this.hsDirPath = torDirPath + File.separator + HS_DIR;
        this.torController = torController;
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort) {
        return bindAsync(hiddenServicePort, "default");
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort, String id) {
        return bindAsync(hiddenServicePort, NetworkUtils.findFreeSystemPort(), id, getAndSetExecutor());
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort,
                                                     int localPort,
                                                     String id,
                                                     @Nullable Executor executor) {
        CompletableFuture<OnionAddress> future = new CompletableFuture<>();
        if (executor == null) {
            executor = MoreExecutors.directExecutor();
        }
        executor.execute(() -> {
            Thread.currentThread().setName("TorServerSocket.bind");
            try {
                bind(hiddenServicePort, localPort, id);
                checkArgument(onionAddress.isPresent(), "onionAddress must be present");
                future.complete(onionAddress.get());
            } catch (IOException | InterruptedException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    // Blocking
    public void bind(int hiddenServicePort, int localPort, String id) throws IOException, InterruptedException {
        log.debug("Start bind TorServerSocket");
        long ts = System.currentTimeMillis();
        File dir = new File(hsDirPath, id);
        File hostNameFile = new File(dir.getCanonicalPath(), HOSTNAME);
        File privKeyFile = new File(dir.getCanonicalPath(), PRIV_KEY);
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
            try {
                super.bind(new InetSocketAddress(LOCALHOST, localPort));
                log.info(">> TorServerSocket ready. Took {} ms", System.currentTimeMillis() - ts);
                latch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        latch.await();
        torController.removeHiddenServiceReadyListener(serviceId);
    }

    @Override
    public void close() throws IOException {
        super.close();

        onionAddress.ifPresent(onionAddress -> {
            torController.removeHiddenServiceReadyListener(onionAddress.getServiceId());
            try {
                torController.destroyHiddenService(onionAddress.getServiceId());
            } catch (IOException ignore) {
            }
        });
        if (executor != null) {
            ThreadingUtils.shutdownAndAwaitTermination(executor);
        }
    }

    public Optional<OnionAddress> getOnionAddress() {
        return onionAddress;
    }

    private ExecutorService getAndSetExecutor() {
        executor = ThreadingUtils.getSingleThreadExecutor("TorServerSocket.bindAsync");
        return executor;
    }
}

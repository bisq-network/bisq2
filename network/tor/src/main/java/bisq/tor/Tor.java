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
import com.runjva.sourceforge.jsocks.protocol.Authentication;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import com.runjva.sourceforge.jsocks.protocol.SocksSocket;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.net.SocketFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;

/**
 * Open TODO:
 * - check <a href="https://github.com/ACINQ/Tor_Onion_Proxy_Library">...</a>
 * - support external running tor instance (external tor mode in netlayer)
 * - test use case with overriding existing torrc files
 * - test bridge and pluggable transports use cases
 * - test linux, windows
 * - check open TODOs
 * - test failure cases at start up (e.g. locked files, cookies remaining,...) specially on windows it seems that
 * current bisq has sometimes issues, we delete whole tor dir in those cases, but better to figure out when that
 * can happen.
 * <p>
 * Support for Android is not planned as long we do not target Android.
 */
@Slf4j
public class Tor {
    public static final String VERSION = "0.1.0";

    private static Optional<Tor> singletonInstance = Optional.empty();

    private final TorController torController;
    private final TorBootstrap torBootstrap;
    private final String torDirPath;
    private final TorContext torContext;
    private int proxyPort = -1;

    public static Tor getTor(String torDirPath, TorContext torContext) {
        if (singletonInstance.isPresent()) {
            return singletonInstance.get();
        }

        Tor tor = new Tor(torDirPath, torContext);
        singletonInstance = Optional.of(tor);
        return tor;
    }

    private Tor(String torDirPath, TorContext torContext) {
        this.torDirPath = torDirPath;
        torBootstrap = new TorBootstrap(torDirPath);
        this.torContext = torContext;
        torController = new TorController(torBootstrap.getCookieFile());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("Tor.shutdownHook");
            shutdown();
        }));
    }

    public CompletableFuture<Boolean> startAsync(ExecutorService executor) {
        return CompletableFuture.supplyAsync(
                () -> {
                    var retryPolicy = RetryPolicy.<Boolean>builder()
                            .handle(IllegalStateException.class)
                            .handleResultIf(result -> torContext.getState() == TorContext.State.STARTING)
                            .withBackoff(Duration.ofSeconds(3), Duration.ofSeconds(30))
                            .withJitter(0.25)
                            .withMaxDuration(Duration.ofMinutes(5)).withMaxRetries(30)
                            .onRetry(e -> log.info("Retry. AttemptCount={}.", e.getAttemptCount()))
                            .onRetriesExceeded(e -> {
                                log.warn("Failed. Max retries exceeded. We shutdown.");
                                shutdown();
                            })
                            .onSuccess(e -> log.debug("Succeeded."))
                            .build();

                    return Failsafe.with(retryPolicy).get(Tor.this::doStart);
                },
                executor
        );
    }

    public void shutdown() {
        TorContext.State previousState = torContext.getAndUpdateStateAtomically(
                state -> state.isStartingOrRunning() ? TorContext.State.STOPPING : state
        );

        if (!previousState.isStartingOrRunning()) {
            return;
        }

        log.info("Shutdown tor.");
        long ts = System.currentTimeMillis();

        torBootstrap.shutdown();
        torController.shutdown();

        log.info("Tor shutdown completed. Took {} ms.", System.currentTimeMillis() - ts); // Usually takes 20-40 ms
        torContext.setState(TorContext.State.TERMINATED);
    }

    private boolean doStart() {
        TorContext.State previousState = torContext.compareAndExchangeState(TorContext.State.NEW, TorContext.State.STARTING);
        switch (previousState) {
            case NEW: {
                boolean isSuccess = startTor();
                torContext.setState(TorContext.State.RUNNING);
                return isSuccess;
            }
            case STARTING: {
                throw new IllegalStateException("Already starting.");
            }
            case RUNNING: {
                log.debug("Got called while already running. We ignore that call.");
                return true;
            }
            case STOPPING:
            case TERMINATED:
                return false;
            default: {
                throw new IllegalStateException("Unhandled state " + torContext.getState());
            }
        }
    }

    private boolean startTor() {
        long ts = System.currentTimeMillis();
        try {
            int controlPort = torBootstrap.start();
            torController.start(controlPort);
            proxyPort = torController.getProxyPort();
        } catch (Exception exception) {
            torBootstrap.deleteVersionFile();
            log.error("Starting tor failed.", exception);
            shutdown();
            return false;
        }
        log.info(">> Starting Tor took {} ms", System.currentTimeMillis() - ts);
        return true;
    }

    public TorServerSocket getTorServerSocket() throws IOException {
        checkArgument(torContext.getState() == TorContext.State.RUNNING,
                "Invalid state at Tor.getTorServerSocket. state=" + torContext.getState());
        return new TorServerSocket(torDirPath, torController);
    }

    public Proxy getProxy(@Nullable String streamId) throws IOException {
        checkArgument(torContext.getState() == TorContext.State.RUNNING,
                "Invalid state at Tor.getProxy. state=" + torContext.getState());
        Socks5Proxy socks5Proxy = getSocks5Proxy(streamId);
        InetSocketAddress socketAddress = new InetSocketAddress(socks5Proxy.getInetAddress(), socks5Proxy.getPort());
        return new Proxy(Proxy.Type.SOCKS, socketAddress);
    }

    public Socket getSocket() throws IOException {
        return getSocket(null);
    }

    public Socket getSocket(@Nullable String streamId) throws IOException {
        Proxy proxy = getProxy(streamId);
        return new Socket(proxy);
    }

    public SocketFactory getSocketFactory(@Nullable String streamId) throws IOException {
        Proxy proxy = getProxy(streamId);
        return new TorSocketFactory(proxy);
    }

    public SocksSocket getSocksSocket(String remoteHost, int remotePort, @Nullable String streamId) throws IOException {
        Socks5Proxy socks5Proxy = getSocks5Proxy(streamId);
        SocksSocket socksSocket = new SocksSocket(socks5Proxy, remoteHost, remotePort);
        socksSocket.setTcpNoDelay(true);
        return socksSocket;
    }

    public Socks5Proxy getSocks5Proxy(@Nullable String streamId) throws IOException {
        checkArgument(torContext.getState() == TorContext.State.RUNNING,
                "Invalid state at Tor.getSocks5Proxy. state=" + torContext.getState());
        checkArgument(proxyPort > -1, "proxyPort must be defined");
        Socks5Proxy socks5Proxy = new Socks5Proxy(Constants.LOCALHOST, proxyPort);
        socks5Proxy.resolveAddrLocally(false);
        if (streamId == null) {
            return socks5Proxy;
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(streamId.getBytes());
            String asBase26 = new BigInteger(digest).toString(26);
            byte[] hash = asBase26.getBytes();
            // Authentication method ID 2 is User/Password
            //noinspection Convert2Lambda
            socks5Proxy.setAuthenticationMethod(2,
                    new Authentication() {
                        @Override
                        public Object[] doSocksAuthentication(int i, Socket socket) throws IOException {
                            // Must not close the streams here, as otherwise we get a socket closed
                            // exception at SocksSocket
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(new byte[]{(byte) 1, (byte) hash.length});
                            outputStream.write(hash);
                            outputStream.write(new byte[]{(byte) 1, (byte) 0});
                            outputStream.flush();

                            byte[] status = new byte[2];
                            InputStream inputStream = socket.getInputStream();
                            if (inputStream.read(status) == -1) {
                                throw new IOException("Did not get data");
                            }
                            if (status[1] != (byte) 0) {
                                throw new IOException("Authentication error: " + status[1]);
                            }
                            return new Object[]{inputStream, outputStream};
                        }
                    });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return socks5Proxy;
    }

    public Optional<String> getHostName(String serverId) {
        String fileName = torDirPath + separator + Constants.HS_DIR + separator + serverId + separator + "hostname";
        if (new File(fileName).exists()) {
            try {
                String host = FileUtils.readAsString(fileName);
                return Optional.of(host);
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }
        return Optional.empty();
    }

    public boolean isHiddenServiceAvailable(String onionUrl) {
        return torController.isHiddenServiceAvailable(onionUrl);
    }
}

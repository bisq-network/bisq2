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
import com.runjva.sourceforge.jsocks.protocol.Authentication;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import com.runjva.sourceforge.jsocks.protocol.SocksSocket;
import com.sun.jdi.Bootstrap;
import network.misq.common.util.ThreadingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static network.misq.tor.Constants.LOCALHOST;

/**
 * Open TODO:
 * - check https://github.com/ACINQ/Tor_Onion_Proxy_Library
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
public class Tor {
    private static final Logger log = LoggerFactory.getLogger(Tor.class);
    public static final String VERSION = "0.1.0";

    // We use one tor binary for one app
    private final static Map<String, Tor> TOR_BY_APP = new HashMap<>();

    enum State {
        NOT_STARTED,
        STARTING,
        STARTED,
        SHUTDOWN_STARTED
    }

    private final TorController torController;
    private final Bootstrap bootstrap;
    private final String torDirPath;
    private final AtomicReference<State> state = new AtomicReference<>(State.NOT_STARTED);
    private final Set<CompletableFuture<Boolean>> startupFutures = new CopyOnWriteArraySet<>();

    @Nullable
    private ExecutorService startupExecutor;
    private int proxyPort = -1;

    public static Tor getTor(String torDirPath) {
        Tor tor;
        synchronized (TOR_BY_APP) {
            if (TOR_BY_APP.containsKey(torDirPath)) {
                tor = TOR_BY_APP.get(torDirPath);
            } else {
                tor = new Tor(torDirPath);
                TOR_BY_APP.put(torDirPath, tor);
            }
        }
        return tor;
    }

    private Tor(String torDirPath) {
        this.torDirPath = torDirPath;
        bootstrap = new Bootstrap(torDirPath);
        torController = new TorController(bootstrap.getCookieFile());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("Tor.shutdownHook");
            shutdown();
        }));
    }

    public void shutdown() {
        if (state.get() == State.SHUTDOWN_STARTED) {
            return;
        }
        state.set(State.SHUTDOWN_STARTED);

        bootstrap.shutdown();
        torController.shutdown();

        if (startupExecutor != null) {
            MoreExecutors.shutdownAndAwaitTermination(startupExecutor, 100, TimeUnit.MILLISECONDS);
            startupExecutor = null;
        }
        log.info("Shutdown Tor completed");
    }

    public CompletableFuture<Boolean> startAsync() {
        return startAsync(getStartupExecutor());
    }

    public CompletableFuture<Boolean> startAsync(Executor executor) {
        if (state.get() == State.STARTED) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        startupFutures.add(future);
        executor.execute(() -> {
            try {
                if (state.get() == State.NOT_STARTED) {
                    start();
                    startupFutures.forEach(f -> f.complete(true));
                }
            } catch (IOException | InterruptedException exception) {
                bootstrap.deleteVersionFile();
                startupFutures.forEach(f -> f.completeExceptionally(exception));
            }
        });
        return future;
    }

    // Blocking start
    public void start() throws IOException, InterruptedException {
        if (state.get() == State.STARTED) {
            return;
        }
        checkArgument(state.get() == State.NOT_STARTED,
                "Invalid state at startAsync. state=" + state.get());
        state.set(State.STARTING);
        long ts = System.currentTimeMillis();
        int controlPort = bootstrap.start();
        torController.start(controlPort);
        proxyPort = torController.getProxyPort();
        state.set(State.STARTED);
        log.info(">> Starting Tor took {} ms", System.currentTimeMillis() - ts);
    }

    public TorServerSocket getTorServerSocket() throws IOException {
        checkArgument(state.get() == State.STARTED,
                "Invalid state at startAsync. state=" + state.get());
        return new TorServerSocket(torDirPath, torController);
    }

    public Proxy getProxy(@Nullable String streamId) throws IOException {
        checkArgument(state.get() == State.STARTED,
                "Invalid state at startAsync. state=" + state.get());
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
        checkArgument(state.get() == State.STARTED,
                "Invalid state at startAsync. state=" + state.get());
        checkArgument(proxyPort > -1, "proxyPort must be defined");
        Socks5Proxy socks5Proxy = new Socks5Proxy(LOCALHOST, proxyPort);
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
            socks5Proxy.setAuthenticationMethod(2,
                    new Authentication() {
                        @Override
                        public Object[] doSocksAuthentication(int i, Socket socket) throws IOException {
                            // Must not close streams here, as otherwise we get a socket closed
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

    private ExecutorService getStartupExecutor() {
        startupExecutor = ThreadingUtils.getSingleThreadExecutor("Tor.startAsync");
        return startupExecutor;
    }
}

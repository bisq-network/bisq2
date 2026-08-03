package bisq.network.tor.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TorControlProtocolTest {

    @Test
    void initializeSupportsUnixDomainControlSocket(@TempDir Path tempDir) throws Exception {
        Path controlSocketPath = tempDir.resolve("tor-control.sock");
        CountDownLatch commandReceived = new CountDownLatch(1);
        AtomicReference<String> commandRef = new AtomicReference<>();
        AtomicReference<Throwable> serverErrorRef = new AtomicReference<>();

        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            serverSocketChannel.bind(UnixDomainSocketAddress.of(controlSocketPath));

            Thread serverThread = new Thread(() -> acceptSingleCommand(serverSocketChannel,
                    commandRef,
                    commandReceived,
                    serverErrorRef));
            serverThread.setName("TorControlProtocolTest.server");
            serverThread.start();

            TorControlProtocol torControlProtocol = new TorControlProtocol();
            try {
                torControlProtocol.initialize(UnixDomainSocketAddress.of(controlSocketPath));
                torControlProtocol.authenticate(new byte[0]);

                assertThat(commandReceived.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(commandRef.get()).startsWith("AUTHENTICATE");
                assertThat(serverErrorRef.get()).isNull();
            } finally {
                torControlProtocol.close();
                serverThread.join(TimeUnit.SECONDS.toMillis(2));
                assertThat(serverThread.isAlive()).isFalse();
            }
        } finally {
            Files.deleteIfExists(controlSocketPath);
        }
    }

    private static void acceptSingleCommand(ServerSocketChannel serverSocketChannel,
                                            AtomicReference<String> commandRef,
                                            CountDownLatch commandReceived,
                                            AtomicReference<Throwable> serverErrorRef) {
        try (SocketChannel clientChannel = serverSocketChannel.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     Channels.newInputStream(clientChannel), StandardCharsets.US_ASCII));
             OutputStream outputStream = Channels.newOutputStream(clientChannel)) {
            commandRef.set(reader.readLine());
            outputStream.write("250 OK\r\n".getBytes(StandardCharsets.US_ASCII));
            outputStream.flush();
            commandReceived.countDown();
        } catch (IOException exception) {
            serverErrorRef.set(exception);
            commandReceived.countDown();
        }
    }
}
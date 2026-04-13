package bisq.network.tor.controller;

import bisq.common.encoding.Hex;
import bisq.network.tor.controller.events.listener.BootstrapEventListener;
import bisq.network.tor.controller.events.listener.HsDescEventListener;
import bisq.network.tor.controller.exceptions.CannotConnectWithTorException;
import bisq.network.tor.controller.exceptions.CannotSendCommandToTorException;
import bisq.security.keys.TorKeyPair;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TorControlProtocol implements AutoCloseable {
    private static final int MAX_CONNECTION_ATTEMPTS = 10;

    private AutoCloseable controlConnection;
    private final TorControlReader torControlReader;
    private Optional<OutputStream> outputStream = Optional.empty();

    // MidReplyLine = StatusCode "-" ReplyLine
    // DataReplyLine = StatusCode "+" ReplyLine CmdData
    private final Pattern multiLineReplyPattern = Pattern.compile("^\\d+[-+].+");
    private volatile boolean closeInProgress;

    public TorControlProtocol() {
        torControlReader = new TorControlReader();
    }

    public void initialize(int port) {
        initialize("127.0.0.1", port);
    }

    public void initialize(String host, int port) {
        try {
            SocketChannel socketChannel = createAndConnectTcpControlSocket(host, port);
            initializeControlConnection(socketChannel,
                    Channels.newInputStream(socketChannel),
                    Channels.newOutputStream(socketChannel),
                    host + ":" + port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Restore interrupted state
            log.error("TorControlProtocol initialization for control endpoint {}:{} was interrupted.", host, port, e);
            close();
            throw new CannotConnectWithTorException(e);
        } catch (CannotConnectWithTorException e) {
            log.error("TorControlProtocol failed to connect to Tor control endpoint {}:{}.", host, port, e);
            close();
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception during TorControlProtocol initialization for control endpoint {}:{}.", host, port, e);
            close();
            throw new CannotConnectWithTorException(e);
        }
    }

    public void initialize(Path controlSocketPath) {
        try {
            SocketChannel socketChannel = createAndConnectUnixControlSocket(controlSocketPath);
            initializeControlConnection(socketChannel,
                    Channels.newInputStream(socketChannel),
                    Channels.newOutputStream(socketChannel),
                    controlSocketPath.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("TorControlProtocol initialization for control socket {} was interrupted.", controlSocketPath, e);
            close();
            throw new CannotConnectWithTorException(e);
        } catch (CannotConnectWithTorException e) {
            log.error("TorControlProtocol failed to connect to Tor control socket {}.", controlSocketPath, e);
            close();
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception during TorControlProtocol initialization for control socket {}.", controlSocketPath, e);
            close();
            throw new CannotConnectWithTorException(e);
        }
    }

    private void initializeControlConnection(AutoCloseable controlConnection,
                                             InputStream inputStream,
                                             OutputStream outputStream,
                                             String endpointDescription) {
        this.controlConnection = controlConnection;
        this.outputStream = Optional.of(outputStream);
        this.torControlReader.start(inputStream);
        log.info("TorControlProtocol initialized successfully for control endpoint {}", endpointDescription);
    }

    @Override
    public void close() {
        if (closeInProgress) {
            return;
        }
        closeInProgress = true;
        log.debug("Closing TorControlProtocol resources.");
        try {
            if (controlConnection != null) {
                controlConnection.close();
                log.debug("Control connection closed.");
            }
        } catch (Exception e) {
            log.warn("Exception while closing control connection. This may be expected if connection was problematic.", e);
        }
        try {
            torControlReader.close();
            log.debug("TorControlReader closed.");
        } catch (Exception e) {
            log.warn("Exception while closing TorControlReader.", e);
        }
        controlConnection = null;
        outputStream = Optional.empty();
        closeInProgress = false;
    }

    public void authenticate(PasswordDigest passwordDigest) {
        byte[] secret = passwordDigest.getSecret();
        String secretHex = Hex.encode(secret);
        String command = "AUTHENTICATE " + secretHex + "\r\n";

        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();

        if (isSuccessReply(reply)) {
            return;
        }

        if (reply.startsWith("515 Authentication failed")) {
            throw new TorControlAuthenticationFailed(reply);
        }
    }

    public void authenticate(byte[] authCookie) {
        String authCookieAsHex = Hex.encode(authCookie);
        String command = "AUTHENTICATE " + authCookieAsHex + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();

        if (isSuccessReply(reply)) {
            return;
        }

        if (reply.startsWith("515 Authentication failed")) {
            throw new TorControlAuthenticationFailed(reply);
        }
    }

    public void addOnion(TorKeyPair torKeyPair, int onionPort, int localPort) {
        String base64SecretScalar = torKeyPair.getBase64SecretScalar();
        String command = "ADD_ONION " + "ED25519-V3:" + base64SecretScalar + " Port=" + onionPort + "," + localPort + "\r\n";

        sendCommand(command);
        Stream<String> replyStream = receiveReply();
        validateReply(replyStream, "ADD_ONION");
    }

    public String getInfo(String keyword) {
        String command = "GETINFO " + keyword + "\r\n";
        sendCommand(command);
        Stream<String> replyStream = receiveReply();
        return validateReply(replyStream, "GETINFO");
    }

    public void hsFetch(String hsAddress) {
        String command = "HSFETCH " + hsAddress + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!isSuccessReply(reply)) {
            throw new ControlCommandFailedException("Couldn't initiate HSFETCH for : " + hsAddress);
        }
    }

    public void resetConf(String configName) {
        String command = "RESETCONF " + configName + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!isSuccessReply(reply)) {
            throw new ControlCommandFailedException("Couldn't reset config: " + configName);
        }
    }

    public void setConfig(String configName, String configValue) {
        String command = "SETCONF " + configName + "=" + configValue + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!isSuccessReply(reply)) {
            throw new ControlCommandFailedException("Couldn't set config: " + configName + "=" + configValue);
        }
    }

    public void takeOwnership() {
        String command = "TAKEOWNERSHIP\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!isSuccessReply(reply)) {
            throw new ControlCommandFailedException("Couldn't take ownership");
        }
    }

    public void addBootstrapEventListener(BootstrapEventListener listener) {
        Set<String> previous = getEventTypesOfBootstrapEventListeners();
        torControlReader.addBootstrapEventListener(listener);
        String newEventType = listener.getEventType().name();
        if (!previous.contains(newEventType)) {
            refreshEventRegistration();
        } else {
            log.debug("We are already for that event registered");
        }
    }

    public void removeBootstrapEventListener(BootstrapEventListener listener) {
        Set<String> previous = getEventTypesOfBootstrapEventListeners();
        String newEventType = listener.getEventType().name();
        if (!previous.contains(newEventType)) {
            log.warn("Remove BootstrapEventListener but did not have eventType in listeners. This could happen if removeBootstrapEventListener was called without addBootstrapEventListener before.");
        }
        torControlReader.removeBootstrapEventListener(listener);
        Set<String> current = getEventTypesOfBootstrapEventListeners();
        if (!current.contains(newEventType)) {
            refreshEventRegistration();
        } else {
            log.debug("Event has been already removed from registration");
        }
    }

    private Set<String> getEventTypesOfBootstrapEventListeners() {
        return torControlReader.getBootstrapEventListeners().stream()
                .map(listener -> listener.getEventType().name())
                .collect(Collectors.toSet());
    }

    public void addHsDescEventListener(HsDescEventListener listener) {
        Set<String> previous = getEventTypesOfHsDescEventListeners();
        torControlReader.addHsDescEventListener(listener);
        String newEventType = listener.getEventType().name();
        if (!previous.contains(newEventType)) {
            refreshEventRegistration();
        } else {
            log.debug("We are already for that event registered");
        }
    }

    public void removeHsDescEventListener(HsDescEventListener listener) {
        Set<String> previous = getEventTypesOfHsDescEventListeners();
        String newEventType = listener.getEventType().name();
        if (!previous.contains(newEventType) && !closeInProgress) {
            log.warn("Remove HsDescEventListener but did not have eventType in listeners. This could happen if removeHsDescEventListener was called without addHsDescEventListener before.");
        }
        torControlReader.removeHsDescEventListener(listener);
        Set<String> current = getEventTypesOfHsDescEventListeners();
        if (!current.contains(newEventType)) {
            refreshEventRegistration();
        } else {
            log.debug("Event has been already removed from registration");
        }
    }

    private Set<String> getEventTypesOfHsDescEventListeners() {
        return torControlReader.getHsDescEventListeners().stream()
                .map(listener -> listener.getEventType().name())
                .collect(Collectors.toSet());
    }

    public void refreshEventRegistration() {
        Set<String> allEvents = Stream.concat(getEventTypesOfBootstrapEventListeners().stream(), getEventTypesOfHsDescEventListeners().stream())
                .collect(Collectors.toSet());
        registerEvents(allEvents);
    }

    private void registerEvents(Set<String> events) {
        if (events.isEmpty()) {
            log.info("Clear registered Events");
        } else {
            log.info("Register events {}", events);
        }
        var stringBuilder = new StringBuffer("SETEVENTS");
        events.forEach(event -> stringBuilder.append(" ").append(event));
        stringBuilder.append("\r\n");

        String command = stringBuilder.toString();
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!isSuccessReply(reply)) {
            throw new ControlCommandFailedException("Couldn't set events: " + events);
        }
    }

    private SocketChannel createAndConnectTcpControlSocket(String host,
                                                           int port) throws InterruptedException, CannotConnectWithTorException {
        int connectionAttempt = 0;
        Exception lastException = null;

        while (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
            SocketChannel socketChannel = null;
            try {
                // The Tor Control Port communication is typically unencrypted.
                // This is considered safe because the connection is made to 127.0.0.1 (localhost),
                // ensuring that the communication does not leave the local machine and is not
                // exposed to external networks. Authentication (via cookie or password)
                // is handled by the Tor control protocol itself after connection.
                //
                // Explicitly use INET (IPv4) so the socket's local address appears as
                // "127.0.0.1" in /proc/net/tcp.  Java's default NioSocketImpl creates
                // AF_INET6 dual-stack sockets, causing the local address to appear as
                // "::ffff:127.0.0.1" in /proc/net/tcp6.  On Tails, onion-grater resolves
                // the client PID via psutil.net_connections() matching on local address;
                // the IPv6-mapped form does not match, so PID lookup fails and
                // onion-grater silently closes the connection.
                socketChannel = SocketChannel.open(StandardProtocolFamily.INET);
                var socketAddress = new InetSocketAddress(host, port);
                log.debug("Attempting to connect to Tor control endpoint {} ({}/{})", socketAddress, connectionAttempt + 1, MAX_CONNECTION_ATTEMPTS);
                socketChannel.connect(socketAddress);
                log.info("Successfully connected control socket to Tor endpoint {} after {} attempts", socketAddress, connectionAttempt + 1);
                return socketChannel;
            } catch (ConnectException e) {
                lastException = e;
                log.warn("ConnectException on attempt {} to Tor control endpoint {}:{}: {}. Closing channel.",
                        connectionAttempt + 1, host, port, e.getMessage());
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                    } catch (IOException closeEx) {
                        log.warn("Failed to close channel after ConnectException on endpoint {}:{}: {}",
                                host, port, closeEx.getMessage(), closeEx);
                    }
                }
            } catch (IOException e) {
                lastException = e;
                log.warn("IOException on attempt {} to Tor control endpoint {}:{}: {}. Closing channel.",
                        connectionAttempt + 1, host, port, e.getMessage());
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                    } catch (IOException closeEx) {
                        log.warn("Failed to close channel after IOException on endpoint {}:{}: {}",
                                host, port, closeEx.getMessage(), closeEx);
                    }
                }
            }

            connectionAttempt++;
            if (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
                log.debug("Connection attempt to Tor control endpoint {}:{} failed. Retrying in 200ms...", host, port);
                Thread.sleep(200);
            }
        }

        String errorMessage = "Failed to connect to Tor control endpoint " + host + ":" + port +
                " after " + MAX_CONNECTION_ATTEMPTS + " attempts.";
        IOException wrapperException = new IOException(errorMessage, lastException);
        log.error(errorMessage, wrapperException);
        throw new CannotConnectWithTorException(wrapperException);
    }

    private SocketChannel createAndConnectUnixControlSocket(Path controlSocketPath) throws InterruptedException, CannotConnectWithTorException {
        int connectionAttempt = 0;
        Exception lastException = null;

        while (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
            SocketChannel socketChannel = null;
            try {
                ProtocolFamily protocolFamily = StandardProtocolFamily.UNIX;
                socketChannel = SocketChannel.open(protocolFamily);
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(controlSocketPath);
                log.debug("Attempting to connect to Tor control socket {} ({}/{})",
                        controlSocketPath, connectionAttempt + 1, MAX_CONNECTION_ATTEMPTS);
                socketChannel.connect(socketAddress);
                log.info("Successfully connected control socket to Tor socket {} after {} attempts",
                        controlSocketPath, connectionAttempt + 1);
                return socketChannel;
            } catch (IOException | UnsupportedOperationException e) {
                lastException = e;
                log.warn("Connection attempt {} to Tor control socket {} failed: {}. Closing channel.",
                        connectionAttempt + 1, controlSocketPath, e.getMessage());
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                    } catch (IOException closeEx) {
                        log.warn("Failed to close socket channel for control socket {}: {}",
                                controlSocketPath, closeEx.getMessage(), closeEx);
                    }
                }
            }

            connectionAttempt++;
            if (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
                log.debug("Connection attempt to Tor control socket {} failed. Retrying in 200ms...", controlSocketPath);
                Thread.sleep(200);
            }
        }

        String errorMessage = "Failed to connect to Tor control socket " + controlSocketPath +
                " after " + MAX_CONNECTION_ATTEMPTS + " attempts.";
        IOException wrapperException = new IOException(errorMessage, lastException);
        log.error(errorMessage, wrapperException);
        throw new CannotConnectWithTorException(wrapperException);
    }

    private void sendCommand(String command) {
        if (closeInProgress) {
            return;
        }
        if (outputStream.isEmpty()) {
            throw new IllegalStateException("TorControlProtocol output stream not initialized. Cannot send command.");
        }
        try {
            String commandToLog = command.contains("AUTHENTICATE")
                    ? command.split(" ")[0] + " [authentication data hidden in logs]"
                    : command;
            if (commandToLog.endsWith("\r\n")) {
                commandToLog = commandToLog.substring(0, commandToLog.length() - 2);
            }
            log.info("Send Tor control command: {}", commandToLog);
            @SuppressWarnings("resource") OutputStream os = this.outputStream.orElseThrow();
            byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII);
            os.write(commandBytes);
            os.flush();
        } catch (IOException e) {
            throw new CannotSendCommandToTorException(e);
        }
    }

    private Stream<String> receiveReply() {
        String reply = tryReadNextReply();
        var streamBuilder = Stream.<String>builder();
        streamBuilder.add(reply);

        while (isMultilineReply(reply)) {
            reply = tryReadNextReply();
            streamBuilder.add(reply);
        }

        return streamBuilder.build();
    }

    private String tryReadNextReply() {
        String reply = torControlReader.readLine();
        if (reply.equals("510 Command filtered")) {
            throw new TorCommandFilteredException();
        }
        return reply;
    }

    private boolean isMultilineReply(String reply) {
        return multiLineReplyPattern.matcher(reply).matches();
    }

    // TODO check if this change is correct
    // We can receive 1 entry with "250 OK" or multiple entries starting with "250-" and the last entry with "250 OK"
    // Multiple entries following a single entry have been observed when using multiple user profiles.
    // E.g. [250 OK]
    // [250-ServiceID=bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd, 250 OK]
    // [250-ServiceID=bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd, 250-ServiceID=xjlwqzk6n4i5co574jrljsigelx6itzya32cfb7sfofqn7wueyhjj4id, 250 OK]
    private String validateReply(Stream<String> replyStream, String commandName) {
        List<String> replies = replyStream.toList();

        String OK250 = "250 OK";
        boolean hasOK250 = replies.stream().anyMatch(e -> e.equals(OK250));
        List<String> listOf250WithData = replies.stream().filter(e -> e.startsWith("250-")).toList();
        if (!listOf250WithData.isEmpty()) {
            // TODO are there use cases where we need all 250WithData entries?
            return listOf250WithData.get(0);
        } else if (hasOK250) {
            return OK250;
        } else {
            throw new ControlCommandFailedException("Invalid " + commandName + " reply: " + replies);
        }

       /* if (replies.size() != 2) {
            throw new ControlCommandFailedException("Invalid " + commandName + " reply: " + replies);
        }

        String firstLine = replies.get(0);
        if (!firstLine.startsWith("250-")) {
            throw new ControlCommandFailedException("Invalid " + commandName + " reply: " + replies);
        }

        String secondLine = replies.get(1);
        if (!secondLine.equals("250 OK")) {
            throw new ControlCommandFailedException("Invalid " + commandName + " reply: " + replies);
        }

        return firstLine;*/
    }

    private boolean isSuccessReply(String reply) {
        return reply.equals("250 OK");
    }
}

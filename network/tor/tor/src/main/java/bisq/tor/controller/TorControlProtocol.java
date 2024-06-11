package bisq.tor.controller;

import bisq.common.encoding.Hex;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.listener.BootstrapEventListener;
import bisq.tor.controller.events.listener.HsDescEventListener;
import bisq.tor.controller.exceptions.CannotConnectWithTorException;
import bisq.tor.controller.exceptions.CannotSendCommandToTorException;
import net.freehaven.tor.control.PasswordDigest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TorControlProtocol implements AutoCloseable {
    private static final int MAX_CONNECTION_ATTEMPTS = 10;

    private final Socket controlSocket;
    private final WhonixTorControlReader whonixTorControlReader;
    private Optional<OutputStream> outputStream = Optional.empty();

    // MidReplyLine = StatusCode "-" ReplyLine
    // DataReplyLine = StatusCode "+" ReplyLine CmdData
    private final Pattern multiLineReplyPattern = Pattern.compile("^\\d+[-+].+");

    public TorControlProtocol() {
        controlSocket = new Socket();
        whonixTorControlReader = new WhonixTorControlReader();
    }

    public void initialize(int port) {
        try {
            connectToTor(port);
            whonixTorControlReader.start(controlSocket.getInputStream());
            outputStream = Optional.of(controlSocket.getOutputStream());
        } catch (IOException | InterruptedException e) {
            throw new CannotConnectWithTorException(e);
        }
    }

    @Override
    public void close() {
        try {
            controlSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void authenticate(PasswordDigest passwordDigest) {
        byte[] secret = passwordDigest.getSecret();
        String secretHex = Hex.encode(secret);
        String command = "AUTHENTICATE " + secretHex + "\r\n";

        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();

        if (reply.equals("250 OK")) {
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
        assertTwoLineOkReply(replyStream, "ADD_ONION");
    }

    public String getInfo(String keyword) {
        String command = "GETINFO " + keyword + "\r\n";
        sendCommand(command);
        Stream<String> replyStream = receiveReply();
        return assertTwoLineOkReply(replyStream, "GETINFO");
    }

    public void hsFetch(String hsAddress) {
        String command = "HSFETCH " + hsAddress + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't initiate HSFETCH for : " + hsAddress);
        }
    }

    public void resetConf(String configName) {
        String command = "RESETCONF " + configName + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't reset config: " + configName);
        }
    }

    public void setConfig(String configName, String configValue) {
        String command = "SETCONF " + configName + "=" + configValue + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't set config: " + configName + "=" + configValue);
        }
    }

    public void setEvents(List<String> events) {
        var stringBuilder = new StringBuffer("SETEVENTS");
        events.forEach(event -> stringBuilder.append(" ").append(event));
        stringBuilder.append("\r\n");

        String command = stringBuilder.toString();
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't set events: " + events);
        }
    }

    public void takeOwnership() {
        String command = "TAKEOWNERSHIP\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't take ownership");
        }
    }

    public void addBootstrapEventListener(BootstrapEventListener listener) {
        whonixTorControlReader.addBootstrapEventListener(listener);
    }

    public void removeBootstrapEventListener(BootstrapEventListener listener) {
        whonixTorControlReader.removeBootstrapEventListener(listener);
    }

    public void addHsDescEventListener(HsDescEventListener listener) {
        whonixTorControlReader.addHsDescEventListener(listener);
    }

    public void removeHsDescEventListener(HsDescEventListener listener) {
        whonixTorControlReader.removeHsDescEventListener(listener);
    }

    private void connectToTor(int port) throws InterruptedException {
        int connectionAttempt = 0;
        while (connectionAttempt < MAX_CONNECTION_ATTEMPTS) {
            try {
                var socketAddress = new InetSocketAddress("127.0.0.1", port);
                controlSocket.connect(socketAddress);
                break;
            } catch (ConnectException e) {
                connectionAttempt++;
                Thread.sleep(200);
            } catch (IOException e) {
                throw new CannotConnectWithTorException(e);
            }
        }
    }

    private void sendCommand(String command) {
        try {
            @SuppressWarnings("resource") OutputStream outputStream = this.outputStream.orElseThrow();
            byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII);
            outputStream.write(commandBytes);
            outputStream.flush();
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
        String reply = whonixTorControlReader.readLine();
        if (reply.equals("510 Command filtered")) {
            throw new TorCommandFilteredException();
        }
        return reply;
    }

    private boolean isMultilineReply(String reply) {
        return multiLineReplyPattern.matcher(reply).matches();
    }

    private String assertTwoLineOkReply(Stream<String> replyStream, String commandName) {
        List<String> replies = replyStream.collect(Collectors.toList());
        if (replies.size() != 2) {
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

        return firstLine;
    }
}

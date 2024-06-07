package bisq.tor.controller;

import bisq.common.encoding.Hex;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.listener.BootstrapEventListener;
import net.freehaven.tor.control.PasswordDigest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TorControlProtocol implements AutoCloseable {
    private final Socket controlSocket;
    private final WhonixTorControlReader whonixTorControlReader;
    private final OutputStream outputStream;

    // MidReplyLine = StatusCode "-" ReplyLine
    // DataReplyLine = StatusCode "+" ReplyLine CmdData
    private final Pattern multiLineReplyPattern = Pattern.compile("^\\d+[-+].+");

    public TorControlProtocol(int port) throws IOException {
        controlSocket = new Socket("127.0.0.1", port);
        whonixTorControlReader = new WhonixTorControlReader(controlSocket.getInputStream());
        outputStream = controlSocket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        controlSocket.close();
    }

    public void initialize() {
        whonixTorControlReader.start();
    }

    public void authenticate(PasswordDigest passwordDigest) throws IOException {
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

    public void addOnion(TorKeyPair torKeyPair, int onionPort, int localPort) throws IOException {
        String base64SecretScalar = torKeyPair.getBase64SecretScalar();
        String command = "ADD_ONION " + "ED25519-V3:" + base64SecretScalar + " Port=" + onionPort + "," + localPort + "\r\n";

        sendCommand(command);
        Stream<String> replyStream = receiveReply();
        assertTwoLineOkReply(replyStream, "ADD_ONION");
    }

    public String getInfo(String keyword) throws IOException {
        String command = "GETINFO " + keyword + "\r\n";
        sendCommand(command);
        Stream<String> replyStream = receiveReply();
        return assertTwoLineOkReply(replyStream, "GETINFO");
    }

    public void resetConf(String configName) throws IOException {
        String command = "RESETCONF " + configName + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't reset config: " + configName);
        }
    }

    public void setConfig(String configName, String configValue) throws IOException {
        String command = "SETCONF " + configName + "=" + configValue + "\r\n";
        sendCommand(command);
        String reply = receiveReply().findFirst().orElseThrow();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't set config: " + configName + "=" + configValue);
        }
    }

    public void setEvents(List<String> events) throws IOException {
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

    public void takeOwnership() throws IOException {
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

    private void sendCommand(String command) throws IOException {
        byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII);
        outputStream.write(commandBytes);
        outputStream.flush();
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

package bisq.tor.controller;

import bisq.common.encoding.Hex;
import bisq.security.keys.TorKeyPair;
import net.freehaven.tor.control.PasswordDigest;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WhonixTorController implements AutoCloseable {
    private final Socket controlSocket;
    private final BufferedReader bufferedReader;
    private final OutputStream outputStream;

    public WhonixTorController() throws IOException {
        controlSocket = new Socket("127.0.0.1", 9051);

        InputStream inputStream = controlSocket.getInputStream();
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));

        outputStream = controlSocket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        controlSocket.close();
    }

    public void authenticate(PasswordDigest passwordDigest) throws IOException {
        byte[] secret = passwordDigest.getSecret();
        String secretHex = Hex.encode(secret);
        String command = "AUTHENTICATE " + secretHex + "\r\n";

        sendCommand(command);
        String reply = receiveReply();

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
        String reply = receiveReply();
    }

    public void resetConf(String configName) throws IOException {
        String command = "RESETCONF " + configName + "\r\n";
        sendCommand(command);
        String reply = receiveReply();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't reset config: " + configName);
        }
    }

    public void setEvents(List<String> events) throws IOException {
        var stringBuilder = new StringBuffer("SETEVENTS");
        events.forEach(event -> stringBuilder.append(" ").append(event));
        stringBuilder.append("\r\n");

        String command = stringBuilder.toString();
        sendCommand(command);
        String reply = receiveReply();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't set events: " + events);
        }
    }

    public void takeOwnership() throws IOException {
        String command = "TAKEOWNERSHIP\r\n";
        sendCommand(command);
        String reply = receiveReply();
        if (!reply.equals("250 OK")) {
            throw new ControlCommandFailedException("Couldn't take ownership");
        }
    }

    private void sendCommand(String command) throws IOException {
        byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII);
        outputStream.write(commandBytes);
        outputStream.flush();
    }

    private String receiveReply() throws IOException {
        String reply = bufferedReader.readLine();
        if (reply.equals("510 Command filtered")) {
            throw new TorCommandFilteredException();
        }
        return reply;
    }
}

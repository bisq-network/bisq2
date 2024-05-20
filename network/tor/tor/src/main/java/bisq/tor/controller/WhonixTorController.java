package bisq.tor.controller;

import bisq.security.keys.TorKeyPair;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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

    public void addOnion(TorKeyPair torKeyPair, int onionPort, int localPort) throws IOException {
        String base64SecretScalar = torKeyPair.getBase64SecretScalar();
        String command = "ADD_ONION " + "ED25519-V3:" + base64SecretScalar + " Port=" + onionPort + "," + localPort + "\r\n";
        byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII);

        outputStream.write(commandBytes);
        outputStream.flush();

        String reply = bufferedReader.readLine();
        if (reply.equals("510 Command filtered")) {
            throw new TorCommandFilteredException();
        }
    }
}

package bisq.network.i2p.streaming;

import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PException;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.*;
import net.i2p.util.I2PThread;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

// Requires the i2p router to be running locally
@Slf4j
public class EchoServer {

    public static void main(String[] args) {
        I2PSocketManager manager = I2PSocketManagerFactory.createManager();

        I2PSocketOptions i2PSocketOptions = manager.getDefaultOptions();
        i2PSocketOptions.setLocalPort(1234);
        manager.setDefaultOptions(i2PSocketOptions);

        I2PServerSocket serverSocket = manager.getServerSocket();
        I2PSession session = manager.getSession();

        session.getPrivateKey();

        // Print the base64 string, the regular string would look like garbage.
        System.out.println(session.getMyDestination().toBase64());

        // Create socket to handle clients
        I2PThread t = new I2PThread(new ClientHandler(serverSocket));
        t.setName("clienthandler1");
        t.setDaemon(false);
        t.start();
    }

    private static class ClientHandler implements Runnable {

        public ClientHandler(I2PServerSocket socket) {
            this.socket = socket;
        }

        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    I2PSocket sock = this.socket.accept();
                    if (sock != null) {
                        log.info("Received incoming connection");

                        //Receive from clients
                        BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        //Send to clients
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                        String line = br.readLine();
                        if (line != null) {
                            System.out.println("Received from client: " + line);
                            bw.write(line + '\n');
                            log.info("Wrote to buffer");
                            bw.flush(); //Flush to make sure everything got sent
                            log.info("Flushed buffer");
                        }

                        sock.close();
                        log.info("Socket closed");
                    }
                } catch (I2PException ex) {
                    System.out.println("General I2P exception!");
                } catch (ConnectException ex) {
                    System.out.println("Error connecting!");
                } catch (SocketTimeoutException ex) {
                    System.out.println("Timeout!");
                } catch (IOException ex) {
                    System.out.println("General read/write-exception!");
                }
            }
        }

        private final I2PServerSocket socket;
    }
}
package bisq.tor.controller;

import bisq.tor.controller.events.events.BootstrapEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class WhonixTorControlReader implements AutoCloseable {
    private final BufferedReader bufferedReader;
    private final BlockingQueue<String> replies = new LinkedBlockingQueue<>();
    private Optional<Thread> workerThread = Optional.empty();

    public WhonixTorControlReader(InputStream inputStream) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));
    }

    public void start() {
        Thread thread = new Thread(() -> {
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {

                    if (isStatusClientEvent(line)) {
                        Optional<BootstrapEvent> bootstrapEvent = BootstrapEventParser.tryParse(line);

                        if (bootstrapEvent.isPresent()) {
                            log.info("{}", bootstrapEvent.get());
                        } else {
                            log.info("Unknown status client event: {}", line);
                        }

                    } else {
                        replies.add(line);
                    }

                    if (Thread.interrupted()) {
                        break;
                    }
                }
            } catch (IOException e) {
                log.error("Tor control port reader couldn't read reply.", e);
            }

        });
        workerThread = Optional.of(thread);
        thread.start();
    }

    @Override
    public void close() throws Exception {
        workerThread.ifPresent(Thread::interrupt);
    }

    public String readLine() {
        try {
            return replies.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isStatusClientEvent(String line) {
        // 650 STATUS_CLIENT NOTICE CIRCUIT_ESTABLISHED
        return line.startsWith("650 STATUS_CLIENT");
    }
}

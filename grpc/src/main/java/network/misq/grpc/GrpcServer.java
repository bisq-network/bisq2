package network.misq.grpc;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class GrpcServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    private final Server server;

    public GrpcServer() {
        this.server = ServerBuilder.forPort(9999)
                // .intercept(new PasswordAuthInterceptor())
                .addService(ProtoReflectionService.newInstance())
                .build();
    }

    public void start() {
        try {
            server.start();
            log.info("Listening on port {}", server.getPort());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void shutdown() {
        log.info("Server shutdown started");
        server.shutdown();
        try {
            if (server.awaitTermination(1, SECONDS)) {
                log.info("Server shutdown complete");
                return;
            }
            server.shutdownNow();
            if (server.awaitTermination(1, SECONDS)) {
                log.info("Forced server shutdown complete");
                return;
            }
        } catch (InterruptedException ex) {
            log.error("", ex);
        }
        throw new RuntimeException("Unable to shutdown server");
    }
}

package bisq.api;

import bisq.api.endpoints.KeyPairEndpoint;
import bisq.application.DefaultApplicationService;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

/**
 * Application main to start and config the rest service.
 * This creates and rest service at BASE_URL for clients to connect and for users to browse the documentation.
 * <p>
 * swagger doc are available at <a href="http://localhost:8082/doc/v1/index.html">REST documentation</a>
 */
@Slf4j
public class RestApplication extends ResourceConfig {
    public static final String BASE_URL = "http://localhost:8082/rest/v1";

    @Getter
    protected final DefaultApplicationService applicationService;

    public RestApplication() {
        applicationService = new DefaultApplicationService(new String[]{"--appName=bisq2_API"});
        applicationService.initialize().join();
    }

    protected static HttpServer httpServer;

    public static void main(String[] args) throws Exception {
        startServer();
    }

    public static void stopServer() {
        httpServer.stop(2);
    }

    public static void startServer() throws Exception {
        // 'config' acts as application in jax-rs
        ResourceConfig app = new RestApplication()
                .register(CustomExceptionMapper.class)
                .register(StatusException.StatusExceptionMapper.class)
//                .register(ProtoWriter.class)
//                .register(KeyPairWriter.class)
                .register(KeyPairEndpoint.class)
                .register(SwaggerResource.class);

        httpServer = JdkHttpServerFactory.createHttpServer(URI.create(BASE_URL), app);
        httpServer.createContext("/doc", new StaticFileHandler("/doc/v1/"));

        // shut down hook
        Runtime.getRuntime().addShutdownHook(new Thread(RestApplication::stopServer));

        log.info("Server started at {}.", BASE_URL);

        // block and wait shut down signal, like CTRL+C
        Thread.currentThread().join();

        stopServer();
    }
}

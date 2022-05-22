package bisq.api.jax;

import bisq.api.jax.resource.ChatApi;
import bisq.api.jax.resource.KeyPairApi;
import bisq.application.DefaultApplicationService;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.ApplicationPath;
import lombok.Getter;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    public static final URI BASE_URI = URI.create("http://localhost:8082/rest");

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

    private static void stopServer() {
        httpServer.stop(0);
    }

    private static void startServer() throws Exception {
        // 'config' acts as application in jax-rs
        final ResourceConfig app = new RestApplication()
                .register(CustomExceptionMapper.class)
                .register(ProtoWriter.class)
                .register(KeyPairWriter.class)
                .register(ChatApi.class)
                .register(KeyPairApi.class)
                .register(SwaggerResource.class);

        httpServer = JdkHttpServerFactory.createHttpServer(BASE_URI, app);
        httpServer.createContext("/doc", new StaticFileHandler("/doc/v1/"));

        // shut down hook
        Runtime.getRuntime().addShutdownHook(new Thread(RestApplication::stopServer));

        System.out.println(
                String.format("Server started at %1$s", BASE_URI));

        // block and wait shut down signal, like CTRL+C
        Thread.currentThread().join();

    }
}

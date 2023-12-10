package bisq.rest_api;

import bisq.common.application.Service;
import bisq.rest_api.endpoints.ChatApi;
import bisq.rest_api.endpoints.KeyPairApi;
import bisq.rest_api.error.CustomExceptionMapper;
import bisq.rest_api.error.StatusException;
import bisq.rest_api.util.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * JAX-RS application for the Bisq REST API
 */
@Slf4j
public class JaxRsApplication extends ResourceConfig implements Service {
    //todo use config
    public static final String BASE_URL = "http://localhost:8082/api/v1";

    @Getter
    private final Supplier<RestApiApplicationService> applicationService;
    private HttpServer httpServer;

    public JaxRsApplication(String[] args, Supplier<RestApiApplicationService> applicationService) {
        this.applicationService = applicationService;
        // 'config' acts as application in jax-rs
        register(CustomExceptionMapper.class)
                .register(StatusException.StatusExceptionMapper.class)
                .register(KeyPairApi.class)
                .register(ChatApi.class)
                .register(SwaggerResolution.class);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        httpServer = JdkHttpServerFactory.createHttpServer(URI.create(BASE_URL), this);
        httpServer.createContext("/doc", new StaticFileHandler("/doc/v1/"));
        log.info("Server started at {}.", BASE_URL);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (httpServer != null) {
            httpServer.stop(2);
        }
        return CompletableFuture.completedFuture(true);
    }
}

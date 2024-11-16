package bisq.rest_api;

import bisq.common.application.Service;
import bisq.rest_api.endpoints.ChatApi;
import bisq.rest_api.endpoints.KeyBundleApi;
import bisq.rest_api.endpoints.ReportApi;
import bisq.rest_api.error.CustomExceptionMapper;
import bisq.rest_api.error.StatusException;
import bisq.rest_api.util.StaticFileHandler;
import bisq.user.identity.UserIdentityApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * JAX-RS application for the Bisq REST API
 */
@Slf4j
public class JaxRsApplication extends ResourceConfig implements Service {
    //todo (refactor, low prio) use config
    public static final String BASE_URL = "http://localhost:8082/api/v1";

    private HttpServer httpServer;

    public JaxRsApplication(String[] args, RestApiApplicationService applicationService) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SerializationModule());

        register(CustomExceptionMapper.class)
                .register(StatusException.StatusExceptionMapper.class)
                .register(SwaggerResolution.class)
                .register(mapper)
                .register(new KeyBundleApi(applicationService.getKeyBundleService()))
                .register(new ChatApi(applicationService.getChatService()))
                .register(new UserIdentityApi(applicationService.getKeyBundleService(), applicationService.getUserService()))
                .register(new ReportApi(applicationService.getNetworkService(), applicationService.getBondedRolesService()));
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        httpServer = JdkHttpServerFactory.createHttpServer(URI.create(BASE_URL), this);
        httpServer.createContext("/doc", new StaticFileHandler("/doc/v1/"));
        httpServer.createContext("/node-monitor", new StaticFileHandler("/node-monitor/"));
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

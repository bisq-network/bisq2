package bisq.rest_api;

import bisq.common.application.Service;
import bisq.common.rest_api.error.CustomExceptionMapper;
import bisq.common.rest_api.error.RestApiException;
import bisq.rest_api.report.ReportApi;
import bisq.rest_api.util.SerializationModule;
import bisq.rest_api.util.StaticFileHandler;
import bisq.rest_api.util.SwaggerResolution;
import bisq.user.identity.UserIdentityServiceApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8082/doc/v1/index.html
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
                .register(RestApiException.Mapper.class)
                .register(mapper)
                .register(SwaggerResolution.class);

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(UserIdentityServiceApi.class);
        register(ReportApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new UserIdentityServiceApi(applicationService.getUserService().getUserIdentityService())).to(UserIdentityServiceApi.class);
                bind(new ReportApi(applicationService.getNetworkService(), applicationService.getBondedRolesService())).to(ReportApi.class);
            }
        });
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
            httpServer.stop(1);
        }
        return CompletableFuture.completedFuture(true);
    }
}

package bisq.http_api.rest_api;

import bisq.http_api.auth.HttpApiAuthFilter;
import bisq.http_api.config.CommonApiConfig;
import bisq.http_api.rest_api.error.CustomExceptionMapper;
import bisq.http_api.rest_api.error.RestApiException;
import bisq.http_api.rest_api.util.SwaggerResolution;
import bisq.http_api.validator.HttpApiRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public abstract class BaseRestApiResourceConfig extends ResourceConfig {
    public BaseRestApiResourceConfig(CommonApiConfig config) {
        super();
        String swaggerBaseUrl = config.getRestApiBaseUrl();
        ObjectMapper mapper = new ObjectMapper();

        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(mapper);

        if (!config.getPassword().isEmpty()) {
            HttpApiAuthFilter httpApiAuthFilter = HttpApiAuthFilter.from(config);
            register(httpApiAuthFilter);
        }

        HttpApiRequestFilter httpApiRequestFilter = HttpApiRequestFilter.from(config);
        register(httpApiRequestFilter);

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(SwaggerResolution.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new SwaggerResolution(swaggerBaseUrl)).to(SwaggerResolution.class);
            }
        });
    }
}

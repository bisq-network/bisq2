package bisq.http_api.rest_api;

import bisq.http_api.rest_api.error.CustomExceptionMapper;
import bisq.http_api.rest_api.error.RestApiException;
import bisq.http_api.rest_api.util.SerializationModule;
import bisq.http_api.rest_api.util.SwaggerResolution;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public abstract class BaseRestApiResourceConfig extends ResourceConfig {
    public BaseRestApiResourceConfig(String swaggerBaseUrl) {
        super();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SerializationModule());

        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(mapper);

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

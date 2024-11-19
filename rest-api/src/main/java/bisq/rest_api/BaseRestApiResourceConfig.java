package bisq.rest_api;

import bisq.common.rest_api.error.CustomExceptionMapper;
import bisq.common.rest_api.error.RestApiException;
import bisq.rest_api.util.SerializationModule;
import bisq.rest_api.util.SwaggerResolution;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public abstract class BaseRestApiResourceConfig extends ResourceConfig {
    public BaseRestApiResourceConfig(RestApiService.Config config) {
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
                bind(new SwaggerResolution(config.getBaseUrl())).to(SwaggerResolution.class);
            }
        });
    }
}

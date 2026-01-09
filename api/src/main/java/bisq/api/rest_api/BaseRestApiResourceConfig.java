package bisq.api.rest_api;

import bisq.common.util.StringUtils;
import bisq.api.ApiConfig;
import bisq.api.auth.ApiAuthFilter;
import bisq.api.rest_api.error.CustomExceptionMapper;
import bisq.api.rest_api.error.RestApiException;
import bisq.api.rest_api.util.SwaggerResolution;
import bisq.api.validator.ApiRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public abstract class BaseRestApiResourceConfig extends ResourceConfig {
    public BaseRestApiResourceConfig(ApiConfig apiConfig) {
        super();
        String swaggerBaseUrl = apiConfig.getRestServerUrl();
        ObjectMapper mapper = new ObjectMapper();

        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(mapper);


        String password = ""; // TODO config.getPassword();
        if (StringUtils.isNotEmpty(password)) {
            ApiAuthFilter apiAuthFilter = new ApiAuthFilter(password);
            register(apiAuthFilter);
        }

        ApiRequestFilter apiRequestFilter = getApiRequestFilter(apiConfig);
        register(apiRequestFilter);

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

    protected abstract ApiRequestFilter getApiRequestFilter(ApiConfig apiConfig);
}

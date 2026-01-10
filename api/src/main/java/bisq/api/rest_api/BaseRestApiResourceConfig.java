package bisq.api.rest_api;

import bisq.common.util.StringUtils;
import bisq.api.auth.ApiAuthFilter;
import bisq.api.config.CommonApiConfig;
import bisq.api.rest_api.error.CustomExceptionMapper;
import bisq.api.rest_api.error.RestApiException;
import bisq.api.rest_api.util.SwaggerResolution;
import bisq.api.validator.ApiRequestFilter;
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


        String password = config.getPassword();
        if (StringUtils.isNotEmpty(password)) {
            ApiAuthFilter apiAuthFilter = new ApiAuthFilter(password);
            register(apiAuthFilter);
        }

        ApiRequestFilter apiRequestFilter = ApiRequestFilter.from(config);
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
}

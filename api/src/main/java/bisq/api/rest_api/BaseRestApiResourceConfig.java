package bisq.api.rest_api;

import bisq.api.ApiConfig;
import bisq.api.access.filter.authn.RestApiAuthenticationFilter;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.filter.authz.RestApiAuthorizationFilter;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.rest_api.error.CustomExceptionMapper;
import bisq.api.rest_api.error.RestApiException;
import bisq.api.rest_api.util.SwaggerResolution;
import bisq.common.json.JsonMapperProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

public abstract class BaseRestApiResourceConfig extends ResourceConfig {
    public BaseRestApiResourceConfig(ApiConfig apiConfig,
                                     PermissionService<RestPermissionMapping> permissionService,
                                     SessionAuthenticationService sessionAuthenticationService) {
        super();
        String swaggerBaseUrl = apiConfig.getRestServerApiBasePath();

        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider(JsonMapperProvider.get(),
                JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);

        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(provider);

        if (apiConfig.isAuthRequired()) {
            register(new RestApiAuthenticationFilter(sessionAuthenticationService));
            register(new RestApiAuthorizationFilter(permissionService,
                    apiConfig.getRestAllowEndpoints(),
                    apiConfig.getRestDenyEndpoints()));
        }

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

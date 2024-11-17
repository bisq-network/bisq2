package bisq.rest_api;

import bisq.common.rest_api.error.CustomExceptionMapper;
import bisq.common.rest_api.error.RestApiException;
import bisq.rest_api.report.ReportRestApi;
import bisq.rest_api.util.SerializationModule;
import bisq.rest_api.util.SwaggerResolution;
import bisq.user.identity.UserIdentityRestApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

@Slf4j
public class RestApiResourceConfig extends ResourceConfig {
    public RestApiResourceConfig(RestApiApplicationService applicationService, String baseUrl) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SerializationModule());

        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(mapper);

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(SwaggerResolution.class);
        register(UserIdentityRestApi.class);
        register(ReportRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new SwaggerResolution(baseUrl)).to(SwaggerResolution.class);
                bind(new UserIdentityRestApi(applicationService.getUserService().getUserIdentityService())).to(UserIdentityRestApi.class);
                bind(new ReportRestApi(applicationService.getNetworkService(), applicationService.getBondedRolesService())).to(ReportRestApi.class);
            }
        });
    }
}

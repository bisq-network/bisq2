package bisq.api.rest_api;

import bisq.api.rest_api.error.CustomExceptionMapper;
import bisq.api.rest_api.error.RestApiException;
import bisq.common.json.JsonMapperProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

public class BaseResourceConfig extends ResourceConfig {
    public BaseResourceConfig() {
        super();
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider(JsonMapperProvider.get(),
                JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(provider);
    }
}

package bisq.api.rest_api;

import bisq.api.rest_api.endpoints.pairing.PairingApi;
import bisq.api.rest_api.error.CustomExceptionMapper;
import bisq.api.rest_api.error.RestApiException;
import bisq.common.json.JsonMapperProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

public class PairingApiResourceConfig extends ResourceConfig {
    public PairingApiResourceConfig(PairingApi pairingApi) {
        super();
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider(JsonMapperProvider.get(),
                JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
        register(CustomExceptionMapper.class)
                .register(RestApiException.Mapper.class)
                .register(provider);

        register(PairingApi.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(pairingApi).to(PairingApi.class);
            }
        });
    }
}

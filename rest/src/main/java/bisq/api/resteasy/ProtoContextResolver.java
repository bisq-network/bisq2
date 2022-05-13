package bisq.api.resteasy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProtoContextResolver implements ContextResolver<ObjectMapper> {
    ObjectMapper mapper;

    public ProtoContextResolver() {
        mapper = new ObjectMapper();
//        mapper.registerModule()

    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}

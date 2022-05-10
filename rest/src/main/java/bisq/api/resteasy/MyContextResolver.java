package bisq.api.resteasy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MyContextResolver implements ContextResolver<ObjectMapper> {
    ObjectMapper mapper;

    public MyContextResolver() {
        mapper = new ObjectMapper();
//        mapper.registerModule()

    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}

package bisq.api;

import bisq.common.proto.Proto;
import bisq.security.KeyPairProtoUtil;
import com.google.protobuf.util.JsonFormat;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.KeyPair;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class KeyPairWriter implements MessageBodyWriter<KeyPair> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.isAssignableFrom(Proto.class);
    }

    @Override
    public void writeTo(KeyPair keyPair,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(JsonFormat.printer().print(KeyPairProtoUtil.toProto(keyPair)).getBytes());
    }
}

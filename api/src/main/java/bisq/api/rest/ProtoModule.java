package bisq.api.rest;

import bisq.common.proto.Proto;
import bisq.security.KeyPairProtoUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.security.KeyPair;

public class ProtoModule extends SimpleModule {
    public ProtoModule() {
        super("ProtoModule", new Version(1, 0, 0, null, null, null));
        addSerializer(new ProtoSerializer());
        addSerializer(new KeyPairSerializer());
    }

    public static class ProtoSerializer extends StdSerializer<Proto> {
        protected ProtoSerializer() {
            super(Proto.class);
        }

        @Override
        public void serialize(Proto proto, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeRawValue(JsonFormat.printer().print(proto.toProto()));
        }
    }

    public static class KeyPairSerializer extends StdSerializer<KeyPair> {
        protected KeyPairSerializer() {
            super(KeyPair.class);
        }

        @Override
        public void serialize(KeyPair kp, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeRawValue(JsonFormat.printer().print(KeyPairProtoUtil.toProto(kp)));
        }
    }
}

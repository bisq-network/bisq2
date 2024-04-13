package bisq.security.keys;

import bisq.common.proto.PersistableProto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class TorKeyPair implements PersistableProto {
    @ToString.Exclude
    private final byte[] privateKey;
    @ToString.Exclude
    private final byte[] publicKey;
    @EqualsAndHashCode.Include
    @Getter
    private final String onionAddress;

    public TorKeyPair(byte[] privateKey, byte[] publicKey) {
        this(privateKey, publicKey, TorKeyGeneration.getOnionAddressFromPublicKey(publicKey));
    }

    public TorKeyPair(byte[] privateKey, byte[] publicKey, String onionAddress) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.onionAddress = onionAddress;
    }

    @Override
    public bisq.security.protobuf.TorKeyPair toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    @Override
    public bisq.security.protobuf.TorKeyPair.Builder getBuilder(boolean ignoreAnnotation) {
        return bisq.security.protobuf.TorKeyPair.newBuilder()
                .setPrivateKey(ByteString.copyFrom(privateKey))
                .setPublicKey(ByteString.copyFrom(publicKey))
                .setOnionAddress(onionAddress);
    }

    public static TorKeyPair fromProto(bisq.security.protobuf.TorKeyPair proto) {
        return new TorKeyPair(proto.getPrivateKey().toByteArray(),
                proto.getPublicKey().toByteArray(),
                proto.getOnionAddress());
    }
}

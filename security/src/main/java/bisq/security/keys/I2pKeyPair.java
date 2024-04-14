package bisq.security.keys;

import bisq.common.proto.PersistableProto;
import bisq.security.protobuf.I2PKeyPair;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class I2pKeyPair implements PersistableProto {
    @ToString.Exclude
    private final byte[] privateKey;
    private final byte[] publicKey;
    @EqualsAndHashCode.Include
    @Getter
    private final String destination;

    public I2pKeyPair(byte[] privateKey, byte[] publicKey) {
        this(privateKey, publicKey, I2pKeyGeneration.getDestinationFromPublicKey(publicKey));
    }

    public I2pKeyPair(byte[] privateKey, byte[] publicKey, String destination) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.destination = destination;
    }

    @Override
    public bisq.security.protobuf.I2PKeyPair toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public I2PKeyPair.Builder getBuilder(boolean serializeForHash) {
        return I2PKeyPair.newBuilder()
                .setPrivateKey(ByteString.copyFrom(privateKey))
                .setPublicKey(ByteString.copyFrom(publicKey))
                .setDestination(destination);
    }

    public static I2pKeyPair fromProto(bisq.security.protobuf.I2PKeyPair proto) {
        return new I2pKeyPair(proto.getPrivateKey().toByteArray(),
                proto.getPublicKey().toByteArray(),
                proto.getDestination());
    }
}
package bisq.security.keys;

import bisq.common.proto.PersistableProto;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@ToString
@Getter
public class KeyBundle implements PersistableProto {
    @ToString.Exclude
    private final KeyPair keyPair;
    private final TorKeyPair torKeyPair;
    private final I2PKeyPair i2PKeyPair;
    private final String keyId;
    @ToString.Exclude
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final byte[] encodedPrivateKey;
    @ToString.Exclude
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final byte[] encodedPublicKey;
    private transient boolean hadEmptyI2PKeyPair;

    public KeyBundle(String keyId, KeyPair keyPair, TorKeyPair torKeyPair, I2PKeyPair i2PKeyPair) {
        this.keyId = keyId;
        encodedPrivateKey = keyPair.getPrivate().getEncoded();
        encodedPublicKey = keyPair.getPublic().getEncoded();
        this.keyPair = keyPair;
        this.torKeyPair = torKeyPair;
        this.i2PKeyPair = i2PKeyPair;
    }

    @Override
    public bisq.security.protobuf.KeyBundle toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.KeyBundle.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.KeyBundle.newBuilder()
                .setKeyId(keyId)
                .setKeyPair(KeyPairProtoUtil.toProto(getKeyPair()))
                .setI2PKeyPair(i2PKeyPair.toProto(serializeForHash))
                .setTorKeyPair(torKeyPair.toProto(serializeForHash));
    }

    public static KeyBundle fromProto(bisq.security.protobuf.KeyBundle proto) {
        // From pre 2.1.8 versions we did not have an i2p key, thus we create one when reading the protobuf
        boolean hasI2PKeyPair = proto.hasI2PKeyPair();
        I2PKeyPair i2PKeyPair = hasI2PKeyPair
                ? I2PKeyPair.fromProto(proto.getI2PKeyPair())
                : I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(proto.getKeyId(),
                KeyPairProtoUtil.fromProto(proto.getKeyPair()),
                TorKeyPair.fromProto(proto.getTorKeyPair()),
                i2PKeyPair);
        keyBundle.hadEmptyI2PKeyPair = !hasI2PKeyPair;
        return keyBundle;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof KeyBundle keyBundle)) return false;

        return Objects.equals(torKeyPair, keyBundle.torKeyPair) &&
                Objects.equals(keyId, keyBundle.keyId) &&
                Arrays.equals(encodedPrivateKey, keyBundle.encodedPrivateKey) &&
                Arrays.equals(encodedPublicKey, keyBundle.encodedPublicKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(torKeyPair);
        result = 31 * result + Objects.hashCode(keyId);
        result = 31 * result + Arrays.hashCode(encodedPrivateKey);
        result = 31 * result + Arrays.hashCode(encodedPublicKey);
        return result;
    }
}

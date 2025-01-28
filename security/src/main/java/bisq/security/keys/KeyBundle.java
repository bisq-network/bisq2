package bisq.security.keys;

import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

@ToString
@Getter
@EqualsAndHashCode
public class KeyBundle implements PersistableProto {
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final KeyPair keyPair;
    private final TorKeyPair torKeyPair;
    private final String keyId;
    @ToString.Exclude
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final byte[] encodedPrivateKey;
    @ToString.Exclude
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final byte[] encodedPublicKey;
    // private final I2pKeyPair i2PKeyPair;

    public KeyBundle(String keyId, KeyPair keyPair, TorKeyPair torKeyPair/*, I2pKeyPair i2PKeyPair*/) {
        this.keyId = keyId;
        encodedPrivateKey = keyPair.getPrivate().getEncoded();
        encodedPublicKey = keyPair.getPublic().getEncoded();
        this.keyPair = keyPair;
        this.torKeyPair = torKeyPair;
        /* this.i2PKeyPair = i2PKeyPair;*/
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
                /* .setI2PKeyPair(i2PKeyPair.toProto(serializeForHash))*/
                .setTorKeyPair(torKeyPair.toProto(serializeForHash));
    }

    public static KeyBundle fromProto(bisq.security.protobuf.KeyBundle proto) {
        return new KeyBundle(proto.getKeyId(),
                KeyPairProtoUtil.fromProto(proto.getKeyPair()),
                TorKeyPair.fromProto(proto.getTorKeyPair())/*,
                I2pKeyPair.fromProto(proto.getI2PKeyPair())*/);
    }
}

package bisq.security.keys;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

@ToString
@Getter
@EqualsAndHashCode
public class KeyBundle implements Proto {
    private final KeyPair keyPair;
    private final TorKeyPair torKeyPair;
    // private final I2pKeyPair i2PKeyPair;

    public KeyBundle(KeyPair keyPair, TorKeyPair torKeyPair/*, I2pKeyPair i2PKeyPair*/) {
        this.keyPair = keyPair;
        this.torKeyPair = torKeyPair;
        /* this.i2PKeyPair = i2PKeyPair;*/
    }

    @Override
    public bisq.security.protobuf.KeyBundle toProto() {
        return bisq.security.protobuf.KeyBundle.newBuilder()
                .setKeyPair(KeyPairProtoUtil.toProto(keyPair))
                .setTorKeyPair(torKeyPair.toProto())
                /* .setI2PKeyPair(i2PKeyPair.toProto())*/
                .build();
    }

    public static KeyBundle fromProto(bisq.security.protobuf.KeyBundle proto) {
        return new KeyBundle(KeyPairProtoUtil.fromProto(proto.getKeyPair()),
                TorKeyPair.fromProto(proto.getTorKeyPair())/*,
                I2pKeyPair.fromProto(proto.getI2PKeyPair())*/);
    }
}

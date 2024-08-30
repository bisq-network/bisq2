package bisq.security.keys;

import bisq.common.proto.PersistableProto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.SHA512Digest;

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
    public bisq.security.protobuf.TorKeyPair toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.TorKeyPair.Builder getBuilder(boolean serializeForHash) {
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

    public String getBase64SecretScalar() {
        // Key Format definition:
        // https://gitlab.torproject.org/tpo/core/torspec/-/blob/main/control-spec.txt
        byte[] secretScalar = generateSecretScalar(privateKey);
        return java.util.Base64.getEncoder().encodeToString(secretScalar);
    }

    /**
     * The format how the private key is stored in the tor directory
     */
    public String getPrivateKeyInOpenSshFormat() {
        return "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                getBase64SecretScalar() + "\n" +
                "-----END OPENSSH PRIVATE KEY-----\n";
    }

    private static byte[] generateSecretScalar(byte[] privateKey) {
        // https://www.rfc-editor.org/rfc/rfc8032#section-5.1
        SHA512Digest sha512Digest = new SHA512Digest();
        sha512Digest.update(privateKey, 0, privateKey.length);

        byte[] secretScalar = new byte[64];
        sha512Digest.doFinal(secretScalar, 0);

        secretScalar[0] &= (byte) 248;
        secretScalar[31] &= 127;
        secretScalar[31] |= 64;

        return secretScalar;
    }
}

package bisq.desktop.common.data;

import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;
import java.util.Objects;

@Getter
@ToString
public final class KeyPairAndId {
    private final String keyId;
    private final KeyPair keyPair;

    public KeyPairAndId(String keyId, KeyPair keyPair) {
        this.keyId = keyId;
        this.keyPair = keyPair;
    }

    // KeyPair does not implement equals and hashCode, though the public and private key implementations do.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyPairAndId that = (KeyPairAndId) o;

        if (!Objects.equals(keyId, that.keyId)) return false;
        return keyPair.getPublic().equals(that.keyPair.getPublic()) &&
                keyPair.getPrivate().equals(that.keyPair.getPrivate());
    }

    @Override
    public int hashCode() {
        int result = keyId != null ? keyId.hashCode() : 0;
        result = 31 * result + keyPair.getPublic().hashCode();
        result = 31 * result + keyPair.getPrivate().hashCode();
        return result;
    }
}

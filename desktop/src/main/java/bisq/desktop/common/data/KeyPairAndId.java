package bisq.desktop.common.data;

import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;
import java.util.Arrays;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyPairAndId that = (KeyPairAndId) o;

        if (!Objects.equals(keyId, that.keyId)) return false;
        return Arrays.equals(keyPair.getPublic().getEncoded(), that.keyPair.getPublic().getEncoded()) &&
                Arrays.equals(keyPair.getPrivate().getEncoded(), that.keyPair.getPrivate().getEncoded());
    }

    @Override
    public int hashCode() {
        int result = keyId != null ? keyId.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(keyPair.getPublic().getEncoded());
        result = 31 * result + Arrays.hashCode(keyPair.getPrivate().getEncoded());
        return result;
    }
}

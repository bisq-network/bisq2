package bisq.desktop.primary.overlay.onboarding.profile;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

@Getter
@ToString
@EqualsAndHashCode
public final class KeyPairAndId {
    private final String keyId;
    private final KeyPair keyPair;

    public KeyPairAndId(String keyId, KeyPair keyPair) {
        this.keyId = keyId;
        this.keyPair = keyPair;
    }
}

package bisq.desktop.primary.overlay.onboarding.profile;

import bisq.security.pow.ProofOfWork;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;

@Getter
@ToString
@EqualsAndHashCode
public final class TempIdentity {
    private final String profileId;
    private final String tempKeyId;
    private final KeyPair tempKeyPair;
    private final ProofOfWork proofOfWork;

    public TempIdentity(String profileId, String tempKeyId, KeyPair tempKeyPair, ProofOfWork proofOfWork) {
        this.profileId = profileId;
        this.tempKeyId = tempKeyId;
        this.tempKeyPair = tempKeyPair;
        this.proofOfWork = proofOfWork;
    }
}

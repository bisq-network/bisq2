package bisq.desktop.primary.overlay.onboarding.profile;

import bisq.security.pow.ProofOfWork;

import java.security.KeyPair;

public record TempIdentity(String profileId, String tempKeyId, KeyPair tempKeyPair, ProofOfWork proofOfWork) {
}

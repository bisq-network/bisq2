package bisq.network.p2p.node.handshake;

import bisq.network.common.Address;
import bisq.security.TorSignatureUtil;
import bisq.security.keys.TorKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.CryptoException;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OnionAddressValidation {
    static final long MAX_SIG_AGE = TimeUnit.HOURS.toMillis(2);

    private static String buildMessageForSigning(Address signersAddress, Address verifiersAddress, long date) {
        return signersAddress.getFullAddress() + "|" + verifiersAddress.getFullAddress() + "@" + date;
    }

    static Optional<byte[]> sign(Address myAddress, Address peerAddress, long date, byte[] privateKey) {
        if (!myAddress.isTorAddress() || !peerAddress.isTorAddress()) {
            return Optional.empty();
        }
        String message = buildMessageForSigning(myAddress, peerAddress, date);
        try {
            return Optional.of(TorSignatureUtil.sign(privateKey, message.getBytes()));
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean verify(Address myAddress, Address peerAddress, long date, Optional<byte[]> signature) {
        if (!myAddress.isTorAddress() || !peerAddress.isTorAddress()) {
            return true;
        }

        if (signature.isEmpty()) {
            return false;
        }

        if (!isDateWithinTolerance(date)) {
            log.warn("{}'s proof failed because the signatureDate=[{}] " +
                            "is outside the 2 hour tolerance. The current time is [{}].",
                    peerAddress.getFullAddress(),
                    new Date(date),
                    new Date(System.currentTimeMillis()));
            return false;
        }

        String message = buildMessageForSigning(peerAddress, myAddress, date);
        byte[] pubKey = TorKeyUtils.getPublicKeyFromOnionAddress(peerAddress.getHost());
        return TorSignatureUtil.verify(pubKey, message.getBytes(), signature.get());
    }

    private static boolean isDateWithinTolerance(long date) {
        return Math.abs(System.currentTimeMillis() - date) <= MAX_SIG_AGE;
    }
}

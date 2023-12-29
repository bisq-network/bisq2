package bisq.network.p2p.node.handshake;

import bisq.network.common.Address;
import bisq.security.TorSignatureUtil;
import bisq.security.keys.TorKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.CryptoException;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OnionAddressValidation {
    private static final long MAX_SIG_AGE = TimeUnit.HOURS.toMillis(2);

    private static String buildMessageForSigning(Address signersAddress, Address verifiersAddress, long date) {
        return signersAddress.getFullAddress() + "|" + verifiersAddress.getFullAddress() + "@" + date;
    }

    static Optional<byte[]> sign(Address myAddress, Address peerAddress, long date, byte[] privateKey) {
        if (!peerAddress.isTorAddress()) {
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
        if (!peerAddress.isTorAddress()) {
            return true;
        }
        String errorMsg = "Peer onion address proof failed because the signatureDate is outside the 2 hour tolerance: " +
                peerAddress.getFullAddress() +
                ", \nsignatureDate: " + new Date(date) +
                ", \nmy date: " + new Date(System.currentTimeMillis());
        checkArgument(Math.abs(System.currentTimeMillis() - date) <= MAX_SIG_AGE, errorMsg);

        String message = buildMessageForSigning(peerAddress, myAddress, date);
        byte[] pubKey = TorKeyUtils.getPublicKeyFromOnionAddress(peerAddress.getHost());
        return TorSignatureUtil.verify(pubKey, message.getBytes(), signature.orElseThrow());
    }
}

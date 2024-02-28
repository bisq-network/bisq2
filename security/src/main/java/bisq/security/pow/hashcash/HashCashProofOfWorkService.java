/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.security.pow.hashcash;

import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * HashCash implementation for proof of work
 * It doubles required work by log2Difficulty increase (adding one leading zero).
 * <p>
 * See <a href="https://www.hashcash.org/papers/hashcash.pdf">hashcash.pdf</a>
 */
@Slf4j
public class HashCashProofOfWorkService extends ProofOfWorkService {
    public HashCashProofOfWorkService() {
    }

    @Override
    public ProofOfWork mint(byte[] payload,
                            byte[] challenge,
                            double difficulty) {
        long ts = System.currentTimeMillis();
        int log2Difficulty = toNumLeadingZeros(difficulty);
        byte[] hash;
        long counter = 0;
        do {
            hash = toSha256Hash(payload, challenge, ++counter);
        }
        while (numberOfLeadingZeros(hash) <= log2Difficulty);
        byte[] solution = Longs.toByteArray(counter);
        return new ProofOfWork(payload, counter, challenge, difficulty, solution, System.currentTimeMillis() - ts);
    }

    @Override
    public boolean verify(ProofOfWork proofOfWork) {
        byte[] hash = toSha256Hash(proofOfWork.getPayload(),
                proofOfWork.getChallenge(),
                proofOfWork.getSolution());
        return numberOfLeadingZeros(hash) > toNumLeadingZeros(proofOfWork.getDifficulty());
    }

    @Override
    public byte[] getChallenge(String itemId, String ownerId) {
        return getBytes(itemId + ownerId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////

    private static byte[] getBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @VisibleForTesting
    static int numberOfLeadingZeros(byte[] bytes) {
        int numberOfLeadingZeros = 0;
        for (int i = 0; i < bytes.length; i++) {
            numberOfLeadingZeros += numberOfLeadingZeros(bytes[i]);
            if (numberOfLeadingZeros < 8 * (i + 1)) {
                break;
            }
        }
        return numberOfLeadingZeros;
    }

    private static byte[] toSha256Hash(byte[] payload, byte[] challenge, long counter) {
        return toSha256Hash(payload, challenge, Longs.toByteArray(counter));
    }

    private static byte[] toSha256Hash(byte[] payload, byte[] challenge, byte[] solution) {
        byte[] preImage = org.bouncycastle.util.Arrays.concatenate(payload,
                challenge,
                solution);
        return DigestUtil.sha256(preImage);
    }


    // Borrowed from Integer.numberOfLeadingZeros and adjusted for byte
    @VisibleForTesting
    static int numberOfLeadingZeros(byte i) {
        if (i <= 0)
            return i == 0 ? 8 : 0;
        int n = 7;
        if (i >= 1 << 4) {
            n -= 4;
            i >>>= 4;
        }
        if (i >= 1 << 2) {
            n -= 2;
            i >>>= 2;
        }
        return n - (i >>> 1);
    }

    // round up to nearest power-of-two and take the base-2 log
    @VisibleForTesting
    static int toNumLeadingZeros(double difficulty) {
        return Math.getExponent(Math.max(Math.nextDown(difficulty), 0.5)) + 1;
    }
}

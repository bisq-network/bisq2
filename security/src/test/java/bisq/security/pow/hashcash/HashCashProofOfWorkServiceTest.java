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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static bisq.security.pow.hashcash.HashCashProofOfWorkService.numberOfLeadingZeros;
import static bisq.security.pow.hashcash.HashCashProofOfWorkService.toNumLeadingZeros;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashCashProofOfWorkServiceTest {

    private final static Logger log = LoggerFactory.getLogger(HashCashProofOfWorkServiceTest.class);

    @Test
    public void testNumberOfLeadingZeros() {
        assertEquals(8, numberOfLeadingZeros((byte) 0x0));
        assertEquals(0, numberOfLeadingZeros((byte) 0xFF));
        assertEquals(6, numberOfLeadingZeros((byte) 0x2));
        assertEquals(2, numberOfLeadingZeros(Byte.parseByte("00100000", 2)));
        assertEquals(1, numberOfLeadingZeros(new byte[]{Byte.parseByte("01000000", 2), Byte.parseByte("00000000", 2)}));
        assertEquals(9, numberOfLeadingZeros(new byte[]{Byte.parseByte("00000000", 2), Byte.parseByte("01000000", 2)}));
        assertEquals(17, numberOfLeadingZeros(new byte[]{Byte.parseByte("00000000", 2), Byte.parseByte("00000000", 2), Byte.parseByte("01000000", 2)}));
        assertEquals(9, numberOfLeadingZeros(new byte[]{Byte.parseByte("00000000", 2), Byte.parseByte("01010000", 2)}));
    }

    @Test
    public void testToNumLeadingZeros() {
        assertEquals(0, toNumLeadingZeros(-1.0));
        assertEquals(0, toNumLeadingZeros(0.0));
        assertEquals(0, toNumLeadingZeros(1.0));
        assertEquals(1, toNumLeadingZeros(1.1));
        assertEquals(1, toNumLeadingZeros(2.0));
        assertEquals(8, toNumLeadingZeros(256.0));
        assertEquals(1024, toNumLeadingZeros(Double.POSITIVE_INFINITY));
    }

    @Test
    public void testDiffIncrease() throws ExecutionException, InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            run(i, stringBuilder);
        }
        log.info(stringBuilder.toString());

//        Test results on 10-core M2 Pro:
//        Minting 1000 tokens with > 0 leading zeros  took 0.096 ms per token and 2 iterations in average. Verification took 0.004 ms per token.
//        Minting 1000 tokens with > 1 leading zeros  took 0.007 ms per token and 4 iterations in average. Verification took 0.001 ms per token.
//        Minting 1000 tokens with > 2 leading zeros  took 0.012 ms per token and 8 iterations in average. Verification took 0.001 ms per token.
//        Minting 1000 tokens with > 3 leading zeros  took 0.015 ms per token and 17 iterations in average. Verification took 0.001 ms per token.
//        Minting 1000 tokens with > 4 leading zeros  took 0.023 ms per token and 33 iterations in average. Verification took 0.001 ms per token.
//        Minting 1000 tokens with > 5 leading zeros  took 0.046 ms per token and 66 iterations in average. Verification took 0.001 ms per token.
//        Minting 1000 tokens with > 6 leading zeros  took 0.087 ms per token and 131 iterations in average. Verification took 0.0 ms per token.
//        Minting 1000 tokens with > 7 leading zeros  took 0.15 ms per token and 240 iterations in average. Verification took 0.001 ms per token.
//        Minting 1000 tokens with > 8 leading zeros  took 0.318 ms per token and 526 iterations in average. Verification took 0.001 ms per token.
    }

    private void run(int log2Difficulty, StringBuilder stringBuilder) throws ExecutionException, InterruptedException {
        double difficulty = Math.scalb(1.0, log2Difficulty);
        int numTokens = 1000;
        byte[] payload = RandomStringUtils.random(50, true, true).getBytes(StandardCharsets.UTF_8);
        long ts = System.currentTimeMillis();
        List<ProofOfWork> tokens = new ArrayList<>();
        for (int i = 0; i < numTokens; i++) {
            byte[] challenge = DigestUtil.sha256(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            tokens.add(new HashCashProofOfWorkService().mint(payload, challenge, difficulty));
        }
        double size = tokens.size();
        long ts2 = System.currentTimeMillis();
        long averageCounter = Math.round(tokens.stream().mapToLong(ProofOfWork::getCounter).average().orElse(0));
        boolean allValid = tokens.stream().allMatch(new HashCashProofOfWorkService()::verify);
        assertTrue(allValid);
        double time1 = (System.currentTimeMillis() - ts) / size;
        double time2 = (System.currentTimeMillis() - ts2) / size;
        stringBuilder.append("\nMinting ").append(numTokens)
                .append(" tokens with > ").append(log2Difficulty)
                .append(" leading zeros  took ").append(time1)
                .append(" ms per token and ").append(averageCounter)
                .append(" iterations in average. Verification took ").append(time2)
                .append(" ms per token.");
    }
}
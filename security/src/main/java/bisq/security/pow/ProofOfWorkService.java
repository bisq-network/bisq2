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

package bisq.security.pow;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

// Borrowed from: https://github.com/bisq-network/bisq
@Slf4j
public abstract class ProofOfWorkService {
    public ProofOfWorkService() {
    }

    public CompletableFuture<ProofOfWork> mintAsync(byte[] payload,
                                                    byte[] challenge,
                                                    double difficulty) {
        return CompletableFuture.supplyAsync(() -> mint(payload, challenge, difficulty));
    }

    public abstract ProofOfWork mint(byte[] payload, byte[] challenge, double difficulty);

    public abstract boolean verify(ProofOfWork proofOfWork);

    public byte[] asUtf8Bytes(String itemId) {
        return itemId.getBytes(StandardCharsets.UTF_8);
    }

    public abstract byte[] getChallenge(String itemId, String ownerId);

    public CompletableFuture<ProofOfWork> mintAsync(String itemId, String ownerId, double difficulty) {
        return mintAsync(asUtf8Bytes(itemId), getChallenge(itemId, ownerId), difficulty);
    }

    public boolean verify(ProofOfWork proofOfWork,
                          String itemId,
                          String ownerId,
                          double controlDifficulty) {
        byte[] controlChallenge = getChallenge(itemId, ownerId);
        return Arrays.equals(proofOfWork.getChallenge(), controlChallenge) &&
                proofOfWork.getDifficulty() >= controlDifficulty &&
                verify(proofOfWork);
    }

    private void printStatistics() {
        CompletableFuture.runAsync(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                double diff = Math.pow(2, i);
                List<Long> tsList = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    long ts = System.currentTimeMillis();
                    byte[] bytes = new byte[1024];
                    new Random().nextBytes(bytes);
                    mintAsync(bytes, null, diff).join();
                    tsList.add(System.currentTimeMillis() - ts);
                }
                double average = tsList.stream().mapToLong(e -> e).average().getAsDouble();
                long min = tsList.stream().mapToLong(e -> e).min().getAsLong();
                long max = tsList.stream().mapToLong(e -> e).max().getAsLong();
                sb.append("\nDifficulty: Math.pow(2, ").append(i).append(") = ").append((int) diff);
                sb.append(": average=").append(average);
                sb.append(", min=").append(min);
                sb.append(", max=").append(max);
                sb.append(", tsList=").append(tsList);
            }
            log.info(sb.toString());
        });
        
/*
Test results with a i9 13 gen laptop:
Difficulty: Math.pow(2, 0) = 1: average=0.1, min=0, max=1, tsList=[0, 0, 0, 0, 0, 0, 0, 1, 0, 0]
Difficulty: Math.pow(2, 1) = 2: average=0.0, min=0, max=0, tsList=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Difficulty: Math.pow(2, 2) = 4: average=0.1, min=0, max=1, tsList=[0, 0, 0, 0, 1, 0, 0, 0, 0, 0]
Difficulty: Math.pow(2, 3) = 8: average=0.0, min=0, max=0, tsList=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Difficulty: Math.pow(2, 4) = 16: average=0.1, min=0, max=1, tsList=[0, 1, 0, 0, 0, 0, 0, 0, 0, 0]
Difficulty: Math.pow(2, 5) = 32: average=0.1, min=0, max=1, tsList=[0, 0, 0, 1, 0, 0, 0, 0, 0, 0]
Difficulty: Math.pow(2, 6) = 64: average=0.3, min=0, max=1, tsList=[0, 1, 0, 0, 1, 0, 1, 0, 0, 0]
Difficulty: Math.pow(2, 7) = 128: average=0.9, min=0, max=2, tsList=[1, 1, 2, 1, 1, 2, 0, 1, 0, 0]
Difficulty: Math.pow(2, 8) = 256: average=0.6, min=0, max=2, tsList=[2, 0, 1, 1, 0, 0, 0, 1, 1, 0]
Difficulty: Math.pow(2, 9) = 512: average=0.4, min=0, max=1, tsList=[0, 1, 1, 0, 0, 1, 0, 0, 1, 0]
Difficulty: Math.pow(2, 10) = 1024: average=1.4, min=0, max=6, tsList=[1, 2, 1, 0, 1, 6, 0, 1, 2, 0]
Difficulty: Math.pow(2, 11) = 2048: average=3.3, min=0, max=10, tsList=[6, 2, 0, 1, 1, 4, 2, 5, 10, 2]
Difficulty: Math.pow(2, 12) = 4096: average=9.7, min=1, max=22, tsList=[3, 4, 22, 18, 1, 1, 18, 2, 7, 21]
Difficulty: Math.pow(2, 13) = 8192: average=13.6, min=2, max=38, tsList=[4, 38, 9, 3, 6, 27, 5, 23, 2, 19]
Difficulty: Math.pow(2, 14) = 16384: average=10.8, min=1, max=46, tsList=[26, 10, 3, 46, 1, 5, 3, 6, 4, 4]
Difficulty: Math.pow(2, 15) = 32768: average=67.6, min=1, max=311, tsList=[80, 1, 120, 19, 35, 32, 36, 4, 311, 38]
Difficulty: Math.pow(2, 16) = 65536: average=64.5, min=2, max=119, tsList=[91, 4, 111, 86, 119, 34, 52, 2, 99, 47]
Difficulty: Math.pow(2, 17) = 131072: average=118.5, min=6, max=438, tsList=[9, 121, 12, 79, 141, 438, 6, 192, 94, 93]
Difficulty: Math.pow(2, 18) = 262144: average=286.3, min=22, max=908, tsList=[253, 58, 251, 22, 392, 908, 167, 636, 67, 109]
Difficulty: Math.pow(2, 19) = 524288: average=474.6, min=89, max=1617, tsList=[188, 1617, 89, 362, 183, 475, 792, 475, 387, 178] 


Test results with a old laptop:
Difficulty: Math.pow(2, 0) = 1: average=0.1, min=0, max=1, tsList=[1, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Difficulty: Math.pow(2, 1) = 2: average=0.2, min=0, max=1, tsList=[0, 1, 0, 0, 0, 1, 0, 0, 0, 0]
Difficulty: Math.pow(2, 2) = 4: average=0.2, min=0, max=1, tsList=[0, 1, 0, 0, 0, 0, 1, 0, 0, 0]
Difficulty: Math.pow(2, 3) = 8: average=0.4, min=0, max=1, tsList=[1, 0, 1, 0, 0, 0, 1, 0, 1, 0]
Difficulty: Math.pow(2, 4) = 16: average=0.8, min=0, max=2, tsList=[1, 0, 2, 0, 1, 2, 0, 1, 1, 0]
Difficulty: Math.pow(2, 5) = 32: average=0.5, min=0, max=1, tsList=[1, 1, 0, 1, 1, 0, 0, 0, 1, 0]
Difficulty: Math.pow(2, 6) = 64: average=1.2, min=0, max=3, tsList=[0, 1, 3, 3, 1, 0, 3, 0, 0, 1]
Difficulty: Math.pow(2, 7) = 128: average=3.4, min=0, max=8, tsList=[6, 0, 4, 8, 1, 2, 1, 6, 3, 3]
Difficulty: Math.pow(2, 8) = 256: average=4.5, min=0, max=14, tsList=[1, 1, 0, 2, 12, 1, 2, 9, 3, 14]
Difficulty: Math.pow(2, 9) = 512: average=9.4, min=1, max=33, tsList=[8, 4, 1, 33, 18, 11, 10, 1, 2, 6]
Difficulty: Math.pow(2, 10) = 1024: average=32.9, min=2, max=127, tsList=[7, 2, 13, 8, 52, 63, 26, 15, 16, 127]
Difficulty: Math.pow(2, 11) = 2048: average=68.6, min=10, max=142, tsList=[18, 142, 41, 87, 68, 10, 87, 64, 98, 71]
Difficulty: Math.pow(2, 12) = 4096: average=119.8, min=19, max=266, tsList=[22, 108, 266, 143, 19, 195, 23, 216, 51, 155]
Difficulty: Math.pow(2, 13) = 8192: average=220.4, min=0, max=632, tsList=[455, 159, 632, 73, 441, 78, 178, 136, 0, 52]
Difficulty: Math.pow(2, 14) = 16384: average=189.5, min=7, max=514, tsList=[7, 128, 117, 479, 45, 158, 113, 514, 292, 42]
Difficulty: Math.pow(2, 15) = 32768: average=498.8, min=1, max=2136, tsList=[2136, 8, 38, 478, 131, 1736, 204, 106, 150, 1]
Difficulty: Math.pow(2, 16) = 65536: average=1025.8, min=9, max=2066, tsList=[1219, 565, 1003, 2066, 9, 789, 1601, 2009, 85, 912]
Difficulty: Math.pow(2, 17) = 131072: average=1647.1, min=32, max=6650, tsList=[286, 493, 6650, 32, 2169, 1538, 436, 1454, 2633, 780]
Difficulty: Math.pow(2, 18) = 262144: average=2781.1, min=101, max=6689, tsList=[101, 1700, 1100, 1292, 555, 3490, 4987, 6365, 6689, 1532]
Difficulty: Math.pow(2, 19) = 524288: average=5365.4, min=1006, max=13135, tsList=[5529, 3647, 11788, 3624, 1006, 1813, 3087, 13135, 3460, 6565] 
*/
    }
}

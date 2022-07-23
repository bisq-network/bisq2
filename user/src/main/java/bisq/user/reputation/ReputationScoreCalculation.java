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

package bisq.user.reputation;

import bisq.common.data.ByteArray;
import bisq.oracle.daobridge.model.AuthorizedProofOfBurnData;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReputationScoreCalculation {
    private static final double MAX_AGE = 30;
    private static final double BURN_BSQ_WEIGHT = 1000;
    private static final double BURN_BSQ_AGE_WEIGHT = 1;

    private static final Map<ByteArray, Long> totalScoreByHash = new ConcurrentHashMap<>();

    public static void addBurnedBsq(ByteArray pubKeyHash, Set<AuthorizedProofOfBurnData> set) {
        long totalBurnedBsqScore = set.stream()
                .mapToLong(proofOfBurnData -> getBurnedBsqScore(proofOfBurnData.getBurnedAmount(),
                        proofOfBurnData.getTime(),
                        MAX_AGE, BURN_BSQ_WEIGHT))
                .sum();
        long totalBurnedBsqAgeScore = set.stream()
                .mapToLong(proofOfBurnData -> getBurnedBsqAgeScore(proofOfBurnData.getBurnedAmount(),
                        proofOfBurnData.getTime(),
                        BURN_BSQ_AGE_WEIGHT))
                .sum();
        long totalScore = totalBurnedBsqScore + totalBurnedBsqAgeScore;
        totalScoreByHash.put(pubKeyHash, totalScore);
    }

    public static ReputationScore getReputationScore(ByteArray pubKeyHash) {
        long totalScore = totalScoreByHash.get(pubKeyHash);
        Map<ByteArray, Long> otherTotalScores = new HashMap<>(totalScoreByHash);
        otherTotalScores.remove(pubKeyHash);
        int ranking = getRanking(totalScore, otherTotalScores.values());
        double relativeRanking = totalScoreByHash.isEmpty() ? 1 : ranking / (double) otherTotalScores.size();
        double relativeScore = getRelativeScore(totalScore, otherTotalScores.values());
        return new ReputationScore(totalScore, relativeScore, ranking, relativeRanking);
    }

    // todo add tests
    @VisibleForTesting
    static double getRelativeScore(long myScore, Collection<Long> otherTotalScores) {
        long bestScore = otherTotalScores.stream().max(Comparator.comparing(Long::longValue)).orElse(myScore);
        return myScore / (double) bestScore;
    }

    @VisibleForTesting
    static int getRanking(long myScore, Collection<Long> otherTotalScores) {
        AtomicInteger rank = new AtomicInteger(0);
        otherTotalScores.stream().sorted()
                .forEach(score -> {
                    if (myScore > score) {
                        rank.incrementAndGet();
                    }
                });
        return rank.get();
    }

    @VisibleForTesting
    static long getBurnedBsqScore(long burnedAmount, long time, double maxAge, double weight) {
        long age = System.currentTimeMillis() - time;
        long ageInDays = age / TimeUnit.DAYS.toMillis(1);
        double decayFactor = Math.max(0, maxAge - ageInDays) / maxAge;
        return Math.round(burnedAmount / 100d * decayFactor * weight);
    }

    @VisibleForTesting
    static long getBurnedBsqAgeScore(long burnedAmount, long time, double weight) {
        long age = System.currentTimeMillis() - time;
        long ageInDays = age / TimeUnit.DAYS.toMillis(1);
        return Math.round(burnedAmount / 100d * ageInDays * weight);
    }
}
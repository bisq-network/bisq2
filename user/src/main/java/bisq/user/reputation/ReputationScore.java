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

import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Slf4j
@EqualsAndHashCode
@Getter
public class ReputationScore implements Comparable<ReputationScore> {
    public static final ReputationScore NONE = new ReputationScore(0, 0, 0, 0);
    private final long totalScore;
    private final double relativeScore;
    private final int ranking;
    private final double relativeRanking;

    public ReputationScore(long totalScore, double relativeScore, int ranking, double relativeRanking) {
        this.totalScore = totalScore;
        this.relativeScore = relativeScore;
        this.ranking = ranking;
        this.relativeRanking = relativeRanking;
    }

    public String getDetails() {
        long percent = Math.round(relativeScore * 10000) / 100;
        return Res.get("reputation.score.tooltip", ranking, totalScore, percent);
    }

    @Override
    public int compareTo(@NonNull ReputationScore o) {
        return Double.compare(totalScore, o.getTotalScore());
    }
}
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

package bisq.http_api.rest_api.domain.offerbook;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "ReputationScoreDto", description = "User reputation details including total score, 5-star rating, and ranking.")
public class ReputationScoreDto {
    @Schema(description = "Total reputation score of the user.", example = "1500")
    private final long totalScore;
    @Schema(description = "5-star system equivalent score (out of 5).", example = "4.8")
    private final double fiveSystemScore;
    @Schema(description = "User's ranking among peers.", example = "12")
    private final int ranking;

    public ReputationScoreDto(long totalScore, double fiveSystemScore, int ranking) {
        this.totalScore = totalScore;
        this.fiveSystemScore = fiveSystemScore;
        this.ranking = ranking;
    }
}

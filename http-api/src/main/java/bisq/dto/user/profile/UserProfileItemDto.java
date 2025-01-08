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

package bisq.dto.user.profile;

import bisq.dto.user.reputation.ReputationScoreDto;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

//todo use UserProfile and handle reputation separately
@Getter
@ToString
@EqualsAndHashCode
public class UserProfileItemDto {
    private final String userName;
    private final String nym;
    private final ReputationScoreDto reputationScore;

    @JsonCreator
    public UserProfileItemDto(String userName, String nym, ReputationScoreDto reputationScore) {
        this.userName = userName;
        this.nym = nym;
        this.reputationScore = reputationScore;
    }
}
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


import bisq.dto.DtoMappings;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;

public class UserProfileItemDtoFactory {
    public static UserProfileItemDto create(UserProfileDto userProfile,
                                            ReputationService reputationService) {
        String nym = userProfile.nym();
        String userName = userProfile.userName();
        ReputationScoreDto reputationScore = reputationService.findReputationScore(userProfile.networkId().pubKey().id())
                .map(DtoMappings.ReputationScoreMapping::fromBisq2Model)
                .orElse(DtoMappings.ReputationScoreMapping.fromBisq2Model(ReputationScore.NONE));
        return new UserProfileItemDto(userName, nym, reputationScore);
    }
}
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
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.oracle.daobridge.model.AuthorizedBondedReputationData;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class BondedReputationService extends SourceReputationService<AuthorizedBondedReputationData> {
    public static final long WEIGHT = 100;
    private static final long AGE_WEIGHT = 1;

    public BondedReputationService(NetworkService networkService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService) {
        super(networkService, userIdentityService, userProfileService);
    }

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedBondedReputationData) {
            processData((AuthorizedBondedReputationData) authenticatedData.getDistributedData());
        }
    }

    @Override
    protected ByteArray getDataKey(AuthorizedBondedReputationData data) {
        return new ByteArray(data.getHash());
    }

    @Override
    protected ByteArray getUserProfileKey(UserProfile userProfile) {
        return userProfile.getBondedReputationKey();
    }

    @Override
    protected long calculateScore(AuthorizedBondedReputationData data) {
        long score = calculateScore(data.getAmount(), data.getLockTime());
        long ageScore = calculateAgeScore(score, data.getTime());
        return score * WEIGHT + ageScore * AGE_WEIGHT;
    }

    private static long calculateScore(long amount, long lockTime) {
        return Math.round(amount / 100d + lockTime / 10000d);
    }

    private static long calculateAgeScore(long score, long time) {
        return score * getAgeInDays(time);
    }
}
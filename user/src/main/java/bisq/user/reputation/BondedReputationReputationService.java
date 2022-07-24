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

import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class BondedReputationReputationService extends SourceReputationService<AuthorizedBondedReputationData> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final double WEIGHT = 100;
    private static final double AGE_WEIGHT = 1;

    public BondedReputationReputationService(NetworkService networkService,
                                             UserIdentityService userIdentityService,
                                             UserProfileService userProfileService) {
        super(networkService,
                userIdentityService,
                userProfileService);
    }

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedBondedReputationData) {
            processData((AuthorizedBondedReputationData) authenticatedData.getDistributedData());
        }
    }

    @Override
    protected ByteArray getOpReturnHash(AuthorizedBondedReputationData data) {
        return new ByteArray(data.getHash());
    }

    @Override
    protected ByteArray getHash(UserProfile userProfile) {
        return userProfile.getBondedReputationHash();
    }

    @Override
    protected long calculateScore(AuthorizedBondedReputationData data) {
        return calculateScore(data.getAmount()) +
                calculateAgeScore(data.getAmount(), data.getTime());
    }

    private static long calculateScore(long amount) {
        return Math.round(amount / 100d * WEIGHT);
    }

    private static long calculateAgeScore(long amount, long time) {
        return Math.round(amount / 100d * getAgeInDays(time) * AGE_WEIGHT);
    }

    private static long getAgeInDays(long timeMs) {
        return (System.currentTimeMillis() - timeMs) / DAY_MS;
    }
}
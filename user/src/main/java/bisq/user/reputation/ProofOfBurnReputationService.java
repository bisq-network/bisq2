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
import bisq.oracle.daobridge.model.AuthorizedProofOfBurnData;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class ProofOfBurnReputationService extends SourceReputationService<AuthorizedProofOfBurnData> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final double MAX_AGE = 30;
    private static final double WEIGHT = 1000;
    private static final double AGE_WEIGHT = 1;

    public ProofOfBurnReputationService(NetworkService networkService,
                                        UserIdentityService userIdentityService,
                                        UserProfileService userProfileService) {
        super(networkService,
                userIdentityService,
                userProfileService);
    }

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedProofOfBurnData) {
            processData((AuthorizedProofOfBurnData) authenticatedData.getDistributedData());
        }
    }

    @Override
    protected ByteArray getOpReturnHash(AuthorizedProofOfBurnData data) {
        return new ByteArray(data.getHash());
    }

    @Override
    protected ByteArray getHash(UserProfile userProfile) {
        return userProfile.getProofOfBurnHash();
    }

    @Override
    protected long calculateScore(AuthorizedProofOfBurnData data) {
        return calculateScore(data.getAmount(), data.getTime()) +
                calculateAgeScore(data.getAmount(), data.getTime());
    }

    private static long calculateScore(long amount, long time) {
        double decayFactor = Math.max(0, MAX_AGE - getAgeInDays(time)) / MAX_AGE;
        return Math.round(amount / 100d * decayFactor * WEIGHT);
    }

    private static long calculateAgeScore(long amount, long time) {
        return Math.round(amount / 100d * getAgeInDays(time) * AGE_WEIGHT);
    }

    private static long getAgeInDays(long timeMs) {
        return (System.currentTimeMillis() - timeMs) / DAY_MS;
    }
}
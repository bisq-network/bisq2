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

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.common.data.ByteArray;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class ProofOfBurnService extends SourceReputationService<AuthorizedProofOfBurnData> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    public static final double MAX_AGE = 30;
    public static final long WEIGHT = 1000;
    private static final long AGE_WEIGHT = 1;

    public ProofOfBurnService(NetworkService networkService,
                              UserIdentityService userIdentityService,
                              UserProfileService userProfileService,
                              AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, authorizedBondedRolesService);
    }

    @Override
    protected Optional<AuthorizedProofOfBurnData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedProofOfBurnData ?
                Optional.of((AuthorizedProofOfBurnData) authorizedDistributedData) :
                Optional.empty();
    }

    @Override
    protected ByteArray getDataKey(AuthorizedProofOfBurnData data) {
        return new ByteArray(data.getHash());
    }

    @Override
    protected ByteArray getUserProfileKey(UserProfile userProfile) {
        return userProfile.getProofOfBurnKey();
    }

    @Override
    public long calculateScore(AuthorizedProofOfBurnData data) {
        long amount = data.getAmount();
        long score = calculateScore(amount, data.getTime());
        long ageScore = calculateAgeScore(amount, data.getTime());
        return score * WEIGHT + ageScore * AGE_WEIGHT;
    }

    private static long calculateScore(long amount, long time) {
        double decayFactor = Math.max(0, MAX_AGE - getAgeInDays(time)) / MAX_AGE;
        return Math.round(amount / 100d * decayFactor);
    }

    private static long calculateAgeScore(long amount, long time) {
        return amount * getAgeInDays(time);
    }
}
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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.data.ByteArray;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
public class ProofOfBurnService extends SourceReputationService<AuthorizedProofOfBurnData> {
    public static final long WEIGHT = 100;
    public static final double INITIAL_AGE_FACTOR = 0.1;
    public static final int AGE_FOR_FULL_WEIGHT_DAYS = 100;
    public static final double AGE_FOR_FULL_WEIGHT = TimeUnit.DAYS.toMillis(AGE_FOR_FULL_WEIGHT_DAYS);

    public ProofOfBurnService(NetworkService networkService,
                              UserIdentityService userIdentityService,
                              UserProfileService userProfileService,
                              BannedUserService bannedUserService,
                              AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, bannedUserService, authorizedBondedRolesService);
    }

    @Override
    protected Optional<AuthorizedProofOfBurnData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedProofOfBurnData ?
                Optional.of((AuthorizedProofOfBurnData) authorizedDistributedData) :
                Optional.empty();
    }

    @Override
    protected boolean isValidVersion(AuthorizedProofOfBurnData data) {
        // We added fields in AuthorizedProofOfBurnData in v2.1.0 and increased version in AuthorizedProofOfBurnData to 1.
        // As we publish both version 0 and version 1 objects we ignore the version 0 objects to avoid duplicates.
        return data.getVersion() > 0;
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
        var time = data.getBlockTime();
        log.error(new Date(time).toString());
        return doCalculateScore(data.getAmount(), data.getBlockTime());
    }

    public static long doCalculateScore(long amount, long blockTime) {
        checkArgument(amount >= 0);
        checkArgument(blockTime >= 0);
        // Allow max 4 hours in the future
        checkArgument(blockTime < System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4));
        long age = Math.max(0, System.currentTimeMillis() - blockTime);
        double ageFactor = Math.min(1, age / AGE_FOR_FULL_WEIGHT);
        double adjustedAgeFactor = INITIAL_AGE_FACTOR + (1 - INITIAL_AGE_FACTOR) * ageFactor;
        return MathUtils.roundDoubleToLong(amount / 100d * WEIGHT * adjustedAgeFactor);
    }
}
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
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
public class BondedReputationService extends SourceReputationService<AuthorizedBondedReputationData> {
    public static final long WEIGHT = 10;
    public static final long LOCK_TIME = 50_000;

    public BondedReputationService(NetworkService networkService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService,
                                   BannedUserService bannedUserService,
                                   AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, bannedUserService, authorizedBondedRolesService);
    }

    @Override
    protected Optional<AuthorizedBondedReputationData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedBondedReputationData ?
                Optional.of((AuthorizedBondedReputationData) authorizedDistributedData) :
                Optional.empty();
    }

    @Override
    protected boolean isDataValid(AuthorizedBondedReputationData data) {
        // We added fields in AuthorizedBondedReputationData in v2.1.0 and increased version in AuthorizedBondedReputationData to 1.
        // We had published both version 0 and 1 data, and old version 0 had no txId and blockHeight set.
        // Version 0 data with txId and blockHeight do not cause any conflict as the hashcode is the same as a version 1 data.
        // Though version 0 data without txId and blockHeight would have a diff. hashcode and would cause duplication
        // in the score calculations.
        // With 2.1.5 we do not publish version 0 data anymore, but as they have a TTL of 100 days,
        // they will be present still a while. From June 2025 on there should not no version 0 data anymore in the network
        // and this check can be removed.
        // In case would get old protobuf data where txId and blockHeight are not present, we would get an empty string
        // an 0 as values (default values for missing fields).
        return data.getTxId().length() == 64 && data.getBlockHeight() > 0;
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
    public long calculateScore(AuthorizedBondedReputationData data) {
        checkArgument(data.getLockTime() >= LOCK_TIME);
        return doCalculateScore(data.getAmount(), data.getBlockTime());
    }

    public static long doCalculateScore(long amount, long blockTime) {
        checkArgument(amount >= 0);
        checkArgument(blockTime < System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4),
                "blockTime must not be more then 4 hours in future");
        double ageBoostFactor = getAgeBoostFactor(blockTime);
        return MathUtils.roundDoubleToLong(amount / 100d * WEIGHT * ageBoostFactor);
    }
}
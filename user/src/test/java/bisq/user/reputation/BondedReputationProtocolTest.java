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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.network.NetworkService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BondedReputationProtocolTest {
    private static final String LOCKUP_TX_ID = "12".repeat(32);
    private static final String UNLOCK_TX_ID = "34".repeat(32);

    @Test
    void currentDataSignsItsVersionAndUnlockStatus() {
        AuthorizedBondedReputationData lockup = currentData(Optional.empty());
        AuthorizedBondedReputationData unlock = AuthorizedBondedReputationData.fromProto(
                lockup.toProto(false).toBuilder().setUnlockTxId(UNLOCK_TX_ID).build());
        AuthorizedBondedReputationData rewrittenAsFuture = fromProtoWithVersion(lockup, 3);

        assertThat(lockup.serializeForHash()).isNotEqualTo(unlock.serializeForHash());
        assertThat(lockup.serializeForHash()).isNotEqualTo(rewrittenAsFuture.serializeForHash());
    }

    @Test
    void legacyMalleableDataIsParseableButContributesNoScore() {
        bisq.user.protobuf.AuthorizedBondedReputationData.Builder legacyBuilder =
                currentData(Optional.empty()).toProto(false).toBuilder()
                        .setVersion(AuthorizedBondedReputationData.LEGACY_VERSION);
        AuthorizedBondedReputationData legacyLockup =
                AuthorizedBondedReputationData.fromProto(legacyBuilder.build());
        AuthorizedBondedReputationData legacyUnlock =
                AuthorizedBondedReputationData.fromProto(legacyBuilder.setUnlockTxId(UNLOCK_TX_ID).build());
        BondedReputationService service = new BondedReputationService(
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                mock(AuthorizedBondedRolesService.class));

        assertThat(legacyLockup.serializeForHash()).isEqualTo(legacyUnlock.serializeForHash());
        assertThat(legacyLockup.isCurrentVersion()).isFalse();
        assertThat(service.isDataValid(legacyLockup)).isFalse();
        assertThat(service.calculateScore(legacyLockup)).isZero();
    }

    private static AuthorizedBondedReputationData currentData(Optional<String> unlockTxId) {
        return new AuthorizedBondedReputationData(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100),
                100_000,
                new byte[20],
                50_000,
                900_000,
                LOCKUP_TX_ID,
                unlockTxId,
                false);
    }

    private static AuthorizedBondedReputationData fromProtoWithVersion(AuthorizedBondedReputationData data,
                                                                        int version) {
        return AuthorizedBondedReputationData.fromProto(
                data.toProto(false).toBuilder().setVersion(version).build());
    }
}

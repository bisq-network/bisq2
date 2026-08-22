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
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LegacyWitnessReputationContainmentTest {
    private static final String PROFILE_ID = "12".repeat(20);

    @Test
    void legacyAccountAgeDataContributesNoScore() {
        AccountAgeService service = new AccountAgeService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                mock(AuthorizedBondedRolesService.class),
                new WitnessReputationClaimRegistry());
        AuthorizedAccountAgeData data = AuthorizedAccountAgeData.fromProto(
                bisq.user.protobuf.AuthorizedAccountAgeData.newBuilder()
                        .setProfileId(PROFILE_ID)
                        .setDateBucket(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100))
                        .setVersion(AuthorizedAccountAgeData.LEGACY_VERSION)
                        .build());

        assertThat(service.calculateScore(data)).isZero();
    }

    @Test
    void legacySignedWitnessDataContributesNoScore() {
        SignedWitnessService service = new SignedWitnessService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                mock(AuthorizedBondedRolesService.class),
                new WitnessReputationClaimRegistry());
        AuthorizedSignedWitnessData data = AuthorizedSignedWitnessData.fromProto(
                bisq.user.protobuf.AuthorizedSignedWitnessData.newBuilder()
                        .setProfileId(PROFILE_ID)
                        .setDateBucket(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100))
                        .setVersion(AuthorizedSignedWitnessData.LEGACY_VERSION)
                        .build());

        assertThat(service.calculateScore(data)).isZero();
    }
}

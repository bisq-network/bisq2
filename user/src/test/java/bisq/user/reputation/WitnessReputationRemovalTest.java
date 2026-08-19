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
import bisq.common.data.ByteArray;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static bisq.bonded_roles.BondedRoleType.ORACLE_NODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WitnessReputationRemovalTest {
    private static final String PROFILE_ID = "12".repeat(20);

    @Test
    void removedAccountAgeAuthorizationClearsScoreWhileProfileIsAbsent() {
        AuthorizedBondedRolesService authorizedBondedRolesService = authorizedBondedRolesService();
        AccountAgeService service = new AccountAgeService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                authorizedBondedRolesService,
                new WitnessReputationClaimRegistry());
        AuthorizedAccountAgeData data = new AuthorizedAccountAgeData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        AuthorizedData authorizedData = authorizedData(data);
        service.onAuthorizedDataAdded(authorizedData);
        seedProcessedScore(service, data, service.calculateScore(data));

        service.onAuthorizedDataRemoved(authorizedData);

        assertThat(service.getDataSetByHash()).doesNotContainKey(profileKey());
        assertThat(service.getScoreByUserProfileId()).doesNotContainKey(PROFILE_ID);
    }

    @Test
    void removedSignedWitnessAuthorizationClearsScoreWhileProfileIsAbsent() {
        AuthorizedBondedRolesService authorizedBondedRolesService = authorizedBondedRolesService();
        SignedWitnessService service = new SignedWitnessService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                authorizedBondedRolesService,
                new WitnessReputationClaimRegistry());
        AuthorizedSignedWitnessData data = new AuthorizedSignedWitnessData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        AuthorizedData authorizedData = authorizedData(data);
        service.onAuthorizedDataAdded(authorizedData);
        seedProcessedScore(service, data, service.calculateScore(data));

        service.onAuthorizedDataRemoved(authorizedData);

        assertThat(service.getDataSetByHash()).doesNotContainKey(profileKey());
        assertThat(service.getScoreByUserProfileId()).doesNotContainKey(PROFILE_ID);
    }

    @Test
    void removingOneOracleAuthorizationRetainsAnotherOracleScore() {
        AccountAgeService service = new AccountAgeService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                authorizedBondedRolesService(),
                new WitnessReputationClaimRegistry());
        AuthorizedAccountAgeData data = new AuthorizedAccountAgeData(
                PROFILE_ID,
                dateBucket(),
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH],
                false);
        AuthorizedData firstOracleData = authorizedData(data);
        AuthorizedData secondOracleData = authorizedData(data);
        service.onAuthorizedDataAdded(firstOracleData);
        service.onAuthorizedDataAdded(secondOracleData);
        long score = service.calculateScore(data);
        seedProcessedScore(service, data, score);

        service.onAuthorizedDataRemoved(firstOracleData);

        assertThat(service.getDataSetByHash()).containsKey(profileKey());
        assertThat(service.getScoreByUserProfileId()).containsEntry(PROFILE_ID, score);
    }

    private static AuthorizedBondedRolesService authorizedBondedRolesService() {
        AuthorizedBondedRolesService service = mock(AuthorizedBondedRolesService.class);
        when(service.hasAuthorizedPubKey(any(), eq(ORACLE_NODE))).thenReturn(true);
        return service;
    }

    private static long dateBucket() {
        long exactDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100);
        return Math.floorDiv(exactDate, WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS) *
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS;
    }

    private static AuthorizedData authorizedData(AuthorizedDistributedData data) {
        return new AuthorizedData(data, KeyGeneration.generateDefaultEcKeyPair().getPublic());
    }

    private static <T extends AuthorizedDistributedData>
    void seedProcessedScore(SourceReputationService<T> service, T data, long score) {
        service.getDataSetByHash().put(profileKey(), new CopyOnWriteArraySet<>(Set.of(data)));
        service.getScoreByUserProfileId().put(PROFILE_ID, score);
    }

    private static ByteArray profileKey() {
        return new ByteArray(PROFILE_ID.getBytes(StandardCharsets.UTF_8));
    }
}

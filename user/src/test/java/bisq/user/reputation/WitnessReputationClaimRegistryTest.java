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
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WitnessReputationClaimRegistryTest {
    private static final byte[] WITNESS_NULLIFIER =
            new byte[WitnessReputationProtocol.NULLIFIER_LENGTH];

    @Test
    void repeatedOracleAuthorizationsForOneProfileAreOneClaim() {
        WitnessReputationClaimRegistry registry = new WitnessReputationClaimRegistry();
        registry.add(accountAgeData("12".repeat(20)), "12".repeat(20), WITNESS_NULLIFIER);
        registry.add(accountAgeData("12".repeat(20)), "12".repeat(20), WITNESS_NULLIFIER);

        assertThat(registry.getClaimingProfileIds(WITNESS_NULLIFIER)).containsExactly("12".repeat(20));
        assertThat(registry.hasConflict(WITNESS_NULLIFIER)).isFalse();
    }

    @Test
    void conflictIsSharedAcrossAccountAgeAndSignedWitnessSources() {
        WitnessReputationClaimRegistry registry = new WitnessReputationClaimRegistry();
        registry.add(accountAgeData("12".repeat(20)), "12".repeat(20), WITNESS_NULLIFIER);
        registry.add(signedWitnessData("34".repeat(20)), "34".repeat(20), WITNESS_NULLIFIER);

        assertThat(registry.getClaimingProfileIds(WITNESS_NULLIFIER))
                .containsExactlyInAnyOrder("12".repeat(20), "34".repeat(20));
        assertThat(registry.hasConflict(WITNESS_NULLIFIER)).isTrue();
    }

    @Test
    void conflictingCrossSourceClaimsProduceZeroScoresInBothConsumers() {
        WitnessReputationClaimRegistry registry = new WitnessReputationClaimRegistry();
        AccountAgeService accountAgeService = new AccountAgeService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                mock(AuthorizedBondedRolesService.class),
                registry);
        SignedWitnessService signedWitnessService = new SignedWitnessService(
                mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                mock(AuthorizedBondedRolesService.class),
                registry);
        long dateBucket = dateBucket();
        AuthorizedAccountAgeData accountAgeData = new AuthorizedAccountAgeData(
                "12".repeat(20), dateBucket, WITNESS_NULLIFIER, false);
        AuthorizedSignedWitnessData signedWitnessData = new AuthorizedSignedWitnessData(
                "34".repeat(20), dateBucket, WITNESS_NULLIFIER, false);

        assertThat(accountAgeService.calculateScore(accountAgeData)).isPositive();
        assertThat(signedWitnessService.calculateScore(signedWitnessData)).isPositive();

        registry.add(new AuthorizedData(accountAgeData,
                        KeyGeneration.generateDefaultEcKeyPair().getPublic()),
                accountAgeData.getProfileId(), WITNESS_NULLIFIER);
        registry.add(new AuthorizedData(signedWitnessData,
                        KeyGeneration.generateDefaultEcKeyPair().getPublic()),
                signedWitnessData.getProfileId(), WITNESS_NULLIFIER);

        assertThat(accountAgeService.calculateScore(accountAgeData)).isZero();
        assertThat(signedWitnessService.calculateScore(signedWitnessData)).isZero();
    }

    private static AuthorizedData accountAgeData(String profileId) {
        return new AuthorizedData(new AuthorizedAccountAgeData(profileId,
                dateBucket(),
                WITNESS_NULLIFIER,
                false), KeyGeneration.generateDefaultEcKeyPair().getPublic());
    }

    private static AuthorizedData signedWitnessData(String profileId) {
        return new AuthorizedData(new AuthorizedSignedWitnessData(profileId,
                dateBucket(),
                WITNESS_NULLIFIER,
                false), KeyGeneration.generateDefaultEcKeyPair().getPublic());
    }

    private static long dateBucket() {
        long exactDate = System.currentTimeMillis() - SourceReputationService.DAY_AS_MS * 100;
        return Math.floorDiv(exactDate, WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS) *
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS;
    }
}

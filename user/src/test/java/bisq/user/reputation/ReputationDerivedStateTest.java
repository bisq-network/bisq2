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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import bisq.user.reputation.data.AuthorizedTimestampData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The reputation services remove the state which they derive from authorized data and from user profiles when the
 * source goes away, and keep the scores of profiles which still have data.
 */
class ReputationDerivedStateTest {
    // Older than MAX_AGE_BOOST_DAYS, so that a score does not change while a test runs
    private static final long DATE = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(400);
    private static final PublicKey ORACLE_NODE_KEY = KeyGeneration.generateDefaultEcKeyPair().getPublic();
    private static final PublicKey OTHER_ORACLE_NODE_KEY = KeyGeneration.generateDefaultEcKeyPair().getPublic();

    private final ObservableHashMap<String, UserProfile> userProfileById = new ObservableHashMap<>();
    private ReputationService reputationService;
    private ProofOfBurnService proofOfBurnService;
    private ProfileAgeService profileAgeService;
    private UserProfile userProfile;
    private String userProfileId;

    @BeforeEach
    void setUp() {
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.getUserProfileById()).thenReturn(userProfileById);
        when(userProfileService.findUserProfile(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(userProfileById.get(invocation.<String>getArgument(0))));
        // The publications of the tests come from oracle nodes. Which keys an oracle node has is not tested here.
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.hasAuthorizedPubKey(any(), eq(BondedRoleType.ORACLE_NODE))).thenReturn(true);
        reputationService = new ReputationService(mock(PersistenceService.class),
                mock(NetworkService.class),
                mock(UserIdentityService.class, RETURNS_DEEP_STUBS),
                userProfileService,
                mock(BannedUserService.class),
                authorizedBondedRolesService);
        proofOfBurnService = reputationService.getProofOfBurnService();
        profileAgeService = reputationService.getProfileAgeService();
        proofOfBurnService.initialize();
        profileAgeService.initialize();

        userProfile = ReputationTestProfiles.createUserProfile();
        userProfileId = userProfile.getId();
    }

    @AfterEach
    void tearDown() {
        profileAgeService.shutdown();
        proofOfBurnService.shutdown();
    }

    @Test
    void removedProofOfBurnRemovesScoreAndTotalScore() {
        addUserProfile();
        AuthorizedData publication = publication(proofOfBurnData(1_000), ORACLE_NODE_KEY);
        proofOfBurnService.onAuthorizedDataAdded(publication);
        assertThat(proofOfBurnService.getScoreByUserProfileId()).containsKey(userProfileId);
        assertThat(reputationService.getScoreByUserProfileId()).containsKey(userProfileId);

        proofOfBurnService.onAuthorizedDataRemoved(publication);

        assertThat(proofOfBurnService.getDataSetByHash()).doesNotContainKey(userProfile.getProofOfBurnKey());
        assertThat(proofOfBurnService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
        assertThat(reputationService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
    }

    @Test
    void proofOfBurnStaysWhileAnotherOracleNodePublishesIt() {
        addUserProfile();
        AuthorizedProofOfBurnData data = proofOfBurnData(1_000);
        AuthorizedData publication = publication(data, ORACLE_NODE_KEY);
        AuthorizedData otherPublication = publication(data, OTHER_ORACLE_NODE_KEY);
        proofOfBurnService.onAuthorizedDataAdded(publication);
        proofOfBurnService.onAuthorizedDataAdded(otherPublication);
        long score = proofOfBurnService.getScore(userProfileId);

        proofOfBurnService.onAuthorizedDataRemoved(publication);

        assertThat(proofOfBurnService.getScoreByUserProfileId()).containsEntry(userProfileId, score);
        assertThat(reputationService.getScoreByUserProfileId()).containsEntry(userProfileId, score);

        proofOfBurnService.onAuthorizedDataRemoved(otherPublication);

        assertThat(proofOfBurnService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
        assertThat(reputationService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
    }

    @Test
    void removalOfPublicationWhichWasNotAddedKeepsTheScore() {
        addUserProfile();
        AuthorizedProofOfBurnData data = proofOfBurnData(1_000);
        proofOfBurnService.onAuthorizedDataAdded(publication(data, ORACLE_NODE_KEY));
        long score = proofOfBurnService.getScore(userProfileId);

        // Equal data under another key, for example of a key without oracle node role
        proofOfBurnService.onAuthorizedDataRemoved(publication(data, OTHER_ORACLE_NODE_KEY));

        assertThat(proofOfBurnService.getScoreByUserProfileId()).containsEntry(userProfileId, score);
    }

    @Test
    void scoreOfProfileWhichTheProfileStoreRemovedFollowsItsData() {
        addUserProfile();
        AuthorizedData first = publication(proofOfBurnData(1_000), ORACLE_NODE_KEY);
        AuthorizedData second = publication(proofOfBurnData(2_000), ORACLE_NODE_KEY);
        proofOfBurnService.onAuthorizedDataAdded(first);
        long firstScore = proofOfBurnService.getScore(userProfileId);

        // The profile store holds a profile for a shorter time than the reputation data
        userProfileById.remove(userProfileId);
        proofOfBurnService.onAuthorizedDataAdded(second);
        long bothScores = proofOfBurnService.getScore(userProfileId);
        assertThat(bothScores).isGreaterThan(firstScore);

        proofOfBurnService.onAuthorizedDataRemoved(first);
        assertThat(proofOfBurnService.getScore(userProfileId)).isEqualTo(bothScores - firstScore);
        assertThat(reputationService.getScoreByUserProfileId()).containsEntry(userProfileId, bothScores - firstScore);

        proofOfBurnService.onAuthorizedDataRemoved(second);
        assertThat(proofOfBurnService.getDataSetByHash()).doesNotContainKey(userProfile.getProofOfBurnKey());
        assertThat(proofOfBurnService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
        assertThat(reputationService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
    }

    @Test
    void profileWithoutDataIsForgottenWhenTheProfileStoreRemovesIt() {
        addUserProfile();
        userProfileById.remove(userProfileId);

        // The data waits for the profile, as at startup when the data arrives before the profile
        proofOfBurnService.onAuthorizedDataAdded(publication(proofOfBurnData(1_000), ORACLE_NODE_KEY));
        assertThat(proofOfBurnService.getDataSetByHash()).doesNotContainKey(userProfile.getProofOfBurnKey());
        assertThat(proofOfBurnService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);

        addUserProfile();
        assertThat(proofOfBurnService.getDataSetByHash()).containsKey(userProfile.getProofOfBurnKey());
        assertThat(reputationService.getScoreByUserProfileId()).containsKey(userProfileId);
    }

    @Test
    void removedTimestampOfProfileWhichTheProfileStoreRemovedRemovesProfileAge() {
        addUserProfile();
        AuthorizedData publication = publication(timestampData(DATE), ORACLE_NODE_KEY);
        profileAgeService.onAuthorizedDataAdded(publication);
        assertThat(profileAgeService.getProfileAge(userProfile)).contains(DATE);
        // The profile age has a score of 0, but the profile has a total score
        assertThat(reputationService.getScoreByUserProfileId()).containsEntry(userProfileId, 0L);
        userProfileById.remove(userProfileId);

        profileAgeService.onAuthorizedDataRemoved(publication);

        assertThat(profileAgeService.getDataSetByHash()).doesNotContainKey(userProfile.getProfileAgeKey());
        assertThat(profileAgeService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
        assertThat(reputationService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
    }

    @Test
    void removedTimestampDoesNotReturnWithItsProfile() {
        // The timestamp arrives before the profile, so it waits for the profile
        AuthorizedData publication = publication(timestampData(DATE), ORACLE_NODE_KEY);
        profileAgeService.onAuthorizedDataAdded(publication);
        profileAgeService.onAuthorizedDataRemoved(publication);

        addUserProfile();

        assertThat(profileAgeService.getProfileAge(userProfile)).isEmpty();
        assertThat(profileAgeService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
        assertThat(reputationService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
    }

    @Test
    void removalOfOlderTimestampLeavesNewerTimestampOfAnotherOracleNode() {
        addUserProfile();
        long newerDate = DATE + TimeUnit.DAYS.toMillis(1);
        AuthorizedData older = publication(timestampData(DATE), ORACLE_NODE_KEY);
        AuthorizedData newer = publication(timestampData(newerDate), OTHER_ORACLE_NODE_KEY);
        profileAgeService.onAuthorizedDataAdded(older);
        profileAgeService.onAuthorizedDataAdded(newer);
        assertThat(profileAgeService.getProfileAge(userProfile)).contains(DATE);

        profileAgeService.onAuthorizedDataRemoved(older);
        assertThat(profileAgeService.getProfileAge(userProfile)).contains(newerDate);
        assertThat(reputationService.getScoreByUserProfileId()).containsKey(userProfileId);

        profileAgeService.onAuthorizedDataRemoved(newer);
        assertThat(profileAgeService.getProfileAge(userProfile)).isEmpty();
        assertThat(reputationService.getScoreByUserProfileId()).doesNotContainKey(userProfileId);
    }

    @Test
    void totalScoreStaysWhileAnotherSourceHasData() {
        addUserProfile();
        AuthorizedData timestamp = publication(timestampData(DATE), ORACLE_NODE_KEY);
        profileAgeService.onAuthorizedDataAdded(timestamp);
        proofOfBurnService.onAuthorizedDataAdded(publication(proofOfBurnData(1_000), ORACLE_NODE_KEY));
        long proofOfBurnScore = proofOfBurnService.getScore(userProfileId);

        profileAgeService.onAuthorizedDataRemoved(timestamp);

        assertThat(reputationService.getScoreByUserProfileId()).containsEntry(userProfileId, proofOfBurnScore);
    }

    private void addUserProfile() {
        userProfileById.put(userProfileId, userProfile);
    }

    private AuthorizedProofOfBurnData proofOfBurnData(long amount) {
        return new AuthorizedProofOfBurnData(DATE,
                amount,
                userProfile.getProofOfBurnKey().getBytes(),
                800_000,
                Long.toHexString(amount).repeat(64).substring(0, 64),
                true);
    }

    private AuthorizedTimestampData timestampData(long date) {
        return new AuthorizedTimestampData(userProfileId, date, true);
    }

    private static AuthorizedData publication(AuthorizedDistributedData data, PublicKey oracleNodeKey) {
        return new AuthorizedData(data, oracleNodeKey);
    }
}

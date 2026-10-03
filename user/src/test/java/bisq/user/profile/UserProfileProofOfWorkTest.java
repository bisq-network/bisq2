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

package bisq.user.profile;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import bisq.user.contact_list.ContactListService;
import bisq.user.identity.UserIdentityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserProfileProofOfWorkTest {
    private static final HashCashProofOfWorkService PROOF_OF_WORK_SERVICE = new HashCashProofOfWorkService();
    // Half of the minimum: valid work for its own claimed difficulty, but below the difficulty at minting
    private static final double HALF_OF_MINIMUM = UserIdentityService.MINT_NYM_DIFFICULTY / 2d;

    @Test
    void acceptsProfileMintedWithRequiredDifficulty() {
        UserProfileService service = createService();
        PubKey pubKey = createPubKey();
        ProofOfWork proofOfWork = PROOF_OF_WORK_SERVICE.mint(pubKey.getHash(), null,
                UserIdentityService.MINT_NYM_DIFFICULTY);

        UserProfile userProfile = createUserProfile(pubKey, proofOfWork);
        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(userProfile));

        assertTrue(service.findUserProfile(userProfile.getId()).isPresent());
    }

    @Test
    void rejectsProfileMintedBelowRequiredDifficulty() {
        UserProfileService service = createService();
        PubKey pubKey = createPubKey();
        ProofOfWork proofOfWork = PROOF_OF_WORK_SERVICE.mint(pubKey.getHash(), null, HALF_OF_MINIMUM);
        // The work is valid for the difficulty which the profile claims
        assertTrue(PROOF_OF_WORK_SERVICE.verify(proofOfWork));

        UserProfile userProfile = createUserProfile(pubKey, proofOfWork);
        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(userProfile));

        assertFalse(service.findUserProfile(userProfile.getId()).isPresent());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.5, -1, Double.NaN, Double.NEGATIVE_INFINITY})
    void rejectsClaimedDifficultyWithoutMinimumWork(double claimedDifficulty) {
        UserProfileService service = createService();
        PubKey pubKey = createPubKey();
        ProofOfWork minted = PROOF_OF_WORK_SERVICE.mint(pubKey.getHash(), null, 1);
        ProofOfWork proofOfWork = new ProofOfWork(minted.getPayload(), minted.getCounter(), null, claimedDifficulty,
                minted.getSolution(), 0);

        UserProfile userProfile = createUserProfile(pubKey, proofOfWork);
        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(userProfile));

        assertFalse(service.findUserProfile(userProfile.getId()).isPresent());
    }

    private static PubKey createPubKey() {
        return new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key");
    }

    private static UserProfile createUserProfile(PubKey pubKey, ProofOfWork proofOfWork) {
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        return new UserProfile(1, "nick", proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static UserProfileService createService() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<UserProfileStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any(UserProfileStore.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), eq(DbSubDirectory.SETTINGS), any(UserProfileStore.class)))
                .thenReturn(persistence);
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.getHashCashProofOfWorkService()).thenReturn(PROOF_OF_WORK_SERVICE);
        return new UserProfileService(persistenceService,
                securityService,
                mock(NetworkService.class),
                mock(ContactListService.class));
    }
}

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The index of nicknames holds a nickname while a stored profile has it.
 */
class UserProfileNickNameIndexTest {
    private static final HashCashProofOfWorkService PROOF_OF_WORK_SERVICE = new HashCashProofOfWorkService();

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<UserProfileStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any(UserProfileStore.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), eq(DbSubDirectory.SETTINGS), any(UserProfileStore.class)))
                .thenReturn(persistence);
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.getHashCashProofOfWorkService()).thenReturn(PROOF_OF_WORK_SERVICE);
        service = new UserProfileService(persistenceService,
                securityService,
                mock(NetworkService.class),
                mock(ContactListService.class));
    }

    @Test
    void nickNameIsRemovedWithItsLastProfile() {
        UserProfile alice = createUserProfile(createPubKey(), "Alice");
        UserProfile otherAlice = createUserProfile(createPubKey(), "Alice");
        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(alice));
        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(otherAlice));
        assertEquals(Set.of(alice.getNym(), otherAlice.getNym()), service.getNymsByNickName().get("Alice"));

        service.onAuthenticatedDataRemoved(new DefaultAuthenticatedData(alice));
        assertEquals(Set.of(otherAlice.getNym()), service.getNymsByNickName().get("Alice"));

        service.onAuthenticatedDataRemoved(new DefaultAuthenticatedData(otherAlice));
        assertFalse(service.getNymsByNickName().containsKey("Alice"));
    }

    @Test
    void newerVersionOfProfileWithAnotherNickNameReplacesTheNickName() {
        PubKey pubKey = createPubKey();
        UserProfile alice = createUserProfile(pubKey, "Alice");
        // The nickname is not part of the proof of work, so the owner can publish the profile with another nickname
        UserProfile mallory = createUserProfile(pubKey, "Mallory", alice.getProofOfWork());
        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(alice));

        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(mallory));

        assertEquals(Set.of("Mallory"), service.getNymsByNickName().keySet());
        service.onAuthenticatedDataRemoved(new DefaultAuthenticatedData(mallory));
        assertTrue(service.getNymsByNickName().isEmpty());
    }

    private static PubKey createPubKey() {
        return new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key");
    }

    private static UserProfile createUserProfile(PubKey pubKey, String nickName) {
        ProofOfWork proofOfWork = PROOF_OF_WORK_SERVICE.mint(pubKey.getHash(), null,
                UserIdentityService.MINT_NYM_DIFFICULTY);
        return createUserProfile(pubKey, nickName, proofOfWork);
    }

    private static UserProfile createUserProfile(PubKey pubKey, String nickName, ProofOfWork proofOfWork) {
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        return new UserProfile(1, nickName, proofOfWork, 0, networkId, "", "", "1.0.0");
    }
}

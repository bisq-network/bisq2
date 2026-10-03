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

package bisq.user.banned;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Several moderators can ban the same user profile, each with its own key. A node keeps the ban while one publication
 * of it remains which a moderator authorized, as a node which starts does (rule 18 of
 * docs/specifications/bonded-roles/authorized-data.md, finding O-3 of the review 2026-10-03).
 */
class BannedUserPublicationsTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private BannedUserService bannedUserService;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(BannedUserProfileData.class, BannedUserProfileData.META_DATA);
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(BannedUserProfileData.class.getSimpleName())
                .join();
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false, Set.of(), true);
        // As NetworkService.addDataServiceListener does
        dataService.addListener(authorizedBondedRolesService);
        bannedUserService = new BannedUserService(mockPersistenceService(), authorizedBondedRolesService, false);
        bannedUserService.initialize().join();
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    @Test
    void banStaysWhileAnotherModeratorPublishesIt() throws GeneralSecurityException {
        KeyPair moderatorKeyPair = addModerator("a");
        KeyPair otherModeratorKeyPair = addModerator("b");
        UserProfile userProfile = createUserProfile();
        BannedUserProfileData ban = new BannedUserProfileData(userProfile, false);
        AuthorizedData publication = publish(ban, moderatorKeyPair);
        AuthorizedData publicationOfOtherModerator = publish(ban, otherModeratorKeyPair);
        assertThat(bannedUserService.isUserProfileBanned(userProfile)).isTrue();

        remove(publication, moderatorKeyPair);

        assertThat(bannedUserService.isUserProfileBanned(userProfile)).isTrue();

        remove(publicationOfOtherModerator, otherModeratorKeyPair);

        assertThat(bannedUserService.isUserProfileBanned(userProfile)).isFalse();
    }

    // Any key can publish a copy of authorized data without the static flag. Its removal must not remove the ban.
    @Test
    void removalOfACopyOfAnotherKeyKeepsTheBan() throws GeneralSecurityException {
        KeyPair moderatorKeyPair = addModerator("a");
        UserProfile userProfile = createUserProfile();
        BannedUserProfileData ban = new BannedUserProfileData(userProfile, false);
        publish(ban, moderatorKeyPair);
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedData copy = publish(ban, otherKeyPair);

        remove(copy, otherKeyPair);

        assertThat(bannedUserService.isUserProfileBanned(userProfile)).isTrue();
    }

    private KeyPair addModerator(String profileIdChar) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(1234)));
        AuthorizedBondedRole moderatorRole = new AuthorizedBondedRole(profileIdChar.repeat(40),
                Hex.encode(keyPair.getPublic().getEncoded()),
                BondedRoleType.MODERATOR,
                "moderator",
                "signature",
                Optional.empty(),
                new NetworkId(addresses, new PubKey(keyPair.getPublic(), "moderator")),
                Optional.empty(),
                false);
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(moderatorRole));
        return keyPair;
    }

    // The publisher is the authorized key, which owns authorized data (see
    // docs/specifications/network/storage-ownership.md)
    private AuthorizedData publish(BannedUserProfileData ban, KeyPair keyPair) throws GeneralSecurityException {
        AuthorizedData authorizedData = new AuthorizedData(ban,
                Optional.of(SignatureUtil.sign(ban.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess()).isTrue();
        return authorizedData;
    }

    private void remove(AuthorizedData authorizedData, KeyPair keyPair) throws GeneralSecurityException {
        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess())
                .isTrue();
    }

    private static UserProfile createUserProfile() {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "user");
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(4321))),
                pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "banned user", proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static PersistenceService mockPersistenceService() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<BannedUserStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any(BannedUserStore.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getStorePath()).thenReturn(Path.of("test-store"));
        when(persistenceService.getOrCreatePersistence(any(), eq(DbSubDirectory.CACHE), any(BannedUserStore.class)))
                .thenReturn(persistence);
        return persistenceService;
    }
}

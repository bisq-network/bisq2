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

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Oracle nodes of earlier versions published bonded roles with the key pair of their network identity. Updated oracle
 * nodes publish them with their authorized key pair, which owns them. Applications use only the publication of the
 * owner (see docs/specifications/network/storage-ownership.md).
 * <p>
 * The test uses the real DataService with its stores and the real AuthorizedBondedRolesService.
 */
class AuthorizedBondedRolePublisherTest {
    @TempDir
    Path tempDir;

    private KeyPair authorizedKeyPair;
    private KeyPair identityKeyPair;
    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private AuthorizedBondedRole oracleRole;
    private AuthorizedBondedRole mediatorRole;
    private AuthorizedData authorizedMediatorRole;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        StorableTypesRegistry.register(AuthorizedBondedRole.class, AuthorizedBondedRole.META_DATA);
        authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        // Entries of earlier versions have no signature V2. A node accepts them only before the cutoff date of
        // signature V1, so the node of these tests has a time before that date.
        dataService = new DataService(new PersistenceService(tempDir),
                () -> SignedRequest.SIGNATURE_V1_CUTOFF_DATE.getTime() - 1);
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedBondedRole.class.getSimpleName())
                .join();
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false);
        // The oracle node whose authorized key authorizes the mediator role
        oracleRole = new AuthorizedBondedRole("a".repeat(40),
                Hex.encode(authorizedKeyPair.getPublic().getEncoded()),
                BondedRoleType.ORACLE_NODE,
                "oracle",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(oracleRole));
        // As NetworkService.addDataServiceListener does
        dataService.addListener(authorizedBondedRolesService);

        mediatorRole = new AuthorizedBondedRole("b".repeat(40),
                "public-key",
                BondedRoleType.MEDIATOR,
                "mediator",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
        authorizedMediatorRole = new AuthorizedData(mediatorRole,
                Optional.of(SignatureUtil.sign(mediatorRole.serializeForHash(), authorizedKeyPair.getPrivate())),
                authorizedKeyPair.getPublic());
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    // The case of the review: the oracle node published the role again with its authorized key pair, and the entry of
    // the earlier version is removed. Its expiry reaches the applications in the same way (see
    // AuthenticatedDataStorageServiceTest and OwnerPublicationTest).
    @Test
    void removalOfTheEntryOfTheEarlierVersionKeepsTheRole() throws GeneralSecurityException {
        assertThat(store.add(createEntryOfEarlierVersion()).isSuccess()).isTrue();
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedMediatorRole, identityKeyPair))
                .isSuccess()).isTrue();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);
    }

    // Applications use only the publication of the owner. The oracle node publishes its roles again with its
    // authorized key pair at startup.
    @Test
    void migrationOfTheOracleNodeMakesTheRoleAvailable() throws GeneralSecurityException {
        AddAuthenticatedDataRequest entryOfEarlierVersion = createEntryOfEarlierVersion();
        assertThat(store.add(entryOfEarlierVersion).isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);

        dataService.migratePublisherOfAuthorizedData(identityKeyPair, authorizedKeyPair);

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);
        assertThat(store.getPersistableStore().getMap().get(entryOfEarlierVersion.getStorageKey()))
                .isInstanceOf(RemoveAuthenticatedDataRequest.class);
    }

    // Until the cutoff date, anyone can publish a copy with an earlier creation date, and the store accepts it
    @Test
    void copyDoesNotRestoreARemovedRole() throws GeneralSecurityException {
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);

        assertThat(store.add(createEntryOf(KeyGeneration.generateDefaultEcKeyPair())).isSuccess()).isTrue();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    // Several oracle nodes publish the same role, each with its own authorized key. The removal of one publication must
    // not remove the role while another oracle node publishes it. Otherwise the observer of OracleNodeService would
    // also remove the publication of its own oracle node, and the removal would spread to all oracle nodes (finding F3
    // of the review 2026-09-29_16-18-18-fable-5.1).
    @Test
    void roleStaysWhileAnotherOracleNodePublishesIt() throws GeneralSecurityException {
        KeyPair otherOracleKeyPair = addOracleNode("c".repeat(40));
        AuthorizedData publicationOfOtherOracle = authorize(mediatorRole, otherOracleKeyPair);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, publicationOfOtherOracle, otherOracleKeyPair))
                .isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, publicationOfOtherOracle, otherOracleKeyPair))
                .isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    // The case of finding F3: before the cutoff date of signature V1, a node stores the copy of the publication of
    // another oracle node without signature V2, which a node of an earlier version relayed. From the cutoff date on, the
    // prune task removes the copy and notifies the applications as at expiry. The role stays, because the node still
    // holds the publication of its own oracle node.
    @Test
    void removalOfACopyWithoutSignatureV2AtTheCutoffDateKeepsTheRole() throws GeneralSecurityException {
        KeyPair otherOracleKeyPair = addOracleNode("c".repeat(40));
        AuthorizedData publicationOfOtherOracle = authorize(mediatorRole, otherOracleKeyPair);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        AddAuthenticatedDataRequest copyWithoutSignatureV2 = createEntryOf(publicationOfOtherOracle, otherOracleKeyPair);
        assertThat(store.add(copyWithoutSignatureV2).isSuccess()).isTrue();

        // As DataStorageService.isToPrune and AuthenticatedDataStorageService.pruneExpired do at the cutoff date
        store.getPersistableStore().getMap().remove(copyWithoutSignatureV2.getStorageKey());
        dataService.onRemoved(publicationOfOtherOracle);

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);
    }

    // A publication counts only if an oracle node authorized it. Anyone can publish role data which the own key
    // authorizes.
    @Test
    void publicationWhichNoOracleNodeAuthorizedDoesNotKeepTheRole() throws GeneralSecurityException {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedData publicationOfOtherKey = authorize(mediatorRole, otherKeyPair);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, publicationOfOtherKey, otherKeyPair))
                .isSuccess()).isTrue();

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    // A publication with staticPublicKeysProvided counts only if its key is a static key of this version. A later
    // version can remove a static key, and the store still holds the publications of that key from an earlier version
    // (see docs/specifications/bonded-roles/authorized-data.md).
    @Test
    void storedPublicationOfARemovedStaticKeyDoesNotKeepTheRole() throws GeneralSecurityException {
        KeyPair removedStaticKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedBondedRole roleWithStaticFlag = new AuthorizedBondedRole(mediatorRole.getProfileId(),
                mediatorRole.getAuthorizedPublicKey(),
                mediatorRole.getBondedRoleType(),
                mediatorRole.getBondUserName(),
                mediatorRole.getSignatureBase64(),
                mediatorRole.getAddressByTransportTypeMap(),
                mediatorRole.getNetworkId(),
                mediatorRole.getAuthorizingOracleNode(),
                true);
        assertThat(roleWithStaticFlag).isEqualTo(mediatorRole);
        AddAuthenticatedDataRequest publicationOfRemovedKey = AddAuthenticatedDataRequest.from(store,
                authorize(roleWithStaticFlag, removedStaticKeyPair), removedStaticKeyPair);
        // The store does not accept it now. A store which holds it from an earlier version loads it unchecked.
        assertThat(store.add(publicationOfRemovedKey).isSuccess()).isFalse();
        store.getPersistableStore().getMap().put(publicationOfRemovedKey.getStorageKey(), publicationOfRemovedKey);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    // After a restart, with the startup of the bonded role service, the node still holds the remove request of the
    // oracle node. It rejects a replay of the removed entry, which a node which missed the removal can send.
    @Test
    void restartKeepsTheRemovalOfTheRole() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authorizedMediatorRole,
                authorizedKeyPair);
        assertThat(store.add(addRequest).isSuccess()).isTrue();
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(store,
                authorizedMediatorRole,
                authorizedKeyPair);
        assertThat(store.remove(removeRequest).isSuccess()).isTrue();
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();

        PersistenceService persistenceServiceAfterRestart = new PersistenceService(tempDir);
        DataService dataServiceAfterRestart = new DataService(persistenceServiceAfterRestart);
        assertThat(persistenceServiceAfterRestart.readAllPersisted().join()).isTrue();
        AuthenticatedDataStorageService storeAfterRestart = dataServiceAfterRestart.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedBondedRole.class.getSimpleName())
                .join();
        assertThat(storeAfterRestart.getPersistableStore().getMap()).containsValue(removeRequest);
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataServiceAfterRestart));
        AuthorizedBondedRolesService authorizedBondedRolesServiceAfterRestart =
                new AuthorizedBondedRolesService(networkService, false);
        authorizedBondedRolesServiceAfterRestart.getBondedRoles().add(new BondedRole(oracleRole));
        authorizedBondedRolesServiceAfterRestart.initialize().join();
        dataServiceAfterRestart.addListener(authorizedBondedRolesServiceAfterRestart);

        try {
            assertThat(storeAfterRestart.add(addRequest).isSequenceNrInvalid()).isTrue();
            assertThat(authorizedBondedRolesServiceAfterRestart.getAuthorizedBondedRoleStream())
                    .doesNotContain(mediatorRole);
        } finally {
            authorizedBondedRolesServiceAfterRestart.shutdown().join();
            storeAfterRestart.getPersistence().persistAsync(storeAfterRestart.getPersistableStore().getClone()).join();
            dataServiceAfterRestart.shutdown();
        }
    }

    // An oracle node whose role the node knows. Its key authorizes roles.
    private KeyPair addOracleNode(String profileId) {
        KeyPair oracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(new AuthorizedBondedRole(profileId,
                Hex.encode(oracleKeyPair.getPublic().getEncoded()),
                BondedRoleType.ORACLE_NODE,
                "oracle " + profileId.charAt(0),
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false)));
        return oracleKeyPair;
    }

    private static AuthorizedData authorize(AuthorizedBondedRole role, KeyPair keyPair) throws GeneralSecurityException {
        return new AuthorizedData(role,
                Optional.of(SignatureUtil.sign(role.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }

    // The entry which the oracle node of the earlier version published with the key pair of its network identity
    private AddAuthenticatedDataRequest createEntryOfEarlierVersion() throws GeneralSecurityException {
        return createEntryOf(identityKeyPair);
    }

    private AddAuthenticatedDataRequest createEntryOf(KeyPair publisherKeyPair) throws GeneralSecurityException {
        return createEntryOf(authorizedMediatorRole, publisherKeyPair);
    }

    // An add request as nodes of earlier versions created it: without signature V2
    private AddAuthenticatedDataRequest createEntryOf(AuthorizedData authorizedData, KeyPair publisherKeyPair)
            throws GeneralSecurityException {
        // The transition rule accepts such entries only if they were created before the cutoff date. After the cutoff
        // date plus the time to live, none is valid anymore, and the transition rule and these tests can be removed.
        long cutoffDate = AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime();
        assumeTrue(System.currentTimeMillis() < cutoffDate + AuthorizedBondedRole.META_DATA.getTtl(),
                "The transition of the publisher of authorized data has ended");
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authorizedData,
                1,
                DigestUtil.hash(publisherKeyPair.getPublic().getEncoded()),
                Math.min(System.currentTimeMillis(), cutoffDate - 1));
        return new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), publisherKeyPair.getPrivate()),
                publisherKeyPair.getPublic().getEncoded(),
                publisherKeyPair.getPublic(),
                new byte[]{},
                0);
    }
}

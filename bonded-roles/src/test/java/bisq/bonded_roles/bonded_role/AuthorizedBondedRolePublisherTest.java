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
import bisq.common.application.DevMode;
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
import java.util.Set;

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
 * The test uses the real DataService with its stores and the real AuthorizedBondedRolesService. The store accepts bonded
 * roles only from the oracle node keys (rule 16 of docs/specifications/bonded-roles/authorized-data.md). In dev mode
 * these are the dev keys of AuthorizedPubKeys, whose private keys are known.
 */
class AuthorizedBondedRolePublisherTest {
    // The first two of the dev keys of AuthorizedPubKeys.DEV_PUB_KEYS
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PRIVATE_KEY_2 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104201c6685cef332eecfddb253b601c0ed89e7c822d2d03c53d5a720a860b5dc9419a00706052b8104000aa144034200044b3a5f869a4a4346fb37ca70991ff55f3ef4f0ed3a31b51238aee75858af48fddaeafb11b9c59b3e9f551ee9022a767e5f39dcea2b4a96e1c610a5f1b1a1878e";
    private static final String DEV_PUBLIC_KEY_2 = "3056301006072a8648ce3d020106052b8104000a034200044b3a5f869a4a4346fb37ca70991ff55f3ef4f0ed3a31b51238aee75858af48fddaeafb11b9c59b3e9f551ee9022a767e5f39dcea2b4a96e1c610a5f1b1a1878e";

    @TempDir
    Path tempDir;

    private KeyPair authorizedKeyPair;
    private KeyPair otherOracleKeyPair;
    private Set<String> oracleNodePubKeys;
    private KeyPair identityKeyPair;
    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private AuthorizedBondedRole mediatorRole;
    private AuthorizedData authorizedMediatorRole;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        StorableTypesRegistry.register(AuthorizedBondedRole.class, AuthorizedBondedRole.META_DATA);
        // The static oracle node keys of this version
        DevMode.setDevMode(true);
        authorizedKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_1),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_1));
        otherOracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_2),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_2));
        oracleNodePubKeys = Set.of(Hex.encode(authorizedKeyPair.getPublic().getEncoded()),
                Hex.encode(otherOracleKeyPair.getPublic().getEncoded()));
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
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false, oracleNodePubKeys,
                true);
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
        try {
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
            dataService.shutdown();
        } finally {
            DevMode.setDevMode(false);
        }
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

    // A publication counts only if an oracle node authorized it. The store rejects role data which another key
    // authorizes (rules 2 and 16 of docs/specifications/bonded-roles/authorized-data.md). The applications do not rely
    // on the store (rule 6), so the test puts such a publication into the store directly.
    @Test
    void publicationWhichNoOracleNodeAuthorizedDoesNotKeepTheRole() throws GeneralSecurityException {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AddAuthenticatedDataRequest publicationOfOtherKey = AddAuthenticatedDataRequest.from(store,
                authorize(mediatorRole, otherKeyPair), otherKeyPair);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();
        assertThat(store.add(publicationOfOtherKey).isAuthorizedKeyNotStatic()).isTrue();
        store.getPersistableStore().getMap().put(publicationOfOtherKey.getStorageKey(), publicationOfOtherKey);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedMediatorRole, authorizedKeyPair))
                .isSuccess()).isTrue();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    // A publication with staticPublicKeysProvided counts only if its key is a static key of this version. A later
    // version can remove a static key. The store neither accepts the publications of that key nor keeps them when it
    // loads (rules 4 and 5 of docs/specifications/bonded-roles/authorized-data.md). The applications do not rely on the
    // store (rule 6), so the test puts such a publication into the store directly.
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
        assertThat(store.add(publicationOfRemovedKey).isAuthorizedKeyNotStatic()).isTrue();
        store.getPersistableStore().getMap().put(publicationOfRemovedKey.getStorageKey(), publicationOfRemovedKey);
        assertThat(store.prunePersisted(store.getPersistableStore().getMutableClone()).getMap())
                .doesNotContainKey(publicationOfRemovedKey.getStorageKey());
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
                new AuthorizedBondedRolesService(networkService, false, oracleNodePubKeys,
                true);
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

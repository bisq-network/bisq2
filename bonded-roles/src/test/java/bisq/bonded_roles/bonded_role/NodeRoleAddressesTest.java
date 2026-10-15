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

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.Any;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createAddressByTransportTypeMap;
import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Nodes reach a node at the addresses of its role. A node adds the addresses of a seed node role to its seed nodes
 * without a check, so a seed node role without addresses stopped the start of every node of v2.1.13 (finding D5 of the
 * review 2026-09-30). The role data of a node without addresses fails at parsing (see
 * docs/specifications/bonded-roles/registration.md).
 * <p>
 * The store accepts bonded roles only from the oracle node keys (rule 16 of
 * docs/specifications/bonded-roles/authorized-data.md). In dev mode these are the dev keys of AuthorizedPubKeys, whose
 * private keys are known.
 */
class NodeRoleAddressesTest {
    // The first of the dev keys of AuthorizedPubKeys.DEV_PUB_KEYS. In dev mode it is an oracle node key.
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String SEED_NODE_PROFILE_ID = "c".repeat(40);

    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private NetworkService networkService;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private KeyPair oracleKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedBondedRole.class, AuthorizedBondedRole.META_DATA);
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedBondedRole", AuthorizedBondedRole.class,
                AuthorizedBondedRole.getResolver());
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedBondedRole.class.getSimpleName())
                .join();
        networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        // The static oracle node key of this version
        DevMode.setDevMode(true);
        oracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_1),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_1));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false,
                Set.of(Hex.encode(oracleKeyPair.getPublic().getEncoded())),
                true);
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        try {
            authorizedBondedRolesService.shutdown().join();
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
            dataService.shutdown();
        } finally {
            DevMode.setDevMode(false);
        }
    }

    @ParameterizedTest
    @EnumSource(BondedRoleType.class)
    void roleOfANodeMustContainTheAddressesOfTheNode(BondedRoleType bondedRoleType)
            throws UnresolvableProtobufException {
        if (bondedRoleType.isNode()) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> createRole(bondedRoleType, Optional.empty()))
                    .withMessageContaining("addresses of the node");
            var protoWithoutAddresses = createRole(bondedRoleType, Optional.of(createAddressByTransportTypeMap()))
                    .toProto(false)
                    .toBuilder()
                    .clearAddressByTransportTypeMap()
                    .build();
            assertThatThrownBy(() -> AuthorizedBondedRole.fromProto(protoWithoutAddresses))
                    .isInstanceOf(UnresolvableProtobufException.class);
        } else {
            AuthorizedBondedRole role = createRole(bondedRoleType, Optional.empty());
            assertThat(AuthorizedBondedRole.fromProto(role.toProto(false))).isEqualTo(role);
        }
    }

    // The case of the finding: a node of an earlier version stored a seed node role without addresses. The store skips
    // the entry when it loads it, and the bonded role service starts and lists the other roles. Without the rule, the
    // startup replay catches the exception of the role (see docs/specifications/network/startup-replay.md), but the
    // service lists the role.
    @Test
    void storedSeedNodeRoleWithoutAddressesDoesNotStopTheStart() throws Exception {
        AuthorizedBondedRole seedNodeRole = new AuthorizedBondedRole(SEED_NODE_PROFILE_ID,
                Hex.encode(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded()),
                BondedRoleType.SEED_NODE,
                "seed",
                "signature",
                Optional.of(createAddressByTransportTypeMap()),
                createNetworkId(),
                Optional.empty(),
                false);
        AuthorizedBondedRole mediatorRole = createRole(BondedRoleType.MEDIATOR, Optional.empty());
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorize(seedNodeRole), oracleKeyPair))
                .isSuccess()).isTrue();
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorize(mediatorRole), oracleKeyPair))
                .isSuccess()).isTrue();

        // The persisted store as an earlier version wrote it: the seed node role has no addresses. The stores do not
        // check the signatures of persisted entries again.
        bisq.network.protobuf.DataStore.Builder persisted = store.getPersistableStore().toProto(false).toBuilder();
        for (int i = 0; i < persisted.getMapEntriesCount(); i++) {
            var authenticatedData = persisted.getMapEntriesBuilder(i)
                    .getValueBuilder()
                    .getAddAuthenticatedDataRequestBuilder()
                    .getAuthenticatedSequentialDataBuilder()
                    .getAuthenticatedDataBuilder();
            var role = authenticatedData.getDistributedData().unpack(bisq.bonded_roles.protobuf.AuthorizedBondedRole.class);
            if (role.getProfileId().equals(SEED_NODE_PROFILE_ID)) {
                authenticatedData.setDistributedData(Any.pack(role.toBuilder().clearAddressByTransportTypeMap().build()));
            }
        }
        // As the persistence service does when it reads the store
        @SuppressWarnings("unchecked")
        DataStore<AuthenticatedDataRequest> loaded =
                (DataStore<AuthenticatedDataRequest>) DataStore.fromProto(persisted.build());
        store.getPersistableStore().applyPersisted(loaded);

        assertThatCode(() -> authorizedBondedRolesService.initialize().join()).doesNotThrowAnyException();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).containsExactly(mediatorRole);
        verify(networkService, never()).addSeedNodeAddressByTransport(any());
    }

    private AuthorizedData authorize(AuthorizedBondedRole role) throws GeneralSecurityException {
        return new AuthorizedData(role,
                Optional.of(SignatureUtil.sign(role.serializeForHash(), oracleKeyPair.getPrivate())),
                oracleKeyPair.getPublic());
    }

    private static AuthorizedBondedRole createRole(BondedRoleType bondedRoleType,
                                                   Optional<AddressByTransportTypeMap> addresses) {
        return new AuthorizedBondedRole("b".repeat(40),
                "public-key",
                bondedRoleType,
                "user",
                "signature",
                addresses,
                createNetworkId(),
                Optional.empty(),
                false);
    }
}

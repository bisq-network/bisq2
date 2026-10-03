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

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.PubKey;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createAddressByTransportTypeMap;
import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Users send registration requests and account timestamp requests to the oracle nodes which
 * AuthorizedBondedRolesService lists. The network layer checks only the signature of data without
 * staticPublicKeysProvided, so any key can publish an oracle node. While oracle nodes with a non-static key are allowed, the
 * service lists an oracle node only if its key is a static key of this version or the key of a known oracle node role
 * (see docs/specifications/bonded-roles/authorized-data.md). StaticOracleNodeKeysOnlyTest covers the temporary
 * restriction to the static keys.
 */
class AuthorizedOracleNodeAuthorizationTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private KeyPair oracleKeyPair;
    private KeyPair identityKeyPair;
    private AuthorizedOracleNode oracleNode;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedOracleNode.class, AuthorizedOracleNode.META_DATA);
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedOracleNode.class.getSimpleName())
                .join();
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false,
                AuthorizedPubKeys.getOracleNodePubKeys(),
                false);

        // The role of the oracle node, which another oracle node registered
        oracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        String oracleKeyAsHex = Hex.encode(oracleKeyPair.getPublic().getEncoded());
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(new AuthorizedBondedRole("a".repeat(40),
                oracleKeyAsHex,
                BondedRoleType.ORACLE_NODE,
                "oracle",
                "signature",
                Optional.of(createAddressByTransportTypeMap()),
                createNetworkId(),
                Optional.empty(),
                false)));
        // The owner of the oracle node data is its network identity, which publishes it (see OracleNodeService)
        identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        oracleNode = new AuthorizedOracleNode(new NetworkId(new AddressByTransportTypeMap(Map.of(TransportType.CLEAR,
                LocalHostAddressTypeFacade.toLocalHostAddress(1234))),
                new PubKey(identityKeyPair.getPublic(), "identity")),
                "a".repeat(40),
                oracleKeyAsHex,
                "oracle",
                "signature",
                false);
    }

    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    @Test
    void oracleNodeOfAKnownOracleRoleIsListed() throws GeneralSecurityException {
        storeAndNotify(authorize(oracleNode, oracleKeyPair));

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).containsExactly(oracleNode);
    }

    // A security manager can ban the role which authorizes the key of an oracle node after the oracle node was added.
    // The list is derived from the stored publications, so the oracle node is no longer listed.
    @Test
    void oracleNodeWhoseRoleIsBannedLaterIsNotListed() throws GeneralSecurityException {
        storeAndNotify(authorize(oracleNode, oracleKeyPair));
        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).containsExactly(oracleNode);

        authorizedBondedRolesService.getBondedRoles().forEach(bondedRole -> bondedRole.setIsBanned(true));

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).isEmpty();
    }

    @Test
    void oracleNodeOfAnUnknownKeyIsNotListed() throws GeneralSecurityException {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedOracleNode otherOracleNode = createOracleNode(Hex.encode(otherKeyPair.getPublic().getEncoded()), false);

        storeAndNotify(authorize(otherOracleNode, otherKeyPair));

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).isEmpty();
    }

    // A later version can remove a static key, and the store still holds the data of that key from an earlier version
    @Test
    void oracleNodeOfAKeyWhichIsNoStaticKeyIsNotListed() throws GeneralSecurityException {
        KeyPair removedStaticKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedOracleNode oracleNodeWithStaticFlag = createOracleNode(
                Hex.encode(removedStaticKeyPair.getPublic().getEncoded()), true);

        storeAndNotify(authorize(oracleNodeWithStaticFlag, removedStaticKeyPair));

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).isEmpty();
    }

    // The removal of a publication which no oracle node authorized does not remove the oracle node. It is removed when
    // no authorized publication of it is stored.
    @Test
    void oracleNodeStaysWhileAnAuthorizedPublicationIsStored() throws GeneralSecurityException {
        AuthorizedData publication = authorize(oracleNode, oracleKeyPair);
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, publication, identityKeyPair);
        store.getPersistableStore().getMap().put(request.getStorageKey(), request);
        authorizedBondedRolesService.onAuthorizedDataAdded(publication);

        authorizedBondedRolesService.onAuthorizedDataRemoved(authorize(oracleNode, KeyGeneration.generateDefaultEcKeyPair()));

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).containsExactly(oracleNode);

        store.getPersistableStore().getMap().remove(request.getStorageKey());
        authorizedBondedRolesService.onAuthorizedDataRemoved(publication);

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).isEmpty();
    }

    // As the DataService does: the store holds the publication when the listener is called
    private void storeAndNotify(AuthorizedData publication) throws GeneralSecurityException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, publication, identityKeyPair);
        store.getPersistableStore().getMap().put(request.getStorageKey(), request);
        authorizedBondedRolesService.onAuthorizedDataAdded(publication);
    }

    // The network identity of the oracle node owns the data and publishes it
    private AuthorizedOracleNode createOracleNode(String authorizedPublicKey, boolean staticPublicKeysProvided) {
        return new AuthorizedOracleNode(oracleNode.getNetworkId(),
                "a".repeat(40),
                authorizedPublicKey,
                "oracle",
                "signature",
                staticPublicKeysProvided);
    }

    private static AuthorizedData authorize(AuthorizedOracleNode data, KeyPair keyPair) throws GeneralSecurityException {
        return new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}

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
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.PersistenceService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * While only static oracle nodes are allowed (AuthorizedBondedRolesService.ONLY_STATIC_ORACLE_NODES), only
 * the static oracle node keys of this version may act as an oracle node. A bonded role of type ORACLE_NODE does not
 * give this authority (see docs/specifications/bonded-roles/authorized-data.md).
 */
class StaticOracleNodeKeysOnlyTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private KeyPair oracleKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedOracleNode.class, AuthorizedOracleNode.META_DATA);
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedOracleNode.class.getSimpleName())
                .join();
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        // The static oracle node key of this version
        oracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false,
                Set.of(Hex.encode(oracleKeyPair.getPublic().getEncoded())),
                true);
    }

    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    // The static oracle node key decides, not staticPublicKeysProvided, which the publisher sets
    @Test
    void oracleNodeOfAStaticOracleNodeKeyIsListed() throws GeneralSecurityException {
        AuthorizedOracleNode oracleNode = createOracleNode(Hex.encode(oracleKeyPair.getPublic().getEncoded()), false);

        storeAndNotify(oracleNode, oracleKeyPair);

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).containsExactly(oracleNode);
    }

    @Test
    void oracleNodeWithStaticFlagOfAStaticOracleNodeKeyIsListed() throws GeneralSecurityException {
        AuthorizedOracleNode oracleNode = createOracleNode(Hex.encode(oracleKeyPair.getPublic().getEncoded()), true);

        storeAndNotify(oracleNode, oracleKeyPair);

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).containsExactly(oracleNode);
    }

    @Test
    void oracleNodeOfAKeyWithAnOracleNodeRoleIsNotListed() throws GeneralSecurityException {
        KeyPair nonStaticKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        String nonStaticKeyAsHex = Hex.encode(nonStaticKeyPair.getPublic().getEncoded());
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(createOracleRole(nonStaticKeyAsHex)));

        storeAndNotify(createOracleNode(nonStaticKeyAsHex, false), nonStaticKeyPair);

        assertThat(authorizedBondedRolesService.getAuthorizedOracleNodes()).isEmpty();
        assertThat(authorizedBondedRolesService.isOracleNodeKeyAllowed(nonStaticKeyAsHex)).isFalse();
    }

    // A role of type ORACLE_NODE of another key would show an oracle node which no node accepts
    @Test
    void oracleNodeRoleOfAKeyWhichIsNoStaticOracleNodeKeyIsNotListedAndAuthorizesNothing()
            throws GeneralSecurityException {
        KeyPair nonStaticKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedBondedRole nonStaticOracleRole = createOracleRole(
                Hex.encode(nonStaticKeyPair.getPublic().getEncoded()));

        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(nonStaticOracleRole, oracleKeyPair));
        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(createMediatorRole(), nonStaticKeyPair));

        assertThat(authorizedBondedRolesService.getBondedRoles()).isEmpty();
    }

    @Test
    void oracleNodeRoleOfAStaticOracleNodeKeyIsListed() throws GeneralSecurityException {
        AuthorizedBondedRole oracleRole = createOracleRole(Hex.encode(oracleKeyPair.getPublic().getEncoded()));
        AuthorizedBondedRole mediatorRole = createMediatorRole();

        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(oracleRole, oracleKeyPair));
        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(mediatorRole, oracleKeyPair));

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream())
                .containsExactlyInAnyOrder(oracleRole, mediatorRole);
    }

    // Oracle discovery reads stored publications. Insert directly because the test supplies its own static key set.
    private void storeAndNotify(AuthorizedOracleNode oracleNode, KeyPair keyPair) throws GeneralSecurityException {
        AuthorizedData publication = authorize(oracleNode, keyPair);
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, publication, keyPair);
        store.getPersistableStore().getMap().put(request.getStorageKey(), request);
        authorizedBondedRolesService.onAuthorizedDataAdded(publication);
    }

    private static AuthorizedOracleNode createOracleNode(String authorizedPublicKey, boolean staticPublicKeysProvided) {
        return new AuthorizedOracleNode(createNetworkId(),
                "a".repeat(40),
                authorizedPublicKey,
                "oracle",
                "signature",
                staticPublicKeysProvided);
    }

    private static AuthorizedBondedRole createOracleRole(String authorizedPublicKey) {
        return new AuthorizedBondedRole("a".repeat(40),
                authorizedPublicKey,
                BondedRoleType.ORACLE_NODE,
                "oracle",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
    }

    private static AuthorizedBondedRole createMediatorRole() {
        return new AuthorizedBondedRole("b".repeat(40),
                "public-key",
                BondedRoleType.MEDIATOR,
                "mediator",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
    }

    private static AuthorizedData authorize(AuthorizedDistributedData data, KeyPair keyPair)
            throws GeneralSecurityException {
        return new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}

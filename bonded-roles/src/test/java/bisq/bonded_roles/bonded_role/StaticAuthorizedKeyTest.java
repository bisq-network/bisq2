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
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
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
import java.security.PublicKey;
import java.util.Optional;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Data with staticPublicKeysProvided is authorized by a key which is written in the code. The network layer checks
 * this when it adds the data. It does not check stored data again when it loads a store. So the application checks
 * the key against the static keys of its version: a later version can remove a key, and a node which updates still
 * holds the data which that key published.
 * <p>
 * The test uses the real DataService with its store and the real AuthorizedBondedRolesService.
 */
class StaticAuthorizedKeyTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private AuthorizedBondedRolesService authorizedBondedRolesService;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedBondedRole.class, AuthorizedBondedRole.META_DATA);
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedBondedRole.class.getSimpleName())
                .join();
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false);
        // As NetworkService.addDataServiceListener does
        dataService.addListener(authorizedBondedRolesService);
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    @Test
    void storedRootRoleOfAKeyWhichIsNoStaticKeyAuthorizesNothing() throws GeneralSecurityException {
        KeyPair removedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedData rootRole = authorize(createOracleRole(removedKeyPair.getPublic(), true), removedKeyPair);
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, rootRole, removedKeyPair);
        // The store does not accept the role, because its key is no static key
        assertThat(store.add(request).isSuccess()).isFalse();
        // A store which holds the role from an earlier version, in which the key was a static key, loads it unchecked
        store.getPersistableStore().getMap().put(request.getStorageKey(), request);

        authorizedBondedRolesService.initialize().join();

        assertThat(authorizedBondedRolesService.hasAuthorizedPubKey(rootRole, BondedRoleType.ORACLE_NODE)).isFalse();
        assertThat(authorizedBondedRolesService.getBondedRoles()).isEmpty();

        // The key cannot authorize other roles either. The store accepts them, because they do not claim a static key.
        AuthorizedBondedRole mediatorRole = new AuthorizedBondedRole("b".repeat(40),
                "public-key",
                BondedRoleType.MEDIATOR,
                "mediator",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorize(mediatorRole, removedKeyPair),
                removedKeyPair)).isSuccess()).isTrue();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).isEmpty();
    }

    @Test
    void rootRoleOfAStaticKeyIsAuthorized() {
        PublicKey staticKey = KeyGeneration.getPublicKeyFromHex(AuthorizedPubKeys.ORACLE_NODE_PUB_KEYS.iterator().next());
        AuthorizedBondedRole oracleRole = createOracleRole(staticKey, true);

        // We do not have the private key of a static key, so the authorization signature is left out. The network
        // layer checks it.
        assertThat(authorizedBondedRolesService.hasAuthorizedPubKey(new AuthorizedData(oracleRole, staticKey),
                BondedRoleType.ORACLE_NODE)).isTrue();
    }

    private static AuthorizedBondedRole createOracleRole(PublicKey authorizedPublicKey,
                                                         boolean staticPublicKeysProvided) {
        return new AuthorizedBondedRole("a".repeat(40),
                Hex.encode(authorizedPublicKey.getEncoded()),
                BondedRoleType.ORACLE_NODE,
                "oracle",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                staticPublicKeysProvided);
    }

    private static AuthorizedData authorize(AuthorizedBondedRole role,
                                            KeyPair authorizedKeyPair) throws GeneralSecurityException {
        return new AuthorizedData(role,
                Optional.of(SignatureUtil.sign(role.serializeForHash(), authorizedKeyPair.getPrivate())),
                authorizedKeyPair.getPublic());
    }
}

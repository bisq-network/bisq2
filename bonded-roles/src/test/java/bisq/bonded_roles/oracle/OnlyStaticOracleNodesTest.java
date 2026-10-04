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

package bisq.bonded_roles.oracle;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.typesafe.config.ConfigFactory;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The restriction to static oracle node keys is the configuration option
 * application.bondedRoles.onlyStaticOracleNodes. The network storage and the bonded role service apply the configured
 * value (rules 10 to 14 and 16 of docs/specifications/bonded-roles/authorized-data.md).
 * <p>
 * The tests use the real store of the DataService and the constructor of AuthorizedBondedRolesService which the
 * application uses.
 */
class OnlyStaticOracleNodesTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private NetworkService networkService;
    // A key which is no oracle node key
    private KeyPair otherKeyPair;
    private AuthorizedData roleOfOtherKey;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        StorableTypesRegistry.register(AuthorizedBondedRole.class, AuthorizedBondedRole.META_DATA);
        dataService = new DataService(new PersistenceService(tempDir));
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedBondedRole.class.getSimpleName())
                .join();
        networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedBondedRole mediatorRole = new AuthorizedBondedRole("b".repeat(40),
                "public-key",
                BondedRoleType.MEDIATOR,
                "mediator",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
        roleOfOtherKey = new AuthorizedData(mediatorRole,
                Optional.of(SignatureUtil.sign(mediatorRole.serializeForHash(), otherKeyPair.getPrivate())),
                otherKeyPair.getPublic());
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        try {
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
            dataService.shutdown();
        } finally {
            OnlyStaticOracleNodes.setEnabled(true);
        }
    }

    @Test
    void configurationContainsTheRestriction() {
        com.typesafe.config.Config config = ConfigFactory.parseString("""
                marketPrice {}
                blockchainExplorer {}
                mobileNotifications {}
                ignoreSecurityManager = false
                onlyStaticOracleNodes = false
                """);

        assertThat(BondedRolesService.Config.from(config).isOnlyStaticOracleNodes()).isFalse();
    }

    // The default of the code. The configuration files of the applications set it as well.
    @Test
    void restrictionRejectsTheBondedRolesOfOtherKeys() throws GeneralSecurityException {
        assertThat(OnlyStaticOracleNodes.isEnabled()).isTrue();
        AuthorizedBondedRolesService authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false);

        DataStorageResult result = store.add(AddAuthenticatedDataRequest.from(store, roleOfOtherKey, otherKeyPair));

        assertThat(result.isAuthorizedKeyNotStatic()).as(result.toString()).isTrue();
        assertThat(authorizedBondedRolesService.isOracleNodeKeyAllowed(getOtherKeyAsHex())).isFalse();
    }

    // Without the restriction, another key can act as an oracle node with a bonded role of type ORACLE_NODE (rule 3).
    // The network storage then checks only the signature of data without the static flag (rule 4).
    @Test
    void withoutRestrictionTheStoreAcceptsTheBondedRolesOfOtherKeys() throws GeneralSecurityException {
        OnlyStaticOracleNodes.setEnabled(false);
        AuthorizedBondedRolesService authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false);

        DataStorageResult result = store.add(AddAuthenticatedDataRequest.from(store, roleOfOtherKey, otherKeyPair));

        assertThat(result.isSuccess()).as(result.toString()).isTrue();
        assertThat(authorizedBondedRolesService.isOracleNodeKeyAllowed(getOtherKeyAsHex())).isTrue();
    }

    private String getOtherKeyAsHex() {
        return Hex.encode(otherKeyPair.getPublic().getEncoded());
    }
}

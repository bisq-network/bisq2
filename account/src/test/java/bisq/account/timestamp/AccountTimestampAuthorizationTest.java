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

package bisq.account.timestamp;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The network layer checks only the signature of an AuthorizedAccountTimestamp without staticPublicKeysProvided, so
 * any key can publish one. AccountTimestampService applies only account timestamps which an oracle node authorized
 * (see docs/specifications/bonded-roles/authorized-data.md).
 */
class AccountTimestampAuthorizationTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private AccountTimestampService accountTimestampService;
    private KeyPair oracleKeyPair;
    private AuthorizedAccountTimestamp authorizedAccountTimestamp;

    @BeforeEach
    void setUp() {
        dataService = new DataService(new PersistenceService(tempDir));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false);
        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);
        accountTimestampService = new AccountTimestampService(networkService, mock(UserService.class),
                bondedRolesService);

        oracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(new AuthorizedBondedRole("a".repeat(40),
                Hex.encode(oracleKeyPair.getPublic().getEncoded()),
                BondedRoleType.ORACLE_NODE,
                "oracle",
                "signature",
                Optional.empty(),
                new NetworkId(new AddressByTransportTypeMap(Map.of(TransportType.CLEAR,
                        LocalHostAddressTypeFacade.toLocalHostAddress(1234))),
                        new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "identity")),
                Optional.empty(),
                false)));
        byte[] hash = new byte[20];
        Arrays.fill(hash, (byte) 1);
        authorizedAccountTimestamp = new AuthorizedAccountTimestamp(new AccountTimestamp(hash, 1_700_000_000_000L),
                false);
        accountTimestampService.initialize().join();
    }

    @AfterEach
    void tearDown() {
        accountTimestampService.shutdown().join();
        dataService.shutdown();
    }

    @Test
    void accountTimestampOfAnOracleNodeIsApplied() throws GeneralSecurityException {
        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(authorizedAccountTimestamp, oracleKeyPair));

        assertThat(accountTimestampService.getAccountTimestampByHash())
                .containsKey(new ByteArray(authorizedAccountTimestamp.getAccountTimestamp().getHash()));
    }

    @Test
    void accountTimestampOfAnotherKeyIsNotApplied() throws GeneralSecurityException {
        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(authorizedAccountTimestamp,
                KeyGeneration.generateDefaultEcKeyPair()));

        assertThat(accountTimestampService.getAccountTimestampByHash()).isEmpty();
    }

    // A removed copy of another key does not remove the account timestamp of the oracle node
    @Test
    void removalOfAnAccountTimestampOfAnotherKeyKeepsTheAppliedOne() throws GeneralSecurityException {
        authorizedBondedRolesService.onAuthorizedDataAdded(authorize(authorizedAccountTimestamp, oracleKeyPair));

        authorizedBondedRolesService.onAuthorizedDataRemoved(authorize(authorizedAccountTimestamp,
                KeyGeneration.generateDefaultEcKeyPair()));

        assertThat(accountTimestampService.getAccountTimestampByHash())
                .containsKey(new ByteArray(authorizedAccountTimestamp.getAccountTimestamp().getHash()));
    }

    private static AuthorizedData authorize(AuthorizedAccountTimestamp data, KeyPair keyPair)
            throws GeneralSecurityException {
        return new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}

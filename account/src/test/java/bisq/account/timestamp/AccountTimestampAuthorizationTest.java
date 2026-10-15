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

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.DevMode;
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AccountTimestampService applies only account timestamps which an oracle node authorized. While only static oracle
 * nodes are allowed, the network layer accepts an AuthorizedAccountTimestamp only from an oracle node key as well. The
 * service does not rely on that (see rules 6, 9 and 16 of docs/specifications/bonded-roles/authorized-data.md), so most
 * tests notify the service directly. The test with the real store uses the dev keys, which are the oracle node keys in
 * dev mode.
 */
class AccountTimestampAuthorizationTest {
    // The first two of the dev keys of AuthorizedPubKeys.DEV_PUB_KEYS. In dev mode they are the oracle node keys.
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PRIVATE_KEY_2 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104201c6685cef332eecfddb253b601c0ed89e7c822d2d03c53d5a720a860b5dc9419a00706052b8104000aa144034200044b3a5f869a4a4346fb37ca70991ff55f3ef4f0ed3a31b51238aee75858af48fddaeafb11b9c59b3e9f551ee9022a767e5f39dcea2b4a96e1c610a5f1b1a1878e";
    private static final String DEV_PUBLIC_KEY_2 = "3056301006072a8648ce3d020106052b8104000a034200044b3a5f869a4a4346fb37ca70991ff55f3ef4f0ed3a31b51238aee75858af48fddaeafb11b9c59b3e9f551ee9022a767e5f39dcea2b4a96e1c610a5f1b1a1878e";

    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private AccountTimestampService accountTimestampService;
    private KeyPair oracleKeyPair;
    private KeyPair otherOracleKeyPair;
    private AuthorizedAccountTimestamp authorizedAccountTimestamp;

    @BeforeEach
    void setUp() {
        dataService = new DataService(new PersistenceService(tempDir));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        // The static oracle node keys of this version
        DevMode.setDevMode(true);
        oracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_1),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_1));
        otherOracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_2),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_2));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false,
                Set.of(Hex.encode(oracleKeyPair.getPublic().getEncoded()),
                        Hex.encode(otherOracleKeyPair.getPublic().getEncoded())), true);
        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);
        accountTimestampService = new AccountTimestampService(networkService, mock(UserService.class),
                bondedRolesService);

        byte[] hash = new byte[20];
        Arrays.fill(hash, (byte) 1);
        authorizedAccountTimestamp = new AuthorizedAccountTimestamp(new AccountTimestamp(hash, 1_700_000_000_000L),
                false);
        accountTimestampService.initialize().join();
    }

    @AfterEach
    void tearDown() {
        try {
            accountTimestampService.shutdown().join();
            dataService.shutdown();
        } finally {
            DevMode.setDevMode(false);
        }
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

    // Two oracle nodes publish the same account timestamp, each with its own key. The removal of one publication keeps
    // the account timestamp; it is removed with the last publication.
    @Test
    void accountTimestampStaysWhileAnotherOracleNodePublishesIt() throws GeneralSecurityException {
        StorableTypesRegistry.register(AuthorizedAccountTimestamp.class, AuthorizedAccountTimestamp.META_DATA);
        // As NetworkService.addDataServiceListener does
        dataService.addListener(authorizedBondedRolesService);
        AuthenticatedDataStorageService store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedAccountTimestamp.class.getSimpleName())
                .join();
        AuthorizedData publication = authorize(authorizedAccountTimestamp, oracleKeyPair);
        AuthorizedData publicationOfOtherOracle = authorize(authorizedAccountTimestamp, otherOracleKeyPair);
        ByteArray hash = new ByteArray(authorizedAccountTimestamp.getAccountTimestamp().getHash());
        try {
            assertThat(store.add(AddAuthenticatedDataRequest.from(store, publication, oracleKeyPair))
                    .isSuccess()).isTrue();
            assertThat(store.add(AddAuthenticatedDataRequest.from(store, publicationOfOtherOracle, otherOracleKeyPair))
                    .isSuccess()).isTrue();
            assertThat(accountTimestampService.getAccountTimestampByHash()).containsKey(hash);

            assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, publication, oracleKeyPair))
                    .isSuccess()).isTrue();
            assertThat(accountTimestampService.getAccountTimestampByHash()).containsKey(hash);

            assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, publicationOfOtherOracle,
                    otherOracleKeyPair)).isSuccess()).isTrue();
            assertThat(accountTimestampService.getAccountTimestampByHash()).doesNotContainKey(hash);
        } finally {
            // The store writes asynchronously. A write queued now completes after all earlier ones, before JUnit
            // deletes tempDir.
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        }
    }

    private static AuthorizedData authorize(AuthorizedAccountTimestamp data, KeyPair keyPair)
            throws GeneralSecurityException {
        return new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}

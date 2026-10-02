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
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Data can arrive before the bonded role which authorizes it. AuthorizedBondedRolesService keeps such data and
 * processes it again when a bonded role arrives. It must not keep data which was removed in between.
 */
class FailedAuthorizedDataTest {
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    // The key of a node which becomes an oracle node after its data arrived
    private KeyPair laterOracleKeyPair;
    private AuthorizedBondedRole mediatorRole;
    private AuthorizedData mediatorRolePublication;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.empty());
        // Bonded roles of type ORACLE_NODE authorize data, not only the static oracle node keys
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false, Set.of(), false);
        laterOracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        mediatorRole = new AuthorizedBondedRole("b".repeat(40),
                "public-key",
                BondedRoleType.MEDIATOR,
                "mediator",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
        mediatorRolePublication = authorize(mediatorRole, laterOracleKeyPair);
    }

    @Test
    void dataWhichArrivedBeforeItsBondedRoleIsProcessedAgain() {
        authorizedBondedRolesService.onAuthorizedDataAdded(mediatorRolePublication);
        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).isEmpty();

        addOracleNodeRoleOfLaterOracleKey();
        authorizedBondedRolesService.reprocess();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).contains(mediatorRole);
    }

    // The stores no longer hold removed data. A role which is processed again after its removal would stay until
    // shutdown.
    @Test
    void dataWhichWasRemovedIsNotProcessedAgain() {
        authorizedBondedRolesService.onAuthorizedDataAdded(mediatorRolePublication);
        authorizedBondedRolesService.onAuthorizedDataRemoved(mediatorRolePublication);

        addOracleNodeRoleOfLaterOracleKey();
        authorizedBondedRolesService.reprocess();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    // The removal of data without an authorized key must not keep the data either
    @Test
    void dataWhoseRemovalArrivedFirstIsNotProcessed() {
        authorizedBondedRolesService.onAuthorizedDataRemoved(mediatorRolePublication);

        addOracleNodeRoleOfLaterOracleKey();
        authorizedBondedRolesService.reprocess();

        assertThat(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).doesNotContain(mediatorRole);
    }

    private void addOracleNodeRoleOfLaterOracleKey() {
        AuthorizedBondedRole oracleRole = new AuthorizedBondedRole("a".repeat(40),
                Hex.encode(laterOracleKeyPair.getPublic().getEncoded()),
                BondedRoleType.ORACLE_NODE,
                "oracle",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(oracleRole));
    }

    private static AuthorizedData authorize(AuthorizedDistributedData data, KeyPair keyPair)
            throws GeneralSecurityException {
        return new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}

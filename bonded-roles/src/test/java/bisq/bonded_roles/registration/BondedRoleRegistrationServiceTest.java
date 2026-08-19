/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.bonded_roles.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BondedRoleRegistrationServiceTest {
    @Test
    void canonicalizesTransactionIdsBeforeSending() {
        var senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId networkIdTemplate = createNetworkId();
        NetworkId senderNetworkId = new NetworkId(networkIdTemplate.getAddressByTransportTypeMap(),
                new PubKey(senderKeyPair.getPublic(), "sender"));
        NetworkIdWithKeyPair sender = new NetworkIdWithKeyPair(senderNetworkId, senderKeyPair);
        String profileId = Hex.encode(DigestUtil.hash(senderKeyPair.getPublic().getEncoded()));

        AuthorizedOracleNode oracleNode = new AuthorizedOracleNode(createNetworkId(),
                "0".repeat(40),
                "public-key",
                "oracle",
                "signature",
                false);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedOracleNodes())
                .thenReturn(new ObservableSet<>(java.util.Set.of(oracleNode)));
        NetworkService networkService = mock(NetworkService.class);
        BondedRoleRegistrationService service = new BondedRoleRegistrationService(
                networkService, authorizedBondedRolesService);

        service.requestBondedRoleRegistration(profileId,
                "public-key",
                BondedRoleType.MEDIATOR,
                "bond-user",
                "signature",
                BondedRoleRegistrationProtocol.CURRENT_VERSION,
                "A".repeat(64),
                "B".repeat(64),
                Optional.empty(),
                sender,
                false);

        ArgumentCaptor<EnvelopePayloadMessage> messageCaptor = ArgumentCaptor.forClass(EnvelopePayloadMessage.class);
        verify(networkService).confidentialSend(messageCaptor.capture(), any(), any());
        BondedRoleRegistrationRequest request = (BondedRoleRegistrationRequest) messageCaptor.getValue();
        assertThat(request.getProposalTxId()).isEqualTo("a".repeat(64));
        assertThat(request.getLockupTxId()).isEqualTo("b".repeat(64));
    }
}

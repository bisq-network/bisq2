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

package bisq.support.mediation.bisq_easy;

import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BisqEasyMediationCaseTest {
    private UserProfile requester;
    private UserProfile peer;
    private UserProfile mediator;
    private BisqEasyContract contract;

    @BeforeEach
    void setUp() {
        requester = TestUserProfiles.create("requester");
        peer = TestUserProfiles.create("peer");
        mediator = TestUserProfiles.create("mediator");
        contract = TestContracts.contract(requester, peer, mediator);
    }

    @Test
    void loadsCaseStoredWithoutPeersRequest() throws UnresolvableProtobufException {
        BisqEasyMediationRequest request = createRequest(requester, peer, "hi");
        // As written before the other trader's request was retained: no field 5.
        bisq.support.protobuf.MediationCase storedCase = bisq.support.protobuf.MediationCase.newBuilder()
                .setMediationRequest(request.toValueProto(false))
                .setRequestDate(1_700_000_700_000L)
                .setIsClosed(true)
                .setCloseCaseDate(1_700_000_800_000L)
                .build();

        BisqEasyMediationCase loadedCase = BisqEasyMediationCase.fromProto(storedCase);

        assertEquals(List.of(request), loadedCase.getMediationRequests());
        assertEquals(request.getChatMessages(), loadedCase.getBisqEasyMediationRequest().getChatMessages());
        assertTrue(loadedCase.isClosed());
        assertEquals(Optional.of(1_700_000_800_000L), loadedCase.getCloseCaseDate());
        assertTrue(loadedCase.setPeersMediationRequest(createRequest(peer, requester, "hello")));
    }

    @Test
    void roundTripKeepsPeersRequest() throws UnresolvableProtobufException {
        BisqEasyMediationRequest requestersRequest = createRequest(requester, peer, "hi");
        BisqEasyMediationRequest peersRequest = createRequest(peer, requester, "hello");
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(requestersRequest);
        mediationCase.setPeersMediationRequest(peersRequest);

        BisqEasyMediationCase loadedCase = BisqEasyMediationCase.fromProto(mediationCase.toProto(false));

        assertEquals(mediationCase, loadedCase);
        assertEquals(List.of(requestersRequest, peersRequest), loadedCase.getMediationRequests());
        assertEquals(peersRequest.getChatMessages(), loadedCase.getMediationRequests().get(1).getChatMessages());
    }

    private BisqEasyMediationRequest createRequest(UserProfile requester, UserProfile peer, String text) {
        BisqEasyOpenTradeMessage message = new BisqEasyOpenTradeMessage("trade-1",
                "message-of-" + requester.getNickName(),
                BisqEasyOpenTradeChannel.createId("trade-1"),
                requester,
                peer.getId(),
                peer.getNetworkId(),
                text,
                Optional.empty(),
                1_700_000_000_000L,
                false,
                Optional.of(mediator),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
        return new BisqEasyMediationRequest("trade-1",
                TestContracts.protoCopy(contract),
                requester,
                peer,
                new ArrayList<>(List.of(message)),
                mediator.getNetworkId());
    }
}

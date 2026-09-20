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

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyMediatorServiceRequestChecksTest {
    private UserProfile requester;
    private UserProfile peer;
    private UserProfile mediator;
    private BisqEasyContract contract;
    private List<BisqEasyMediationCase> cases;

    @BeforeEach
    void setUp() {
        requester = TestUserProfiles.create("requester");
        peer = TestUserProfiles.create("peer");
        mediator = TestUserProfiles.create("mediator");
        contract = mock(BisqEasyContract.class);
        cases = new ArrayList<>();
    }

    @Test
    void acceptsFirstRequestForTrade() {
        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, requester, peer), cases, false));
    }

    @Test
    void rejectsRepeatedRequestFromSameRequesterWhileChannelExists() {
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer)));

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, requester, peer), cases, true));
        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", mock(BisqEasyContract.class), requester, peer), cases, true));
    }

    @Test
    void acceptsRepeatedRequestAfterMediatorLeftChannel() {
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer)));

        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, requester, peer), cases, false));
    }

    @Test
    void acceptsPeersRequestForKnownTradeRegardlessOfOpenCaseCount() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("other-" + i, mock(BisqEasyContract.class), peer, requester))));
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer)));

        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases, true));
        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", mock(BisqEasyContract.class), peer, requester), cases, true));
    }

    @Test
    void rejectsRequesterAtOpenCaseLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("trade-" + i, mock(BisqEasyContract.class), requester, peer))));

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, requester, peer), cases, false));
        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, peer, requester), cases, false));
    }

    @Test
    void closedCasesDoNotCountTowardsLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("trade-" + i, mock(BisqEasyContract.class), requester, peer))));
        cases.get(0).setClosed(true);

        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, requester, peer), cases, false));
    }

    @Test
    void channelMustMatchOfferAndTraders() {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(contract.getOffer()).thenReturn(offer);
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class);
        when(channel.getBisqEasyOffer()).thenReturn(offer);
        when(channel.getTraders()).thenReturn(Set.of(requester, peer));

        assertTrue(BisqEasyMediatorService.hasMatchingChannel(channel, contract, requester, peer));
        assertTrue(BisqEasyMediatorService.hasMatchingChannel(channel, contract, peer, requester));
        assertFalse(BisqEasyMediatorService.hasMatchingChannel(channel, contract, requester, TestUserProfiles.create("outsider")));

        when(channel.getBisqEasyOffer()).thenReturn(mock(BisqEasyOffer.class));
        assertFalse(BisqEasyMediatorService.hasMatchingChannel(channel, contract, requester, peer));
    }

    @Test
    void firstRequestImportsWholeTranscript() {
        BisqEasyOpenTradeMessage requesterMessage = createMessage("m1", requester);
        BisqEasyOpenTradeMessage peerMessage = createMessage("m2", peer);
        BisqEasyMediationRequest request = createRequest("trade-1", contract, requester, peer, List.of(requesterMessage, peerMessage));

        assertEquals(Set.of(requesterMessage, peerMessage), Set.copyOf(BisqEasyMediatorService.selectChatMessages(request, false)));
    }

    @Test
    void laterRequestImportsOnlyRequestersOwnMessages() {
        BisqEasyOpenTradeMessage peersOwnMessage = createMessage("m3", peer);
        BisqEasyOpenTradeMessage claimedRequesterMessage = createMessage("m4", requester);
        BisqEasyMediationRequest peersRequest = createRequest("trade-1", contract, peer, requester, List.of(peersOwnMessage, claimedRequesterMessage));

        assertEquals(List.of(peersOwnMessage), BisqEasyMediatorService.selectChatMessages(peersRequest, true));
    }

    @Test
    void peersForwardedMessagesAreUnverified() {
        BisqEasyOpenTradeMessage requesterMessage = createMessage("m1", requester);
        BisqEasyOpenTradeMessage peerMessage = createMessage("m2", peer);
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer,
                List.of(requesterMessage, peerMessage))));

        assertEquals(Set.of("m2"), BisqEasyMediatorService.getUnverifiedChatMessageIds("trade-1", cases));
        assertEquals(Set.of(), BisqEasyMediatorService.getUnverifiedChatMessageIds("trade-2", cases));
    }

    @Test
    void messagesForwardedByTheirAuthorAreVerified() {
        BisqEasyOpenTradeMessage requesterMessage = createMessage("m1", requester);
        BisqEasyOpenTradeMessage peerMessage = createMessage("m2", peer);
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer,
                List.of(requesterMessage, peerMessage))));
        // Peer's request with a different contract is stored as second case and carries copies of both messages
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", mock(BisqEasyContract.class), peer, requester,
                List.of(createMessage("m1", requester), createMessage("m2", peer)))));

        assertEquals(Set.of(), BisqEasyMediatorService.getUnverifiedChatMessageIds("trade-1", cases));
    }

    private BisqEasyMediationRequest createRequest(String tradeId,
                                                   BisqEasyContract contract,
                                                   UserProfile requester,
                                                   UserProfile peer) {
        return createRequest(tradeId, contract, requester, peer, List.of());
    }

    private BisqEasyMediationRequest createRequest(String tradeId,
                                                   BisqEasyContract contract,
                                                   UserProfile requester,
                                                   UserProfile peer,
                                                   List<BisqEasyOpenTradeMessage> chatMessages) {
        return new BisqEasyMediationRequest(tradeId, contract, requester, peer, new ArrayList<>(chatMessages), mediator.getNetworkId());
    }

    private BisqEasyOpenTradeMessage createMessage(String id, UserProfile sender) {
        BisqEasyOpenTradeMessage message = mock(BisqEasyOpenTradeMessage.class);
        when(message.getId()).thenReturn(id);
        when(message.getSenderUserProfile()).thenReturn(sender);
        when(message.getTextOrNA()).thenReturn("text");
        return message;
    }
}

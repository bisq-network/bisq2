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
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
        BisqEasyOpenTradeMessage requesterMessage = createMessage("m1", requester, "hi");
        BisqEasyOpenTradeMessage peerMessage = createMessage("m2", peer, "hello");
        BisqEasyMediationRequest request = createRequest("trade-1", contract, requester, peer, List.of(requesterMessage, peerMessage));

        assertEquals(Set.of(requesterMessage, peerMessage), Set.copyOf(BisqEasyMediatorService.selectChatMessages(request, false)));
    }

    @Test
    void laterRequestImportsOnlyRequestersOwnMessages() {
        BisqEasyOpenTradeMessage peersOwnMessage = createMessage("m3", peer, "hello");
        BisqEasyOpenTradeMessage claimedRequesterMessage = createMessage("m4", requester, "hi");
        BisqEasyMediationRequest peersRequest = createRequest("trade-1", contract, peer, requester, List.of(peersOwnMessage, claimedRequesterMessage));

        assertEquals(List.of(peersOwnMessage), BisqEasyMediatorService.selectChatMessages(peersRequest, true));
    }

    @Test
    void peersForwardedMessagesAreUnverified() {
        BisqEasyOpenTradeMessage requesterMessage = createMessage("m1", requester, "hi");
        BisqEasyOpenTradeMessage peerMessage = createMessage("m2", peer, "hello");
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer,
                List.of(requesterMessage, peerMessage))));

        assertEquals(Set.of(BisqEasyMediatorService.getProvenanceKey(peerMessage)),
                BisqEasyMediatorService.getUnverifiedChatMessageKeys("trade-1", cases));
        assertEquals(Set.of(), BisqEasyMediatorService.getUnverifiedChatMessageKeys("trade-2", cases));
    }

    @Test
    void messagesForwardedByTheirAuthorAreVerified() {
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "hello")))));
        // Peer's request with a different contract is stored as second case and carries copies of both messages
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", mock(BisqEasyContract.class), peer, requester,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "hello")))));

        assertEquals(Set.of(), BisqEasyMediatorService.getUnverifiedChatMessageKeys("trade-1", cases));
    }

    @Test
    void sharedMessageIdDoesNotClearTheMark() {
        BisqEasyOpenTradeMessage forgedPeerMessage = createMessage("m1", peer, "I never paid");
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), forgedPeerMessage))));

        assertEquals(Set.of(BisqEasyMediatorService.getProvenanceKey(forgedPeerMessage)),
                BisqEasyMediatorService.getUnverifiedChatMessageKeys("trade-1", cases));
    }

    @Test
    void alteredCopyOfPeersMessageStaysUnverified() {
        BisqEasyOpenTradeMessage alteredCopy = createMessage("m2", peer, "I never paid");
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer, List.of(alteredCopy))));
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", mock(BisqEasyContract.class), peer, requester,
                List.of(createMessage("m2", peer, "I paid")))));

        assertEquals(Set.of(BisqEasyMediatorService.getProvenanceKey(alteredCopy)),
                BisqEasyMediatorService.getUnverifiedChatMessageKeys("trade-1", cases));
    }

    @Test
    void provenanceKeyIgnoresReactions() {
        BisqEasyOpenTradeMessage message = createMessage("m1", peer, "hi");
        BisqEasyOpenTradeMessage withReaction = createMessage("m1", peer, "hi");
        withReaction.getChatMessageReactions().add(mock(BisqEasyOpenTradeMessageReaction.class));

        assertEquals(BisqEasyMediatorService.getProvenanceKey(message), BisqEasyMediatorService.getProvenanceKey(withReaction));
    }

    @Test
    void rejectsRequestWithSharedMessageIds() {
        when(contract.getMaker()).thenReturn(new Party(Role.MAKER, requester.getNetworkId()));
        when(contract.getTaker()).thenReturn(new Party(Role.TAKER, peer.getNetworkId()));
        when(contract.getMediator()).thenReturn(Optional.of(mediator));
        BannedUserService bannedUserService = mock(BannedUserService.class);
        BisqEasyMediationRequest accepted = createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "I never paid")));
        BisqEasyMediationRequest rejected = createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), createMessage("m1", peer, "I never paid")));

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(accepted, bannedUserService, cases, Optional.empty()).isPresent());
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(rejected, bannedUserService, cases, Optional.empty()).isEmpty());
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

    private BisqEasyOpenTradeMessage createMessage(String id, UserProfile sender, String text) {
        return new BisqEasyOpenTradeMessage("trade-1",
                id,
                BisqEasyOpenTradeChannel.createId("trade-1"),
                sender,
                mediator.getId(),
                mediator.getNetworkId(),
                text,
                Optional.empty(),
                1L,
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }
}

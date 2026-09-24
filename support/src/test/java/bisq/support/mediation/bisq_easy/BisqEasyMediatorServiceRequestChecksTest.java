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

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyMediatorServiceRequestChecksTest {
    private UserProfile requester;
    private UserProfile peer;
    private UserIdentity mediatorIdentity;
    private UserProfile mediator;
    private BisqEasyContract contract;
    private List<BisqEasyMediationCase> cases;

    @BeforeEach
    void setUp() {
        requester = TestUserProfiles.create("requester");
        peer = TestUserProfiles.create("peer");
        mediatorIdentity = TestUserProfiles.createUserIdentity("mediator");
        mediator = mediatorIdentity.getUserProfile();
        contract = mock(BisqEasyContract.class);
        cases = new ArrayList<>();
    }

    @Test
    void acceptsFirstRequestForTrade() {
        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, requester, peer), cases));
    }

    @Test
    void rejectsRepeatedRequestFromSameRequester() {
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer)));

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, requester, peer), cases));
        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", mock(BisqEasyContract.class), requester, peer), cases));
    }

    @Test
    void rejectsRepeatedRequestAfterMediatorLeftChannel() {
        // Leaving removes the mediator's channel. The case keeps the requester on record.
        BisqEasyContract realContract = TestContracts.contract(requester, peer, mediator);
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", realContract, requester, peer)));
        BannedUserService bannedUserService = mock(BannedUserService.class);

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(createRequest("trade-1", TestContracts.protoCopy(realContract), requester, peer),
                bannedUserService, cases, Optional.empty()).isEmpty());
        BisqEasyContract otherContract = TestContracts.contract(requester, peer, mediator, List.of("en", "de"));
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(createRequest("trade-1", otherContract, requester, peer),
                bannedUserService, cases, Optional.empty()).isEmpty());
    }

    @Test
    void rejectsRequestNamingOtherTradersForKnownTrade() {
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", TestContracts.contract(requester, peer, mediator), requester, peer)));
        BannedUserService bannedUserService = mock(BannedUserService.class);
        UserProfile outsider = TestUserProfiles.create("outsider");
        UserProfile otherOutsider = TestUserProfiles.create("otherOutsider");
        BisqEasyMediationRequest outsidersRequest = createRequest("trade-1",
                TestContracts.contract(outsider, otherOutsider, mediator), outsider, otherOutsider);
        // The other trader of the trade names another profile as its counterparty.
        BisqEasyMediationRequest peerNamingOutsider = createRequest("trade-1",
                TestContracts.contract(peer, outsider, mediator), peer, outsider);

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(outsidersRequest, bannedUserService, cases, Optional.empty()).isEmpty());
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(peerNamingOutsider, bannedUserService, cases, Optional.empty()).isEmpty());
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(outsidersRequest, bannedUserService, List.of(), Optional.empty()).isPresent());
    }

    @Test
    void peersRequestForKnownTradeCountsAgainstItsOwnLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER - 1)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("other-" + i, mock(BisqEasyContract.class), peer, requester))));
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer)));

        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases));

        cases.add(new BisqEasyMediationCase(createRequest("other-last", mock(BisqEasyContract.class), peer, requester)));
        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases));
    }

    @Test
    void casesWhereRequesterIsTheOtherTraderCountTowardsLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> {
                    BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest("trade-" + i, mock(BisqEasyContract.class), peer, requester));
                    mediationCase.setPeersMediationRequest(createRequest("trade-" + i, mediationCase.getBisqEasyMediationRequest().getContract(), requester, peer));
                    cases.add(mediationCase);
                });

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, requester, peer), cases));
    }

    @Test
    void rejectsRequesterAtOpenCaseLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("trade-" + i, mock(BisqEasyContract.class), requester, peer))));

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, requester, peer), cases));
        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, peer, requester), cases));
    }

    @Test
    void closedCasesDoNotCountTowardsLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("trade-" + i, mock(BisqEasyContract.class), requester, peer))));
        cases.get(0).setClosed(true);

        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, requester, peer), cases));
    }

    @Test
    void channelMustMatchTraders() {
        BisqEasyOpenTradeChannel channel = createMediatorChannel("trade-1",
                TestContracts.contract(requester, peer, mediator), requester, peer);

        assertTrue(BisqEasyMediatorService.hasMatchingTraders(channel, requester, peer));
        assertTrue(BisqEasyMediatorService.hasMatchingTraders(channel, peer, requester));
        assertFalse(BisqEasyMediatorService.hasMatchingTraders(channel, requester, TestUserProfiles.create("outsider")));
        assertFalse(BisqEasyMediatorService.hasMatchingTraders(channel, requester, requester));
    }

    @Test
    void rejectsRepeatedRequestFromPeerStoredOnCase() {
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer));
        assertFalse(mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, requester, peer)));
        assertTrue(mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, peer, requester)));
        assertFalse(mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, peer, requester)));
        cases.add(mediationCase);

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases));
        assertEquals(2, BisqEasyMediatorService.getRequestsForTrade("trade-1", cases).size());
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
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "hello"))));
        // Peer's request for the same contract is kept on the case and carries copies of both messages
        mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, peer, requester,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "hello"))));
        cases.add(mediationCase);

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
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer, List.of(alteredCopy)));
        mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, peer, requester,
                List.of(createMessage("m2", peer, "I paid"))));
        cases.add(mediationCase);

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
    void forwardedReactionsAreNotImported() {
        // The requester's own message carries a reaction attributed to the other trader.
        BisqEasyOpenTradeMessage requestersMessage = createMessage("m1", requester, "I paid");
        requestersMessage.getChatMessageReactions().add(createReaction("m1", peer));
        BisqEasyMediationRequest request = createRequest("trade-1", contract, requester, peer, List.of(requestersMessage));

        List<BisqEasyOpenTradeMessage> messagesToImport = BisqEasyMediatorService.getMessagesToImport(request, Set.of());

        assertEquals(1, messagesToImport.size());
        assertTrue(messagesToImport.get(0).getChatMessageReactions().isEmpty());
        assertEquals(BisqEasyMediatorService.getProvenanceKey(requestersMessage),
                BisqEasyMediatorService.getProvenanceKey(messagesToImport.get(0)));
    }

    @Test
    void messageAlreadyInChannelIsNotImported() {
        // The channel's copy carries a reaction the mediator added, which does not change the message.
        BisqEasyOpenTradeMessage channelCopy = createMessage("m1", requester, "hi");
        channelCopy.getChatMessageReactions().add(createReaction("m1", mediator));
        BisqEasyOpenTradeMessage newMessage = createMessage("m2", peer, "hello");
        BisqEasyMediationRequest request = createRequest("trade-1", contract, peer, requester,
                List.of(createMessage("m1", requester, "hi"), newMessage));
        BisqEasyOpenTradeMessage alteredCopy = createMessage("m1", requester, "I never paid");
        BisqEasyMediationRequest requestWithAlteredCopy = createRequest("trade-1", contract, peer, requester, List.of(alteredCopy));

        assertEquals(List.of(newMessage), BisqEasyMediatorService.getMessagesToImport(request, Set.of(channelCopy)));
        assertEquals(List.of(alteredCopy), BisqEasyMediatorService.getMessagesToImport(requestWithAlteredCopy, Set.of(channelCopy)));
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

    @Test
    void rejectsTranscriptWithLocalNoticeTypes() {
        setUpContractParties();
        BannedUserService bannedUserService = mock(BannedUserService.class);
        BisqEasyMediationRequest withLeave = createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "left", ChatMessageType.LEAVE)));
        BisqEasyMediationRequest withChatRulesWarning = createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", requester, "hi"), createMessage("m2", peer, "rules", ChatMessageType.CHAT_RULES_WARNING)));
        BisqEasyMediationRequest withExpiredIndicator = createRequest("trade-1", contract, requester, peer,
                List.of(createMessage("m1", peer, "expired", ChatMessageType.EXPIRED_MESSAGES_INDICATOR)));

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(withLeave, bannedUserService, cases, Optional.empty()).isPresent());
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(withChatRulesWarning, bannedUserService, cases, Optional.empty()).isEmpty());
        assertTrue(BisqEasyMediatorService.verifyMediationRequest(withExpiredIndicator, bannedUserService, cases, Optional.empty()).isEmpty());
    }

    @Test
    void rejectsRequestNamingTheMediatorAsTrader() {
        // Forged messages from the mediator's own profile would render as the mediator's own words.
        when(contract.getMaker()).thenReturn(new Party(Role.MAKER, requester.getNetworkId()));
        when(contract.getTaker()).thenReturn(new Party(Role.TAKER, mediator.getNetworkId()));
        when(contract.getMediator()).thenReturn(Optional.of(mediator));
        BannedUserService bannedUserService = mock(BannedUserService.class);
        BisqEasyMediationRequest request = createRequest("trade-1", contract, requester, mediator,
                List.of(createMessage("m1", mediator, "I confirm the payment arrived")));

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(request, bannedUserService, cases, Optional.empty()).isEmpty());
    }

    @Test
    void requestsOfBothTradersAreDistinctMessagesForOneCase() {
        BisqEasyContract realContract = TestContracts.contract(requester, peer, mediator);
        BisqEasyMediationRequest requestersRequest = createRequest("trade-1", TestContracts.protoCopy(realContract), requester, peer);
        BisqEasyMediationRequest peersRequest = createRequest("trade-1", TestContracts.protoCopy(realContract), peer, requester);

        assertNotEquals(requestersRequest, peersRequest);
        assertEquals(new BisqEasyMediationCase(requestersRequest), new BisqEasyMediationCase(peersRequest));
        assertEquals(new BisqEasyMediationCase(requestersRequest).hashCode(), new BisqEasyMediationCase(peersRequest).hashCode());
        BisqEasyContract otherOffersContract = TestContracts.contract(requester, peer, mediator, List.of("en", "de"));
        assertNotEquals(new BisqEasyMediationCase(requestersRequest),
                new BisqEasyMediationCase(createRequest("trade-1", otherOffersContract, peer, requester)));
        assertEquals(requestersRequest, createRequest("trade-1", TestContracts.protoCopy(realContract), requester, peer,
                List.of(createMessage("m1", peer, "resent"))));
    }

    @Test
    void acceptsOtherTradersCopyOfTheContractIntoTheSameCase() {
        BisqEasyContract realContract = TestContracts.contract(requester, peer, mediator);
        BisqEasyMediationRequest requestersRequest = createRequest("trade-1", TestContracts.protoCopy(realContract), requester, peer);
        cases.add(new BisqEasyMediationCase(requestersRequest));
        BisqEasyOpenTradeChannel channel = createMediatorChannel("trade-1", requestersRequest.getContract(), requester, peer);
        BisqEasyMediationRequest peersRequest = createRequest("trade-1", TestContracts.protoCopy(realContract), peer, requester);

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(peersRequest, mock(BannedUserService.class), cases, Optional.of(channel)).isPresent());
        assertEquals(cases.get(0), new BisqEasyMediationCase(peersRequest));
    }

    @Test
    void acceptsOtherTradersRequestWithDifferentOfferAsSecondCase() {
        // The first accepted request set the channel's offer. The other trader's contract differs in one offer field.
        BisqEasyContract firstContract = TestContracts.contract(requester, peer, mediator);
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", firstContract, requester, peer)));
        BisqEasyOpenTradeChannel channel = createMediatorChannel("trade-1", firstContract, requester, peer);
        BisqEasyContract otherContract = TestContracts.contract(requester, peer, mediator, List.of("en", "de"));
        BisqEasyMediationRequest peersRequest = createRequest("trade-1", otherContract, peer, requester);

        assertTrue(BisqEasyMediatorService.verifyMediationRequest(peersRequest, mock(BannedUserService.class), cases, Optional.of(channel)).isPresent());
        assertNotEquals(cases.get(0), new BisqEasyMediationCase(peersRequest));
    }

    private void setUpContractParties() {
        when(contract.getMaker()).thenReturn(new Party(Role.MAKER, requester.getNetworkId()));
        when(contract.getTaker()).thenReturn(new Party(Role.TAKER, peer.getNetworkId()));
        when(contract.getMediator()).thenReturn(Optional.of(mediator));
    }

    private BisqEasyOpenTradeChannel createMediatorChannel(String tradeId,
                                                           BisqEasyContract contract,
                                                           UserProfile requester,
                                                           UserProfile peer) {
        return BisqEasyOpenTradeChannel.create(tradeId,
                contract.getOffer(),
                mediatorIdentity,
                Set.of(requester, peer),
                Optional.of(mediatorIdentity.getUserProfile()),
                true);
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

    private BisqEasyOpenTradeMessageReaction createReaction(String chatMessageId, UserProfile sender) {
        return new BisqEasyOpenTradeMessageReaction("reaction-" + chatMessageId,
                sender,
                mediator.getId(),
                mediator.getNetworkId(),
                BisqEasyOpenTradeChannel.createId("trade-1"),
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                chatMessageId,
                0,
                1_700_000_000_000L,
                false);
    }

    private BisqEasyOpenTradeMessage createMessage(String id, UserProfile sender, String text) {
        return createMessage(id, sender, text, ChatMessageType.TEXT);
    }

    private BisqEasyOpenTradeMessage createMessage(String id, UserProfile sender, String text, ChatMessageType type) {
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
                type,
                Optional.empty(),
                new HashSet<>());
    }
}

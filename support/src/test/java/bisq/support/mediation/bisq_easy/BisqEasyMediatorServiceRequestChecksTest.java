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
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
    void peersRequestForKnownTradeCountsAgainstItsOwnLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER - 1)
                .forEach(i -> cases.add(new BisqEasyMediationCase(createRequest("other-" + i, mock(BisqEasyContract.class), peer, requester))));
        cases.add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer)));

        assertTrue(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases, true));

        cases.add(new BisqEasyMediationCase(createRequest("other-last", mock(BisqEasyContract.class), peer, requester)));
        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases, true));
    }

    @Test
    void casesWhereRequesterIsTheOtherTraderCountTowardsLimit() {
        IntStream.range(0, BisqEasyMediatorService.MAX_OPEN_CASES_PER_REQUESTER)
                .forEach(i -> {
                    BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest("trade-" + i, mock(BisqEasyContract.class), peer, requester));
                    mediationCase.setPeersMediationRequest(createRequest("trade-" + i, mediationCase.getBisqEasyMediationRequest().getContract(), requester, peer));
                    cases.add(mediationCase);
                });

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-new", contract, requester, peer), cases, false));
    }

    @Test
    void repeatedRequestReplacesTheRetainedRequestOfThatTrader() {
        BisqEasyMediationRequest first = createRequest("trade-1", contract, requester, peer, List.of(createMessage("m1", peer, "v1")));
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(first);
        BisqEasyMediationRequest peersRequest = createRequest("trade-1", contract, peer, requester, List.of(createMessage("m1", peer, "v1")));
        assertTrue(mediationCase.setPeersMediationRequest(peersRequest));

        BisqEasyMediationRequest repeated = createRequest("trade-1", contract, requester, peer, List.of(createMessage("m2", peer, "forged")));
        BisqEasyMediationCase updated = mediationCase.withRequest(repeated);
        assertEquals(List.of(repeated, peersRequest), updated.getMediationRequests());
        assertEquals(mediationCase, updated);

        BisqEasyMediationRequest peersRepeated = createRequest("trade-1", contract, peer, requester, List.of(createMessage("m3", peer, "v2")));
        assertEquals(List.of(repeated, peersRepeated), updated.withRequest(peersRepeated).getMediationRequests());

        UserProfile outsider = TestUserProfiles.create("outsider");
        assertEquals(updated, updated.withRequest(createRequest("trade-1", contract, outsider, peer)));
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
    void rejectsRepeatedRequestFromPeerStoredOnCase() {
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer));
        assertFalse(mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, requester, peer)));
        assertTrue(mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, peer, requester)));
        assertFalse(mediationCase.setPeersMediationRequest(createRequest("trade-1", contract, peer, requester)));
        cases.add(mediationCase);

        assertFalse(BisqEasyMediatorService.isWithinCaseLimits(createRequest("trade-1", contract, peer, requester), cases, true));
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
    void channelMatchesTheOtherTradersCopyOfTheContract() {
        BisqEasyContract realContract = TestContracts.contract(requester, peer, mediator);
        BisqEasyOpenTradeChannel channel = createMediatorChannel("trade-1", TestContracts.protoCopy(realContract), requester, peer);

        assertTrue(BisqEasyMediatorService.hasMatchingChannel(channel, TestContracts.protoCopy(realContract), peer, requester));
    }

    @Test
    void unverifiedKeysFollowRequestsArrivingWhileTheChannelIsOpen() {
        PersistenceService persistenceService = mock(PersistenceService.class, RETURNS_DEEP_STUBS);
        BisqEasyMediatorService service = new BisqEasyMediatorService(persistenceService,
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS));
        BisqEasyOpenTradeMessage peersMessage = createMessage("m1", peer, "I never paid");
        service.getMediationCases().add(new BisqEasyMediationCase(createRequest("trade-1", contract, requester, peer, List.of(peersMessage))));

        String key = BisqEasyMediatorService.getProvenanceKey(peersMessage);
        assertEquals(Set.of(key), service.getUnverifiedChatMessageKeys("trade-1"));

        // The author's own request arrives while the mediator has the channel open.
        service.getMediationCases().add(new BisqEasyMediationCase(createRequest("trade-1",
                mock(BisqEasyContract.class), peer, requester, List.of(peersMessage))));

        assertTrue(service.getUnverifiedChatMessageKeys("trade-1").isEmpty());
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

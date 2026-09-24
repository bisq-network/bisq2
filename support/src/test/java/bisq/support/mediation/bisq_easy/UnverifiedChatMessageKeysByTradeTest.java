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
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnverifiedChatMessageKeysByTradeTest {
    private UserProfile requester;
    private UserProfile peer;
    private UserProfile mediator;
    private BisqEasyContract contract;
    private ObservableSet<BisqEasyMediationCase> mediationCases;
    private UnverifiedChatMessageKeysByTrade unverifiedChatMessageKeysByTrade;
    private BisqEasyOpenTradeMessage peersMessage;
    private String peersMessageKey;

    @BeforeEach
    void setUp() {
        requester = TestUserProfiles.create("requester");
        peer = TestUserProfiles.create("peer");
        mediator = TestUserProfiles.create("mediator");
        contract = TestContracts.contract(requester, peer, mediator);
        mediationCases = new ObservableSet<>();
        unverifiedChatMessageKeysByTrade = new UnverifiedChatMessageKeysByTrade(mediationCases);
        peersMessage = createMessage("m1", peer, "I never paid");
        peersMessageKey = BisqEasyMediatorService.getProvenanceKey(peersMessage);
    }

    @Test
    void keysFollowPeersRequestSetInPlace() {
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest(requester, peer, List.of(peersMessage)));
        mediationCases.add(mediationCase);
        Set<String> keys = unverifiedChatMessageKeysByTrade.get("trade-1");
        assertEquals(Set.of(peersMessageKey), keys);
        assertSame(keys, unverifiedChatMessageKeysByTrade.get("trade-1"));

        // The author's own request joins the case already in the set.
        mediationCase.setPeersMediationRequest(createRequest(peer, requester, List.of(createMessage("m1", peer, "I never paid"))));

        assertTrue(unverifiedChatMessageKeysByTrade.get("trade-1").isEmpty());
    }

    @Test
    void keysFollowAddedAndRemovedCase() {
        mediationCases.add(new BisqEasyMediationCase(createRequest(requester, peer, List.of(peersMessage))));
        assertEquals(Set.of(peersMessageKey), unverifiedChatMessageKeysByTrade.get("trade-1"));

        // The author's request with a differing contract opens a second case of the trade.
        BisqEasyContract otherContract = TestContracts.contract(requester, peer, mediator, List.of("en", "de"));
        BisqEasyMediationCase secondCase = new BisqEasyMediationCase(new BisqEasyMediationRequest("trade-1",
                otherContract,
                peer,
                requester,
                new ArrayList<>(List.of(createMessage("m1", peer, "I never paid"))),
                mediator.getNetworkId()));
        mediationCases.add(secondCase);
        assertTrue(unverifiedChatMessageKeysByTrade.get("trade-1").isEmpty());

        mediationCases.remove(secondCase);
        assertEquals(Set.of(peersMessageKey), unverifiedChatMessageKeysByTrade.get("trade-1"));
    }

    @Test
    void keysFollowCaseRequestedAgainWithOtherMessages() {
        BisqEasyMediationCase mediationCase = new BisqEasyMediationCase(createRequest(requester, peer, List.of(peersMessage)));
        mediationCases.add(mediationCase);
        assertEquals(Set.of(peersMessageKey), unverifiedChatMessageKeysByTrade.get("trade-1"));

        // After the mediator removed the case, an equal request with another transcript opens it again.
        mediationCases.remove(mediationCase);
        BisqEasyOpenTradeMessage otherPeersMessage = createMessage("m2", peer, "I paid");
        mediationCases.add(new BisqEasyMediationCase(createRequest(requester, peer, List.of(otherPeersMessage))));

        assertEquals(Set.of(BisqEasyMediatorService.getProvenanceKey(otherPeersMessage)), unverifiedChatMessageKeysByTrade.get("trade-1"));
    }

    private BisqEasyMediationRequest createRequest(UserProfile requester,
                                                   UserProfile peer,
                                                   List<BisqEasyOpenTradeMessage> chatMessages) {
        return new BisqEasyMediationRequest("trade-1",
                TestContracts.protoCopy(contract),
                requester,
                peer,
                new ArrayList<>(chatMessages),
                mediator.getNetworkId());
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

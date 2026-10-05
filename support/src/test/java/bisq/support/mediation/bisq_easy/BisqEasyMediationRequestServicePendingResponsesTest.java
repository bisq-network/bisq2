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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A trader's node keeps a mediator response whose trade channel does not exist yet, and processes it again when a
 * trade channel is added. Anybody can send such responses, with any trade ID, but the node keeps only those of a
 * mediator. See docs/specifications/trade/pending-trade-messages.md.
 */
class BisqEasyMediationRequestServicePendingResponsesTest {
    private static final String TRADE_ID = "trade-id";

    private BisqEasyOpenTradeChannelService channelService;
    private ObservableSet<BisqEasyOpenTradeChannel> channels;
    private BisqEasyMediationRequestService service;
    private NetworkId mediator;
    private NetworkId otherMediator;

    @BeforeEach
    void setUp() {
        channelService = mock(BisqEasyOpenTradeChannelService.class);
        channels = new ObservableSet<>();
        when(channelService.getChannels()).thenReturn(channels);
        when(channelService.findChannelByTradeId(anyString())).thenAnswer(invocation -> channels.stream()
                .filter(channel -> channel.getTradeId().equals(invocation.getArgument(0)))
                .findAny());
        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(channelService);
        UserService userService = mock(UserService.class);
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        mediator = networkId("mediator");
        otherMediator = networkId("other-mediator");
        AuthorizedBondedRole mediatorRole = mediatorRole(mediator);
        AuthorizedBondedRole otherMediatorRole = mediatorRole(otherMediator);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream())
                .thenAnswer(invocation -> Stream.of(mediatorRole, otherMediatorRole));
        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);
        service = new BisqEasyMediationRequestService(mock(NetworkService.class),
                chatService,
                userService,
                bondedRolesService);
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    // Another mediator knows the trade ID and responds first. Both responses arrive before the trade channel exists.
    @Test
    void mediatorsResponseIsAppliedAlthoughAnotherSenderRespondedEarlier() throws InterruptedException {
        service.onMessage(new BisqEasyMediatorsResponse(TRADE_ID, otherMediator));
        service.onMessage(new BisqEasyMediatorsResponse(TRADE_ID, mediator));

        BisqEasyOpenTradeChannel channel = channelWithMediator(TRADE_ID, mediator);
        channels.add(channel);
        awaitNoPendingResponses();

        // Only the mediator's response is applied
        verify(channelService, times(1)).setIsInMediation(channel, true);
        verify(channelService, times(1)).addMediatorsResponseMessage(eq(channel), anyString());
    }

    @Test
    void numberOfPendingResponsesIsBounded() {
        for (int i = 0; i < 120; i++) {
            service.onMessage(new BisqEasyMediatorsResponse("made-up-trade-id-" + i, mediator));
        }

        assertEquals(100, service.getNumPendingMediatorsResponses());
    }

    @Test
    void responseOfOtherSenderIsNotKeptAsPending() {
        service.onMessage(new BisqEasyMediatorsResponse(TRADE_ID, networkId("other-sender")));

        assertEquals(0, service.getNumPendingMediatorsResponses());
    }

    // The pending responses are bounded. Responses of another sender would otherwise push the mediator's response out.
    @Test
    void responsesOfOtherSenderDoNotPushTheMediatorsResponseOut() {
        service.onMessage(new BisqEasyMediatorsResponse(TRADE_ID, mediator));
        NetworkId otherSender = networkId("other-sender");
        for (int i = 0; i < 120; i++) {
            service.onMessage(new BisqEasyMediatorsResponse("made-up-trade-id-" + i, otherSender));
        }

        assertEquals(1, service.getNumPendingMediatorsResponses());
    }

    // The service processes the pending responses on a scheduler thread after a trade channel was added. A processed
    // response is no longer pending, so no pending response means that the processing has finished.
    private void awaitNoPendingResponses() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (service.getNumPendingMediatorsResponses() > 0) {
            if (System.nanoTime() > deadline) {
                fail("The pending responses were not processed");
            }
            Thread.sleep(10);
        }
    }

    private static BisqEasyOpenTradeChannel channelWithMediator(String tradeId, NetworkId mediator) {
        UserProfile mediatorProfile = mock(UserProfile.class);
        when(mediatorProfile.getId()).thenReturn(mediator.getId());
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class);
        when(channel.getTradeId()).thenReturn(tradeId);
        when(channel.getMediator()).thenReturn(Optional.of(mediatorProfile));
        return channel;
    }

    private static AuthorizedBondedRole mediatorRole(NetworkId networkId) {
        return new AuthorizedBondedRole(networkId.getId(),
                "public-key",
                BondedRoleType.MEDIATOR,
                networkId.getPubKey().getKeyId(),
                "signature",
                Optional.empty(),
                networkId,
                Optional.empty(),
                false);
    }

    static NetworkId networkId(String keyId) {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }
}

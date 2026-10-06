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
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.util.StringUtils;
import bisq.contract.TradeId;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.NetworkService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The chat service decides which trade channel the mediator uses for an accepted mediation request
 * (docs/specifications/trade/chat-channel-binding.md, section "Channel on the mediator's node"). The mediator service
 * does not ignore a request because of an existing channel of the trade, unless the channel belongs to another
 * identity of the mediator's node. Only the first request of a trade gets the mediator's responses
 * (docs/specifications/support/mediation-request-validation.md).
 */
class BisqEasyMediatorServiceChannelTest {
    private UserIdentityService userIdentityService;
    private NetworkService networkService;
    private BisqEasyOpenTradeChannelService openTradeChannelService;
    private BisqEasyMediatorService mediatorService;
    private UserProfile mediator;
    private UserProfile maker;
    private UserProfile taker;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getStorePath()).thenReturn(Path.of("test"));
        PersistenceService persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);

        UserIdentity mediatorIdentity = TestUserProfiles.createUserIdentity("mediator");
        mediator = mediatorIdentity.getUserProfile();
        userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.findUserIdentity(mediatorIdentity.getId())).thenReturn(Optional.of(mediatorIdentity));
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));

        networkService = mock(NetworkService.class);
        openTradeChannelService = new BisqEasyOpenTradeChannelService(persistenceService, networkService, userService);
        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);

        AuthorizedBondedRole mediatorRole = mock(AuthorizedBondedRole.class);
        when(mediatorRole.getBondedRoleType()).thenReturn(BondedRoleType.MEDIATOR);
        when(mediatorRole.getProfileId()).thenReturn(mediatorIdentity.getId());
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream(anyBoolean()))
                .thenAnswer(invocation -> Stream.of(mediatorRole));
        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);

        mediatorService = new BisqEasyMediatorService(persistenceService,
                networkService,
                chatService,
                userService,
                bondedRolesService);

        maker = TestUserProfiles.create("maker");
        taker = TestUserProfiles.create("taker");
    }

    // Older versions created a channel with the sender as the only trader from a trade chat message with an offer which
    // reached the mediator before the request. The chat service replaces such a stored channel by the channel of the
    // request.
    @Test
    void requestAfterAChatMessageCreatedTheChannelOpensACase() {
        BisqEasyContract contract = TestContracts.contract(maker, taker, mediator);
        String tradeId = TradeId.fromContract(contract);
        channelCreatedByChatMessageOfOlderVersion(chatMessage(contract, taker, mediator));
        assertEquals(Set.of(taker.getId()), traderIds(findChannel(tradeId)));

        mediatorService.onMessage(mediationRequest(contract, maker, taker));

        assertEquals(1, mediatorService.getMediationCases().size());
        assertEquals(Set.of(maker.getId(), taker.getId()), traderIds(findChannel(tradeId)));
    }

    // The mediator's node can trade in the trade under another identity. The chat service keeps the channel of that
    // identity, and the mediator does not take the case into it.
    @Test
    void requestForATradeOfAnotherOwnIdentityIsIgnored() {
        UserIdentity tradersIdentity = TestUserProfiles.createUserIdentity("trader");
        when(userIdentityService.findUserIdentity(tradersIdentity.getId())).thenReturn(Optional.of(tradersIdentity));
        UserProfile trader = tradersIdentity.getUserProfile();
        BisqEasyContract contract = TestContracts.contract(trader, taker, mediator);
        String tradeId = TradeId.fromContract(contract);
        // The node trades in this trade under the other identity
        openTradeChannelService.setIsTradeWithPeer((id, userProfileId) -> id.equals(tradeId) &&
                userProfileId.equals(taker.getId()));
        openTradeChannelService.onMessage(chatMessage(contract, taker, trader));

        mediatorService.onMessage(mediationRequest(contract, taker, trader));

        assertTrue(mediatorService.getMediationCases().isEmpty());
        BisqEasyOpenTradeChannel channel = findChannel(tradeId);
        assertEquals(tradersIdentity.getId(), channel.getMyUserIdentity().getId());
        assertFalse(channel.isInMediation());
        verify(networkService, never()).confidentialSend(isA(BisqEasyMediatorsResponse.class), any(), any());
    }

    // The responses to the first request tell both traders that the mediator joined
    @Test
    void requestOfTheOtherTraderGetsNoResponse() {
        BisqEasyContract contract = TestContracts.contract(maker, taker, mediator);

        mediatorService.onMessage(mediationRequest(contract, maker, taker));
        mediatorService.onMessage(mediationRequest(contract, taker, maker));

        assertEquals(1, mediatorService.getMediationCases().size());
        assertEquals(2, mediatorService.getMediationCases().iterator().next().getMediationRequests().size());
        assertOneResponseToEachTrader(TradeId.fromContract(contract));
    }

    @Test
    void requestWithAnotherContractGetsNoResponse() {
        BisqEasyContract contract = TestContracts.contract(maker, taker, mediator);
        BisqEasyContract otherContract = TestContracts.contract(maker, taker, mediator, List.of("en", "de"));

        mediatorService.onMessage(mediationRequest(contract, maker, taker));
        mediatorService.onMessage(mediationRequest(otherContract, taker, maker));

        assertEquals(2, mediatorService.getMediationCases().size());
        assertOneResponseToEachTrader(TradeId.fromContract(contract));
    }

    private void assertOneResponseToEachTrader(String tradeId) {
        verify(networkService).confidentialSend(isA(BisqEasyMediatorsResponse.class), eq(maker.getNetworkId()), any());
        verify(networkService).confidentialSend(isA(BisqEasyMediatorsResponse.class), eq(taker.getNetworkId()), any());
        long numResponseMessages = findChannel(tradeId).getChatMessages().stream()
                .filter(message -> message.getChatMessageType() == ChatMessageType.PROTOCOL_LOG_MESSAGE)
                .count();
        assertEquals(2, numResponseMessages);
    }

    // The predicate which holds for any trade lets the message create the channel, as older versions did without a
    // trade. Such a channel can be in the persisted store.
    private void channelCreatedByChatMessageOfOlderVersion(BisqEasyOpenTradeMessage message) {
        openTradeChannelService.setIsTradeWithPeer((tradeId, userProfileId) -> true);
        openTradeChannelService.onMessage(message);
        openTradeChannelService.setIsTradeWithPeer((tradeId, userProfileId) -> false);
    }

    private BisqEasyMediationRequest mediationRequest(BisqEasyContract contract,
                                                      UserProfile requester,
                                                      UserProfile peer) {
        return new BisqEasyMediationRequest(TradeId.fromContract(contract),
                contract,
                requester,
                peer,
                List.of(),
                mediator.getNetworkId());
    }

    // A trade chat message with the offer of the contract, as the first message of a trade carries it
    private BisqEasyOpenTradeMessage chatMessage(BisqEasyContract contract, UserProfile sender, UserProfile receiver) {
        String tradeId = TradeId.fromContract(contract);
        return new BisqEasyOpenTradeMessage(tradeId,
                StringUtils.createUid(),
                BisqEasyOpenTradeChannel.createId(tradeId),
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.of(mediator),
                ChatMessageType.TEXT,
                Optional.of(contract.getOffer()),
                new HashSet<>());
    }

    private BisqEasyOpenTradeChannel findChannel(String tradeId) {
        return openTradeChannelService.findChannelByTradeId(tradeId).orElseThrow();
    }

    private static Set<String> traderIds(BisqEasyOpenTradeChannel channel) {
        return channel.getTraders().stream().map(UserProfile::getId).collect(Collectors.toSet());
    }
}

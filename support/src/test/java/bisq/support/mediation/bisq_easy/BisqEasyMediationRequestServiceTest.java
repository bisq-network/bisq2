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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.contract.TradeId;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A mediation request goes to the mediator of the signed contract. The chat routes messages to the mediator and
 * accepts mediator responses by the channel's copy, so a channel whose mediator differs from the contract's one
 * cannot request mediation. The mediator ignores a request whose requester and peer are not the parties of the
 * contract, so a channel with another own identity or peer cannot request mediation either.
 */
class BisqEasyMediationRequestServiceTest {
    private NetworkService networkService;
    private BisqEasyOpenTradeChannelService openTradeChannelService;
    private BisqEasyMediationRequestService service;
    private UserIdentity makerIdentity;
    private UserProfile taker;
    private UserProfile mediator;
    private BisqEasyContract contract;
    private String tradeId;

    @BeforeEach
    void setUp() {
        networkService = mock(NetworkService.class);
        UserService userService = mock(UserService.class);
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        openTradeChannelService = mock(BisqEasyOpenTradeChannelService.class);
        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);
        service = new BisqEasyMediationRequestService(networkService,
                chatService,
                userService,
                mock(BondedRolesService.class));

        makerIdentity = createUserIdentity("maker");
        taker = createUserProfile("taker");
        mediator = createUserProfile("mediator");
        contract = new BisqEasyContract(System.currentTimeMillis(),
                createOffer(makerIdentity.getUserProfile().getNetworkId()),
                taker.getNetworkId(),
                100_000,
                100_0000,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.of(mediator),
                new MarketPriceSpec(),
                60_000_0000);
        tradeId = TradeId.fromContract(contract);
    }

    private BisqEasyOpenTradeChannel makersChannel(Optional<UserProfile> channelMediator) {
        return makersChannel(makerIdentity, taker, channelMediator);
    }

    private BisqEasyOpenTradeChannel makersChannel(UserIdentity myUserIdentity,
                                                   UserProfile peer,
                                                   Optional<UserProfile> channelMediator) {
        return BisqEasyOpenTradeChannel.create(tradeId, contract.getOffer(), myUserIdentity, Set.of(peer), channelMediator, false);
    }

    @Test
    void requestGoesToTheContractMediator() throws UnresolvableProtobufException {
        // The channel holds its own copy of the mediator's profile, as it came with the taker's first chat message.
        UserProfile channelMediator = UserProfile.fromProto(mediator.toProto(false));
        BisqEasyOpenTradeChannel channel = makersChannel(Optional.of(channelMediator));
        assertTrue(service.findChannelMismatch(channel, contract).isEmpty());

        service.requestMediation(channel, contract);

        ArgumentCaptor<BisqEasyMediationRequest> request = ArgumentCaptor.forClass(BisqEasyMediationRequest.class);
        verify(networkService).confidentialSend(request.capture(), eq(mediator.getNetworkId()), any());
        assertEquals(mediator.getNetworkId(), request.getValue().getMediatorNetworkId());
        assertEquals(makerIdentity.getUserProfile(), request.getValue().getRequester());
        assertEquals(taker, request.getValue().getPeer());
    }

    @Test
    void requestCarriesOnlyMessagesOfTheTrade() {
        BisqEasyOpenTradeChannel channel = makersChannel(Optional.of(mediator));
        String channelIdOfTrade = BisqEasyOpenTradeChannel.createId(tradeId);
        BisqEasyOpenTradeMessage messageOfTrade = message(taker, tradeId, channelIdOfTrade);
        channel.addChatMessage(messageOfTrade);
        // The taker chooses the IDs of its messages, and the channel holds messages of the mediator as well. The
        // mediator ignores a request with a message of another trade, channel or sender.
        channel.addChatMessage(message(taker, "other-trade-id", channelIdOfTrade));
        channel.addChatMessage(message(taker, tradeId, BisqEasyOpenTradeChannel.createId("other-trade-id")));
        channel.addChatMessage(message(mediator, tradeId, channelIdOfTrade));

        service.requestMediation(channel, contract);

        ArgumentCaptor<BisqEasyMediationRequest> request = ArgumentCaptor.forClass(BisqEasyMediationRequest.class);
        verify(networkService).confidentialSend(request.capture(), eq(mediator.getNetworkId()), any());
        assertEquals(List.of(messageOfTrade), request.getValue().getChatMessages());
    }

    // The API sends the request through the service as the desktop does. The requester's messages go to the mediator
    // from the request on.
    @Test
    void requestPutsTheChannelInMediation() {
        BisqEasyOpenTradeChannel channel = makersChannel(Optional.of(mediator));

        service.requestMediation(channel, contract);

        verify(openTradeChannelService).setIsInMediation(channel, true);
    }

    @Test
    void refusedRequestDoesNotPutTheChannelInMediation() {
        BisqEasyOpenTradeChannel channel = makersChannel(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.requestMediation(channel, contract));

        verify(openTradeChannelService, never()).setIsInMediation(any(), anyBoolean());
    }

    @Test
    void requestWithAnotherOfferInTheChannelIsRefused() {
        BisqEasyOpenTradeChannel channel = BisqEasyOpenTradeChannel.create(tradeId,
                createOffer(makerIdentity.getUserProfile().getNetworkId(), 200_0000),
                makerIdentity,
                Set.of(taker),
                Optional.of(mediator),
                false);
        assertTrue(service.findChannelMismatch(channel, contract).isPresent());

        assertThrows(IllegalArgumentException.class, () -> service.requestMediation(channel, contract));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void requestWithAnotherMediatorInTheChannelIsRefused() {
        BisqEasyOpenTradeChannel channel = makersChannel(Optional.of(createUserProfile("other-mediator")));
        assertTrue(service.findChannelMismatch(channel, contract).isPresent());

        assertThrows(IllegalArgumentException.class, () -> service.requestMediation(channel, contract));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void requestWithoutMediatorInTheChannelIsRefused() {
        BisqEasyOpenTradeChannel channel = makersChannel(Optional.empty());
        assertTrue(service.findChannelMismatch(channel, contract).isPresent());

        assertThrows(IllegalArgumentException.class, () -> service.requestMediation(channel, contract));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void requestFromAnotherLocalIdentityInTheChannelIsRefused() {
        BisqEasyOpenTradeChannel channel = makersChannel(createUserIdentity("other-maker-identity"), taker, Optional.of(mediator));
        assertTrue(service.findChannelMismatch(channel, contract).isPresent());

        assertThrows(IllegalArgumentException.class, () -> service.requestMediation(channel, contract));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void requestWithAnotherPeerInTheChannelIsRefused() {
        BisqEasyOpenTradeChannel channel = makersChannel(makerIdentity, createUserProfile("other-taker"), Optional.of(mediator));
        assertTrue(service.findChannelMismatch(channel, contract).isPresent());

        assertThrows(IllegalArgumentException.class, () -> service.requestMediation(channel, contract));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    private BisqEasyOpenTradeMessage message(UserProfile sender, String messageTradeId, String channelId) {
        return new BisqEasyOpenTradeMessage(messageTradeId,
                StringUtils.createUid(),
                channelId,
                sender,
                makerIdentity.getId(),
                makerIdentity.getUserProfile().getNetworkId(),
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }

    private static BisqEasyOffer createOffer(NetworkId makerNetworkId) {
        return createOffer(makerNetworkId, 100_0000);
    }

    private static BisqEasyOffer createOffer(NetworkId makerNetworkId, long quoteAmount) {
        return new BisqEasyOffer("offer-id",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(quoteAmount),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                "2.1.13");
    }

    private static UserIdentity createUserIdentity(String nickName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId networkId = createNetworkId(nickName, keyPair);
        KeyBundle keyBundle = new KeyBundle(nickName, keyPair, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair());
        return new UserIdentity(new Identity(nickName, networkId, keyBundle), createUserProfile(nickName, networkId));
    }

    private static UserProfile createUserProfile(String nickName) {
        return createUserProfile(nickName, createNetworkId(nickName, KeyGeneration.generateDefaultEcKeyPair()));
    }

    private static UserProfile createUserProfile(String nickName, NetworkId networkId) {
        return new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                "",
                "");
    }

    private static NetworkId createNetworkId(String keyId, KeyPair keyPair) {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }
}

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

package bisq.chat.bisq_easy.open_trades;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BisqEasyOpenTradeChannelServiceTest {
    private static final String TAKER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String MAKER_PROFILE_ID = "2222222222222222222222222222222222222222";
    private static final String OTHER_PROFILE_ID = "3333333333333333333333333333333333333333";
    private static final String TRADE_ID = "tradeId";
    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    private BisqEasyOpenTradeChannelService service;
    private UserIdentityService userIdentityService;
    private UserProfileService userProfileService;
    private BannedUserService bannedUserService;
    private NetworkService networkService;
    private UserProfile taker;
    private UserIdentity makerIdentity;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        userIdentityService = mock(UserIdentityService.class);
        userProfileService = mock(UserProfileService.class);
        bannedUserService = mock(BannedUserService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(userService.getContactListService()).thenReturn(mock(ContactListService.class));

        makerIdentity = mock(UserIdentity.class);
        when(makerIdentity.getId()).thenReturn(MAKER_PROFILE_ID);
        when(userIdentityService.findUserIdentity(MAKER_PROFILE_ID)).thenReturn(Optional.of(makerIdentity));

        taker = mock(UserProfile.class);
        when(taker.getId()).thenReturn(TAKER_PROFILE_ID);

        networkService = mock(NetworkService.class);
        service = new BisqEasyOpenTradeChannelService(persistenceService, networkService, userService);
    }

    @Test
    void takeOfferMessageCreatesChannel() {
        tradeWithTakerExists();
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOpenTradeMessage message = takeOfferMessage(offer, MAKER_PROFILE_ID, System.currentTimeMillis());

        service.onMessage(message);

        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertSame(offer, channel.getBisqEasyOffer());
        assertTrue(channel.getChatMessages().contains(message));
    }

    @Test
    void takeOfferMessageJoinsChannelCreatedByTaker() {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOpenTradeChannel channel = service.traderFindOrCreatesChannel(TRADE_ID, offer, makerIdentity, taker, Optional.empty());

        BisqEasyOpenTradeMessage message = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(message);

        assertEquals(1, service.getChannels().size());
        assertSame(channel, service.findChannelByTradeId(TRADE_ID).orElseThrow());
        assertSame(offer, channel.getBisqEasyOffer());
        assertTrue(channel.getChatMessages().contains(message));
    }

    @Test
    void messageWithTheTradeIdOfAnotherTradeIsNotAddedToTheChannel() {
        BisqEasyOpenTradeChannel channel = service.traderFindOrCreatesChannel(TRADE_ID, mock(BisqEasyOffer.class),
                makerIdentity, taker, Optional.empty());
        BisqEasyOpenTradeMessage messageOfTrade = textMessage(TRADE_ID, "messageOfTrade", MAKER_PROFILE_ID,
                System.currentTimeMillis());
        BisqEasyOpenTradeMessage messageOfAnotherTrade = textMessage("otherTradeId", "messageOfAnotherTrade",
                MAKER_PROFILE_ID, System.currentTimeMillis());

        service.onMessage(messageOfTrade);
        service.onMessage(messageOfAnotherTrade);

        assertEquals(Set.of(messageOfTrade), channel.getChatMessages());
    }

    @Test
    void expiredTakeOfferMessageIsIgnored() {
        long elevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(11);

        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, elevenDaysAgo));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void takeOfferMessageForUnknownReceiverIdentityIsIgnored() {
        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), "3333333333333333333333333333333333333333", System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void expiredMessageWithoutOfferIsDroppedAndNotParked() {
        tradeWithTakerExists();
        long elevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(11);
        service.onMessage(textMessage(MAKER_PROFILE_ID, elevenDaysAgo));

        BisqEasyOpenTradeMessage takeOfferMessage = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(takeOfferMessage);

        // A parked message would be added to the channel once the take offer message has created it.
        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertEquals(1, channel.getChatMessages().size());
        assertTrue(channel.getChatMessages().contains(takeOfferMessage));
    }

    @Test
    void messageWithoutOfferForUnknownReceiverIdentityIsDroppedAndNotParked() {
        tradeWithTakerExists();
        service.onMessage(textMessage("3333333333333333333333333333333333333333", System.currentTimeMillis()));

        BisqEasyOpenTradeMessage takeOfferMessage = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(takeOfferMessage);

        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertEquals(1, channel.getChatMessages().size());
        assertTrue(channel.getChatMessages().contains(takeOfferMessage));
    }

    // The network layer decrypted the message with the key of its receiver network ID. A message which names another
    // of our identities as receiver must not open a channel of that identity.
    @Test
    void takeOfferMessageWhoseReceiverProfileIdIsNotTheIdOfItsReceiverNetworkIdIsIgnored() {
        tradeWithTakerExists();
        String otherOwnProfileId = "4444444444444444444444444444444444444444";
        when(userIdentityService.findUserIdentity(otherOwnProfileId)).thenReturn(Optional.of(mock(UserIdentity.class)));

        // Such a message fails at parsing (PrivateChatMessage.verify), so it never reaches the service
        assertThrows(IllegalArgumentException.class, () -> takeOfferMessage(mock(BisqEasyOffer.class),
                MAKER_PROFILE_ID,
                networkIdOf(otherOwnProfileId),
                System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void takeOfferMessageWithoutTradeCreatesNoChannel() {
        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void takeOfferMessageFromAnotherUserThanTheTradePeerCreatesNoChannel() {
        service.setIsTradeWithPeer((tradeId, senderUserProfileId, receiverUserProfileId) ->
                senderUserProfileId.equals(OTHER_PROFILE_ID));

        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    // The trade service decides with the receiver whether the trade is ours under this user profile (rule 6 of
    // docs/specifications/trade/pending-trade-messages.md)
    @Test
    void tradeCheckGetsTheSenderAndTheReceiverOfTheMessage() {
        List<String> checkedProfiles = new ArrayList<>();
        service.setIsTradeWithPeer((tradeId, senderUserProfileId, receiverUserProfileId) -> {
            checkedProfiles.add(senderUserProfileId + " to " + receiverUserProfileId);
            return false;
        });

        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis()));

        assertFalse(checkedProfiles.isEmpty());
        assertTrue(checkedProfiles.stream().allMatch(profiles -> profiles.equals(TAKER_PROFILE_ID + " to " + MAKER_PROFILE_ID)),
                checkedProfiles.toString());
    }

    @Test
    void pendingTakeOfferMessageCreatesChannelWhenTradeExists() {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOpenTradeMessage message = takeOfferMessage(offer, MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(message);
        assertTrue(service.getChannels().isEmpty());

        tradeWithTakerExists();
        service.processPendingMessages(TRADE_ID);

        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertSame(offer, channel.getBisqEasyOffer());
        assertTrue(channel.getChatMessages().contains(message));
    }

    // The trade service creates the trade and processes the pending messages after the check, before the message is
    // pending. The service checks the trade again after it kept the message as pending.
    @Test
    void tradeCreatedWhileTheMessageBecomesPendingCreatesChannel() {
        AtomicInteger checks = new AtomicInteger();
        service.setIsTradeWithPeer((tradeId, senderUserProfileId, receiverUserProfileId) -> checks.incrementAndGet() > 1);
        BisqEasyOpenTradeMessage message = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());

        service.onMessage(message);

        assertTrue(service.findChannelByTradeId(TRADE_ID).orElseThrow().getChatMessages().contains(message));
    }

    // Another thread can create the channel after the lookup of a message found none, and process the pending
    // messages before the message became pending. The message must still reach the channel (rule 6 of
    // docs/specifications/trade/pending-trade-messages.md).
    @Test
    void messageWhoseChannelIsCreatedWhileItBecomesPendingIsAdded() {
        BisqEasyOpenTradeChannel channel = service.traderFindOrCreatesChannel(TRADE_ID, mock(BisqEasyOffer.class),
                makerIdentity, taker, Optional.empty());
        BisqEasyOpenTradeChannelService spiedService = spy(service);
        AtomicBoolean firstLookup = new AtomicBoolean(true);
        doAnswer(invocation -> firstLookup.getAndSet(false) ? Optional.empty() : invocation.callRealMethod())
                .when(spiedService).findChannel(any(ChatMessage.class));
        BisqEasyOpenTradeMessage message = textMessage(MAKER_PROFILE_ID, System.currentTimeMillis());

        spiedService.onMessage(message);

        assertEquals(Set.of(message), channel.getChatMessages());
        assertEquals(0, spiedService.getNumPendingMessages());
    }

    // A channel which another thread created while the message became pending can reject the message, for example
    // because its sender is not a peer of the channel. Such a message does not stay pending, as the channel would
    // reject it again.
    @Test
    void messageWhichTheChannelRejectsAfterItBecamePendingIsNotKept() {
        BisqEasyOpenTradeChannel channel = service.traderFindOrCreatesChannel(TRADE_ID, mock(BisqEasyOffer.class),
                makerIdentity, taker, Optional.empty());
        BisqEasyOpenTradeChannelService spiedService = spy(service);
        AtomicBoolean firstLookup = new AtomicBoolean(true);
        doAnswer(invocation -> firstLookup.getAndSet(false) ? Optional.empty() : invocation.callRealMethod())
                .when(spiedService).findChannel(any(ChatMessage.class));
        UserProfile otherUser = mock(UserProfile.class);
        when(otherUser.getId()).thenReturn(OTHER_PROFILE_ID);

        spiedService.onMessage(textMessage(otherUser, TRADE_ID, "messageOfOtherUser", MAKER_PROFILE_ID,
                System.currentTimeMillis()));

        assertTrue(channel.getChatMessages().isEmpty());
        assertEquals(0, spiedService.getNumPendingMessages());
    }

    // A received chat message is processed alone. A pending message waits until the node creates its trade or its
    // channel, which processes the pending messages of that trade again (rule 6 of
    // docs/specifications/trade/pending-trade-messages.md).
    @Test
    void receivedMessageDoesNotProcessOtherPendingMessagesAgain() {
        List<String> checkedTradeIds = new ArrayList<>();
        service.setIsTradeWithPeer((tradeId, senderUserProfileId, receiverUserProfileId) -> {
            checkedTradeIds.add(tradeId);
            return false;
        });
        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis()));
        checkedTradeIds.clear();

        service.onMessage(textMessage("otherTradeId", "messageOfOtherTrade", MAKER_PROFILE_ID,
                System.currentTimeMillis()));

        assertFalse(checkedTradeIds.contains(TRADE_ID), checkedTradeIds.toString());
    }

    @Test
    void sendTakeOfferMessageFailsWhenMakerNotFound() {
        BisqEasyOffer offer = createOffer(createNetworkId(4001));
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.empty());

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertTrue(result.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, result::get);
        assertTrue(ex.getCause().getMessage().contains("makerUserProfile not found"));
    }

    @Test
    void sendTakeOfferMessageFailsWhenMakerIsBanned() {
        NetworkId makerNetworkId = createNetworkId(4001);
        UserProfile makerProfile = createUserProfile(makerNetworkId);
        BisqEasyOffer offer = createOffer(makerNetworkId);
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
        when(bannedUserService.isUserProfileBanned(makerProfile)).thenReturn(true);

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertTrue(result.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, result::get);
        assertTrue(ex.getCause().getMessage().contains("Maker is banned"));
    }

    @Test
    void sendTakeOfferMessageFailsWhenSelfIsBanned() {
        NetworkId makerNetworkId = createNetworkId(4001);
        UserProfile makerProfile = createUserProfile(makerNetworkId);
        BisqEasyOffer offer = createOffer(makerNetworkId);
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
        when(bannedUserService.isUserProfileBanned(makerProfile)).thenReturn(false);

        NetworkId takerNetworkId = createNetworkId(4002);
        UserProfile takerProfile = createUserProfile(takerNetworkId);
        UserIdentity takerIdentity = createUserIdentity(takerProfile, takerNetworkId);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(takerIdentity);
        when(bannedUserService.isUserProfileBanned(takerProfile)).thenReturn(true);

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertTrue(result.isCompletedExceptionally());
        verify(networkService, never()).confidentialSend(any(), any(), any());
        assertTrue(service.findChannelByTradeId("trade-1").isEmpty());
    }

    @Test
    void sendTakeOfferMessageCreatesChannelAndSends() {
        NetworkId makerNetworkId = createNetworkId(4001);
        UserProfile makerProfile = createUserProfile(makerNetworkId);
        BisqEasyOffer offer = createOffer(makerNetworkId);
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(String.class))).thenReturn(false);
        when(bannedUserService.isRateLimitExceeding(any(String.class))).thenReturn(false);

        NetworkId takerNetworkId = createNetworkId(4002);
        UserProfile takerProfile = createUserProfile(takerNetworkId);
        UserIdentity takerIdentity = createUserIdentity(takerProfile, takerNetworkId);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(takerIdentity);

        SendMessageResult sendResult = new SendMessageResult();
        when(networkService.confidentialSend(any(), any(NetworkId.class), any())).thenReturn(CompletableFuture.completedFuture(sendResult));

        CompletableFuture<SendMessageResult> result = service.sendTakeOfferMessage("trade-1", offer, Optional.empty());

        assertFalse(result.isCompletedExceptionally());
        assertDoesNotThrow(() -> result.get());
        ArgumentCaptor<EnvelopePayloadMessage> sent = ArgumentCaptor.forClass(EnvelopePayloadMessage.class);
        verify(networkService).confidentialSend(sent.capture(), eq(makerNetworkId), any());
        BisqEasyOpenTradeMessage takeOfferMessage = assertInstanceOf(BisqEasyOpenTradeMessage.class, sent.getValue());
        assertEquals(ChatMessageType.TAKE_BISQ_EASY_OFFER, takeOfferMessage.getChatMessageType());
        assertEquals(Optional.of(offer), takeOfferMessage.getBisqEasyOffer());
        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId("trade-1").orElseThrow();
        assertTrue(channel.getChatMessages().contains(takeOfferMessage));
    }

    private void tradeWithTakerExists() {
        service.setIsTradeWithPeer((tradeId, senderUserProfileId, receiverUserProfileId) ->
                tradeId.equals(TRADE_ID) && senderUserProfileId.equals(TAKER_PROFILE_ID));
    }

    private BisqEasyOpenTradeMessage textMessage(String receiverUserProfileId, long date) {
        return textMessage(TRADE_ID, "textMessageId", receiverUserProfileId, date);
    }

    // A text message to the channel of TRADE_ID which carries the given trade ID
    private BisqEasyOpenTradeMessage textMessage(String tradeId,
                                                 String messageId,
                                                 String receiverUserProfileId,
                                                 long date) {
        return textMessage(taker, tradeId, messageId, receiverUserProfileId, date);
    }

    private BisqEasyOpenTradeMessage textMessage(UserProfile sender,
                                                 String tradeId,
                                                 String messageId,
                                                 String receiverUserProfileId,
                                                 long date) {
        return new BisqEasyOpenTradeMessage(tradeId,
                messageId,
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                sender,
                receiverUserProfileId,
                networkIdOf(receiverUserProfileId),
                "text",
                Optional.empty(),
                date,
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }

    private BisqEasyOpenTradeMessage takeOfferMessage(BisqEasyOffer bisqEasyOffer, String receiverUserProfileId, long date) {
        return takeOfferMessage(bisqEasyOffer, receiverUserProfileId, networkIdOf(receiverUserProfileId), date);
    }

    private BisqEasyOpenTradeMessage takeOfferMessage(BisqEasyOffer bisqEasyOffer,
                                                      String receiverUserProfileId,
                                                      NetworkId receiverNetworkId,
                                                      long date) {
        return new BisqEasyOpenTradeMessage(TRADE_ID,
                "messageId",
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                taker,
                receiverUserProfileId,
                receiverNetworkId,
                null,
                Optional.empty(),
                date,
                false,
                Optional.of(mock(UserProfile.class)),
                ChatMessageType.TAKE_BISQ_EASY_OFFER,
                Optional.of(bisqEasyOffer),
                new HashSet<>());
    }

    // The ID of a network ID is the hash of its public key, which is also the profile ID of that key
    private static NetworkId networkIdOf(String userProfileId) {
        NetworkId networkId = mock(NetworkId.class);
        when(networkId.getId()).thenReturn(userProfileId);
        return networkId;
    }

    private static NetworkId createNetworkId(int port) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
    }

    private static UserProfile createUserProfile(NetworkId networkId) {
        PubKey pubKey = networkId.getPubKey();
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick", proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static UserIdentity createUserIdentity(UserProfile userProfile, NetworkId networkId) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        var torKeyPair = TorKeyGeneration.generateKeyPair();
        var i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(userProfile.getId(), kp, torKeyPair, i2pKeyPair);
        Identity identity = new Identity(userProfile.getId(), networkId, keyBundle);
        return new UserIdentity(identity, userProfile);
    }

    private static BisqEasyOffer createOffer(NetworkId makerNid) {
        return new BisqEasyOffer(
                "offer-channel-test",
                System.currentTimeMillis(),
                makerNid,
                Direction.SELL,
                MARKET,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );
    }
}

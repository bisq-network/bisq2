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

package bisq.chat;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.chat.ChatMessageType.LEAVE;
import static bisq.chat.ChatMessageType.PROTOCOL_LOG_MESSAGE;
import static bisq.chat.ChatMessageType.TAKE_BISQ_EASY_OFFER;
import static bisq.chat.ChatMessageType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReceivedChatMessageTypeTest {
    private static final UserIdentity MY_USER_IDENTITY = ChatTestUsers.createUserIdentity("me");
    private static final UserProfile PEER = ChatTestUsers.createUserIdentity("peer").getUserProfile();
    private static final UserProfile OTHER_TRADER = ChatTestUsers.createUserIdentity("otherTrader").getUserProfile();
    private static final String TRADE_ID = "tradeId";

    private PersistenceService persistenceService;
    private NetworkService networkService;
    private UserService userService;
    private ContactListService contactListService;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        // Read by the shutdown hook of RateLimitedPersistenceClient
        when(persistence.getStorePath()).thenReturn(Path.of("test"));
        persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any(PersistableStore.class)))
                .thenReturn(persistence);

        networkService = mock(NetworkService.class);

        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.findUserIdentity(MY_USER_IDENTITY.getId())).thenReturn(Optional.of(MY_USER_IDENTITY));
        contactListService = mock(ContactListService.class);
        userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getContactListService()).thenReturn(contactListService);
    }

    @Nested
    class TwoPartyChat {
        private TwoPartyPrivateChatChannelService service;

        @BeforeEach
        void setUp() {
            SettingsService settingsService = mock(SettingsService.class);
            when(settingsService.getDoAutoAddToContactList()).thenReturn(true);
            service = new TwoPartyPrivateChatChannelService(persistenceService,
                    networkService,
                    userService,
                    settingsService,
                    ChatChannelDomain.DISCUSSION);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"TEXT", "LEAVE"}, mode = EnumSource.Mode.EXCLUDE)
        void firstMessageOfUnsupportedTypeOpensNoChannelAndAddsNoContact(ChatMessageType type) {
            service.onMessage(message(type));

            assertTrue(service.getChannels().isEmpty());
            verifyNoInteractions(contactListService);
        }

        @Test
        void firstTextMessageOpensChannelAndAddsContact() {
            TwoPartyPrivateChatMessage message = message(TEXT);

            service.onMessage(message);

            assertTrue(service.findChannel(message).orElseThrow().getChatMessages().contains(message));
            verify(contactListService).addContactListEntry(PEER, MY_USER_IDENTITY.getUserProfile(), ContactReason.PRIVATE_CHAT);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"TEXT", "LEAVE"}, mode = EnumSource.Mode.EXCLUDE)
        void messageOfUnsupportedTypeIsNotAddedToOpenChannel(ChatMessageType type) {
            TwoPartyPrivateChatMessage textMessage = message(TEXT);
            service.onMessage(textMessage);

            service.onMessage(message(type));

            TwoPartyPrivateChatChannel channel = service.findChannel(textMessage).orElseThrow();
            assertEquals(1, channel.getChatMessages().size());
            assertTrue(channel.getChatMessages().contains(textMessage));
        }

        private TwoPartyPrivateChatMessage message(ChatMessageType type) {
            return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                    ChatChannelDomain.DISCUSSION,
                    TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, PEER.getId(), MY_USER_IDENTITY.getId()),
                    PEER,
                    MY_USER_IDENTITY.getId(),
                    MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                    "text",
                    Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    type,
                    new HashSet<>());
        }
    }

    @Nested
    class BisqEasyTradeChat {
        private BisqEasyOpenTradeChannelService service;

        @BeforeEach
        void setUp() {
            service = new BisqEasyOpenTradeChannelService(persistenceService, networkService, userService);
            service.setIsTradeWithPeer((tradeId, userProfileId) -> true);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class,
                names = {"TEXT", "LEAVE", "TAKE_BISQ_EASY_OFFER", "PROTOCOL_LOG_MESSAGE"},
                mode = EnumSource.Mode.EXCLUDE)
        void messageOfUnsupportedTypeWithOfferOpensNoChannel(ChatMessageType type) {
            service.onMessage(message(type));

            assertTrue(service.getChannels().isEmpty());
        }

        @Test
        void takeOfferMessageOpensChannel() {
            BisqEasyOpenTradeMessage message = message(TAKE_BISQ_EASY_OFFER);

            service.onMessage(message);

            assertTrue(service.findChannelByTradeId(TRADE_ID).orElseThrow().getChatMessages().contains(message));
        }

        // The mediator adds the messages of a mediation request with addMessage
        @ParameterizedTest
        @EnumSource(ChatMessageType.class)
        void mediatorImportAddsOnlySupportedTypes(ChatMessageType type) {
            BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID,
                    mock(BisqEasyOffer.class),
                    MY_USER_IDENTITY,
                    PEER,
                    OTHER_TRADER);
            BisqEasyOpenTradeMessage message = message(type, OTHER_TRADER);

            service.addMessage(message, channel);

            boolean isSupported = EnumSet.of(TEXT, LEAVE, TAKE_BISQ_EASY_OFFER, PROTOCOL_LOG_MESSAGE).contains(type);
            assertEquals(isSupported, channel.getChatMessages().contains(message));
        }

        private BisqEasyOpenTradeMessage message(ChatMessageType type) {
            return message(type, MY_USER_IDENTITY.getUserProfile());
        }

        private BisqEasyOpenTradeMessage message(ChatMessageType type, UserProfile receiver) {
            return new BisqEasyOpenTradeMessage(TRADE_ID,
                    StringUtils.createUid(),
                    BisqEasyOpenTradeChannel.createId(TRADE_ID),
                    PEER,
                    receiver.getId(),
                    receiver.getNetworkId(),
                    "text",
                    Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    Optional.empty(),
                    type,
                    Optional.of(mock(BisqEasyOffer.class)),
                    new HashSet<>());
        }
    }

    @Nested
    class MuSigTradeChat {
        private MuSigOpenTradeChannelService service;

        @BeforeEach
        void setUp() {
            service = new MuSigOpenTradeChannelService(persistenceService, networkService, userService);
        }

        // A message for a trade without a channel waits until the trade opens the channel
        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class,
                names = {"TEXT", "LEAVE", "PROTOCOL_LOG_MESSAGE"},
                mode = EnumSource.Mode.EXCLUDE)
        void messageOfUnsupportedTypeIsNotAddedWhenChannelOpens(ChatMessageType type) {
            service.onMessage(message(type));

            assertTrue(openChannel().getChatMessages().isEmpty());
        }

        @Test
        void textMessageIsAddedWhenChannelOpens() {
            MuSigOpenTradeMessage message = message(TEXT);

            service.onMessage(message);

            assertTrue(openChannel().getChatMessages().contains(message));
        }

        private MuSigOpenTradeChannel openChannel() {
            return service.traderFindOrCreatesChannel(TRADE_ID, MY_USER_IDENTITY, PEER, Optional.empty(), Optional.empty());
        }

        private MuSigOpenTradeMessage message(ChatMessageType type) {
            return new MuSigOpenTradeMessage(TRADE_ID,
                    StringUtils.createUid(),
                    MuSigOpenTradeChannel.createId(TRADE_ID),
                    PEER,
                    MY_USER_IDENTITY.getId(),
                    MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                    "text",
                    Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    Optional.empty(),
                    Optional.empty(),
                    type,
                    Optional.empty(),
                    new HashSet<>());
        }
    }

    @Nested
    class PublicChat {
        private CommonPublicChatChannel channel;
        private CommonPublicChatChannelService service;

        @BeforeEach
        void setUp() {
            channel = new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, SubDomain.DISCUSSION_BISQ);
            service = new CommonPublicChatChannelService(persistenceService,
                    networkService,
                    userService,
                    ChatChannelDomain.DISCUSSION,
                    List.of(channel));
            service.initialize();
        }

        // A public message of another type fails at parsing (PublicChatMessageTypeTest)
        @Test
        void textMessageIsAdded() {
            CommonPublicChatMessage message = message(TEXT);

            service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(message));

            assertTrue(channel.getChatMessages().contains(message));
        }

        private CommonPublicChatMessage message(ChatMessageType type) {
            return new CommonPublicChatMessage(StringUtils.createUid(),
                    ChatChannelDomain.DISCUSSION,
                    channel.getId(),
                    PEER.getId(),
                    Optional.of("text"),
                    Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    type);
        }
    }
}

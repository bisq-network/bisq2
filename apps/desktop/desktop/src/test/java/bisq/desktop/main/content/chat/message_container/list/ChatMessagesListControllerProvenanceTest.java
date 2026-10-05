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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
import bisq.support.mediation.bisq_easy.BisqEasyMediatorService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.util.WaitForAsyncUtils;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The mediator's list marks a message which reached the mediator only inside a request of another trader. The mediator
 * service can import such a message while the mediator opens the channel.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class ChatMessagesListControllerProvenanceTest {
    private static final String TRADE_ID = "trade-id";

    private final Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>();
    private BisqEasyMediatorService mediatorService;
    private ChatMessagesListController controller;
    private BisqEasyOpenTradeChannel channel;
    private BisqEasyOpenTradeMessage forwardedMessageOfPeer;

    @BeforeEach
    void setUp() {
        UserProfile mediator = userProfile("mediator");
        UserIdentity mediatorIdentity = mock(UserIdentity.class);
        when(mediatorIdentity.getId()).thenReturn(mediator.getId());
        when(mediatorIdentity.getUserProfile()).thenReturn(mediator);
        UserProfile requester = userProfile("requester");
        UserProfile peer = userProfile("peer");
        channel = BisqEasyOpenTradeChannel.create(TRADE_ID,
                mock(BisqEasyOffer.class),
                mediatorIdentity,
                Set.of(requester, peer),
                Optional.of(mediator),
                true);
        // A message of the peer which the requester forwarded
        forwardedMessageOfPeer = new BisqEasyOpenTradeMessage(TRADE_ID,
                StringUtils.createUid(),
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                peer,
                mediator.getId(),
                mediator.getNetworkId(),
                "I sent the payment",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());

        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        when(userService.getUserProfileService().getIgnoredUserProfileIds()).thenReturn(new ObservableSet<>());
        when(userService.getUserIdentityService().getSelectedUserIdentity()).thenReturn(mediatorIdentity);
        when(userService.getUserIdentityService().getSelectedUserIdentityObservable())
                .thenReturn(new Observable<>(mediatorIdentity));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.findReputationScore(any(UserProfile.class))).thenReturn(Optional.empty());
        when(reputationService.getUserProfileIdWithScoreChange()).thenReturn(new Observable<>());
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());
        when(userService.getReputationService()).thenReturn(reputationService);

        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        when(chatService.getChatChannelSelectionServices()
                .get(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .getSelectedChannel())
                .thenReturn(selectedChannel);

        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getBisqEasyOfferbookMessageTypeFilter())
                .thenReturn(new Observable<>(bisq.settings.ChatMessageType.ALL));
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);

        mediatorService = mock(BisqEasyMediatorService.class);
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getChatService()).thenReturn(chatService);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);
        when(serviceProvider.getUserService()).thenReturn(userService);
        when(serviceProvider.getSupportService().getBisqEasyMediatorService()).thenReturn(mediatorService);
        when(serviceProvider.getBondedRolesService().getMarketPriceService())
                .thenReturn(mock(MarketPriceService.class));
        when(serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService().getAuthorizedBondedRoleStream())
                .thenAnswer(invocation -> Stream.empty());
        when(serviceProvider.getNetworkService().getMessageDeliveryStatusByMessageId())
                .thenReturn(new ObservableHashMap<>());
        when(serviceProvider.getNetworkService().getResendMessageService()).thenReturn(Optional.empty());

        WaitForAsyncUtils.asyncFx(() -> {
            controller = new ChatMessagesListController(serviceProvider,
                    userProfile -> {
                    },
                    chatMessage -> {
                    },
                    chatMessage -> {
                    },
                    () -> {
                    },
                    ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
            controller.onActivate();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() {
        WaitForAsyncUtils.asyncFx(() -> controller.onDeactivate());
        WaitForAsyncUtils.waitForFxEvents();
        Transitions.setSettingsService(null);
    }

    @Test
    void messageImportedWhileTheChannelOpensIsMarked() {
        // The mediator service retains the request before it imports its messages. Here the request is retained and its
        // message imported right after the list read the keys for the first time.
        AtomicBoolean imported = new AtomicBoolean();
        when(mediatorService.getUnverifiedChatMessageKeys(TRADE_ID)).thenAnswer(invocation -> {
            if (imported.compareAndSet(false, true)) {
                channel.addChatMessage(forwardedMessageOfPeer);
                return Set.of();
            }
            return Set.of(BisqEasyMediatorService.getProvenanceKey(forwardedMessageOfPeer));
        });

        selectedChannel.set(channel);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(controller.getModel().getChatMessages())
                .filteredOn(item -> item.getChatMessage().equals(forwardedMessageOfPeer))
                .singleElement()
                .matches(ChatMessageListItem::isUnverifiedForwardedMessage);
    }

    private static UserProfile userProfile(String nickName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), nickName));
        return new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                "",
                "");
    }
}

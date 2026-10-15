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

package bisq.trade.bisq_easy;

import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.user.banned.BannedUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * "Same restrictions as a new message" in docs/specifications/network/startup-replay.md: the trade service subscribes
 * to confidential messages at startup, and the subscription replays the messages which the node processed before, for
 * example from its mailbox. A replayed trade message must not pass a halt of trading which the node knows.
 */
class BisqEasyTradeServiceStartupReplayRestrictionTest {
    private ObservableSet<AuthorizedAlertData> alerts;
    private BannedUserService bannedUserService;
    private BisqEasyTradeMessage replayedMessage;
    private BisqEasyTradeService service;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        alerts = new ObservableSet<>();
        when(serviceProvider.getBondedRolesService().getAlertService().getAuthorizedAlertDataSet()).thenReturn(alerts);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService()).thenReturn(channelService);
        when(channelService.getChannels()).thenReturn(new ObservableSet<>());
        bannedUserService = mock(BannedUserService.class);
        when(serviceProvider.getUserService().getBannedUserService()).thenReturn(bannedUserService);

        NetworkId sender = mock(NetworkId.class);
        replayedMessage = mock(BisqEasyTradeMessage.class);
        when(replayedMessage.getTradeId()).thenReturn("tradeId");
        when(replayedMessage.getSender()).thenReturn(sender);
        // The subscription replays the processed messages to the new listener before it returns
        NetworkService networkService = serviceProvider.getNetworkService();
        doAnswer(invocation -> {
            invocation.<ConfidentialMessageService.Listener>getArgument(0).onMessage(replayedMessage);
            return null;
        }).when(networkService).subscribeToConfidentialMessages(any());

        service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void replayedTradeMessageIsIgnoredDuringAStoredHaltOfTrading() {
        alerts.add(haltOfTrading());

        service.initialize();

        // The service checks the sender only after the trade restrictions
        verify(bannedUserService, never()).isUserProfileBanned(any(NetworkId.class));
    }

    @Test
    void replayedTradeMessageIsProcessedWithoutRestriction() {
        service.initialize();

        verify(bannedUserService).isUserProfileBanned(replayedMessage.getSender());
    }

    private static AuthorizedAlertData haltOfTrading() {
        return new AuthorizedAlertData("alertId",
                System.currentTimeMillis(),
                AlertType.EMERGENCY,
                Optional.empty(),
                Optional.of("Trading is halted"),
                true,
                false,
                Optional.empty(),
                Optional.empty(),
                "1".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }
}

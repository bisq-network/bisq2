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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeSelectionService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.TradeDataHeader;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.i18n.Res;
import bisq.presentation.notifications.NotificationsService;
import bisq.settings.SettingsService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyOpenTradesController extends ChatController<BisqEasyOpenTradesView, BisqEasyOpenTradesModel> {
    private final BisqEasyOpenTradeChannelService channelService;
    private final BisqEasyOpenTradeSelectionService selectionService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final SettingsService settingsService;
    private final ReputationService reputationService;
    private final NotificationsService notificationsService;
    private final ChatNotificationService chatNotificationService;
    private TradeStateController tradeStateController;
    private Pin channelItemPin, tradesPin, channelsPin, selectedChannelPin, tradeRulesConfirmedPin;
    private OpenTradesWelcome openTradesWelcome;
    private TradeDataHeader tradeDataHeader;

    public BisqEasyOpenTradesController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OPEN_TRADES, NavigationTarget.BISQ_EASY_OPEN_TRADES);

        channelService = chatService.getBisqEasyOpenTradeChannelService();
        selectionService = chatService.getBisqEasyOpenTradesChannelSelectionService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        settingsService = serviceProvider.getSettingsService();
        reputationService = serviceProvider.getUserService().getReputationService();
        notificationsService = serviceProvider.getNotificationsService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        tradeStateController = new TradeStateController(serviceProvider, this::handleTradeClosed);
        openTradesWelcome = new OpenTradesWelcome();
        tradeDataHeader = new TradeDataHeader(serviceProvider, Res.get("bisqEasy.openTrades.chat.peer.description").toUpperCase());
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }

    @Override
    public BisqEasyOpenTradesModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyOpenTradesModel(chatChannelDomain);
    }

    @Override
    public BisqEasyOpenTradesView createAndGetView() {
        return new BisqEasyOpenTradesView(model,
                this,
                tradeDataHeader.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot(),
                tradeStateController.getView().getRoot(),
                openTradesWelcome.getView().getRoot());
    }

    @Override
    public void onActivate() {
        model.getFilteredList().setPredicate(e -> false);

        tradesPin = bisqEasyTradeService.getTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyTrade trade) {
                channelService.findChannelByTradeId(trade.getId())
                        .ifPresent(channel -> onTradeAndChannelAdded(trade, channel));
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyTrade) {
                    BisqEasyTrade trade = (BisqEasyTrade) element;
                    onTradeRemoved(trade);
                }
            }

            @Override
            public void clear() {
                onClearTrades();
            }
        });

        channelItemPin = channelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOpenTradeChannel channel) {
                bisqEasyTradeService.findTrade(channel.getTradeId())
                        .ifPresent(trade -> onTradeAndChannelAdded(trade, channel));
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel) {
                    BisqEasyOpenTradeChannel channel = (BisqEasyOpenTradeChannel) element;
                    bisqEasyTradeService.findTrade(channel.getTradeId())
                            .ifPresent(trade -> onTradeRemoved(trade));
                }
            }

            @Override
            public void clear() {
                onClearTrades();
            }
        });

        tradeRulesConfirmedPin = settingsService.getTradeRulesConfirmed().addObserver(isConfirmed ->
                UIThread.run(() -> {
                    if (isConfirmed) {
                        model.getTradeRulesAccepted().set(true);
                        maybeSelectFirst();
                        updateVisibility();
                    }
                }));

        bisqEasyTradeService.getTrades().addObserver(() -> {
            UIThread.run(() -> {
                model.getFilteredList()
                        .setPredicate(e -> bisqEasyTradeService.findTrade(e.getTradeId()).isPresent());
                maybeSelectFirst();
                updateVisibility();
            });
        });

        channelsPin = channelService.getChannels().addObserver(this::channelsChanged);

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

        maybeSelectFirst();
        updateVisibility();
    }


    @Override
    public void onDeactivate() {
        tradeRulesConfirmedPin.unbind();
        channelItemPin.unbind();
        tradesPin.unbind();
        selectedChannelPin.unbind();
        channelsPin.unbind();
        doCloseChatWindow();
        model.reset();
        resetSelectedChildTarget();
        selectionService.selectChannel(null);
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getSelectedItem().set(null);
                tradeStateController.setSelectedChannel(null);
                tradeDataHeader.setSelectedChannel(null);
                maybeSelectFirst();
            }

            if (chatChannel instanceof BisqEasyOpenTradeChannel) {
                BisqEasyOpenTradeChannel channel = (BisqEasyOpenTradeChannel) chatChannel;

                applyPeersIcon(channel);
                UserProfile peerUserProfile = ((BisqEasyOpenTradeChannel) chatChannel).getPeer();
                String peerUserName = peerUserProfile.getUserName();

                String shortTradeId = channel.getTradeId().substring(0, 8);
                model.getChatWindowTitle().set(Res.get("bisqEasy.openTrades.chat.window.title",
                        serviceProvider.getConfig().getAppName(), peerUserName, shortTradeId));

                model.getListItems().stream()
                        .filter(item -> item.getChannel().equals(channel))
                        .findAny()
                        .ifPresent(item -> model.getSelectedItem().set(item));

                tradeStateController.setSelectedChannel(channel);
                tradeDataHeader.setSelectedChannel(channel);
            }
        });
    }

    void onSelectItem(BisqEasyOpenTradesView.ListItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else {
            onShowTradeRulesAcceptedWarning();
            BisqEasyOpenTradeChannel channel = item.getChannel();
            if (!channel.equals(selectionService.getSelectedChannel().get())) {
                selectionService.selectChannel(channel);
                updateVisibility();
            }
        }
    }

    void onShowTradeRulesAcceptedWarning() {
        if (!model.getFilteredList().isEmpty() && !settingsService.getTradeRulesConfirmed().get()) {
            new Popup().information(Res.get("bisqEasy.tradeGuide.notConfirmed.warn"))
                    .actionButtonText(Res.get("bisqEasy.tradeGuide.open"))
                    .onAction(() -> Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE))
                    .show();
        }
    }

    void onToggleChatWindow() {
        if (model.getChatWindow().get() == null) {
            model.getChatWindow().set(new Stage());
        } else {
            doCloseChatWindow();
        }
    }

    void onCloseChatWindow() {
        doCloseChatWindow();
    }

    private void doCloseChatWindow() {
        if (model.getChatWindow().get() != null) {
            model.getChatWindow().get().hide();
        }
        model.getChatWindow().set(null);
    }

    private void handleTradeClosed(BisqEasyTrade trade, BisqEasyOpenTradeChannel channel) {
        if (model.getSelectedChannel() != null) {
            bisqEasyTradeService.removeTrade(trade);
            channelService.leaveChannel(channel);
            selectionService.getSelectedChannel().set(null);
        }
    }

    private void onTradeAndChannelAdded(BisqEasyTrade trade, BisqEasyOpenTradeChannel channel) {
        UIThread.run(() -> {
            if (findListItem(trade).isEmpty()) {
                model.getListItems().add(new BisqEasyOpenTradesView.ListItem(channel,
                        trade,
                        reputationService,
                        notificationsService,
                        chatNotificationService));
                maybeSelectFirst();
                updateVisibility();
            }
        });
    }

    private void onTradeRemoved(BisqEasyTrade trade) {
        UIThread.run(() -> {
            Optional<BisqEasyOpenTradesView.ListItem> toRemove = findListItem(trade);
            toRemove.ifPresent(item -> {
                UIThread.run(() -> {
                    item.dispose();
                    model.getListItems().remove(item);
                    maybeSelectFirst();
                    updateVisibility();
                });
            });
        });
    }

    private void onClearTrades() {
        UIThread.run(() -> {
            model.getListItems().forEach(BisqEasyOpenTradesView.ListItem::dispose);
            model.getListItems().clear();
            maybeSelectFirst();
            updateVisibility();
        });
    }

    private void updateVisibility() {
        boolean openTradesAvailable = !model.getFilteredList().isEmpty();
        model.getNoOpenTrades().set(!openTradesAvailable);

        boolean tradeRuleConfirmed = settingsService.getTradeRulesConfirmed().get();
        model.getTradeWelcomeVisible().set(openTradesAvailable && !tradeRuleConfirmed);
        model.getTradeStateVisible().set(openTradesAvailable && tradeRuleConfirmed);
        model.getChatVisible().set(openTradesAvailable && tradeRuleConfirmed);
    }

    private void channelsChanged() {
        UIThread.run(() -> {
            maybeSelectFirst();
            updateVisibility();
        });
    }

    private void maybeSelectFirst() {
        if (selectionService.getSelectedChannel().get() == null &&
                !channelService.getChannels().isEmpty() &&
                !model.getSortedList().isEmpty()) {
            selectionService.getSelectedChannel().set(model.getSortedList().get(0).getChannel());
        }
    }

    private Optional<BisqEasyOpenTradesView.ListItem> findListItem(BisqEasyTrade trade) {
        return model.getListItems().stream()
                .filter(e -> e.getTrade().equals(trade))
                .findAny();
    }
}

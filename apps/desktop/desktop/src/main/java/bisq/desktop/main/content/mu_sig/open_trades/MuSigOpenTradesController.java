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

package bisq.desktop.main.content.mu_sig.open_trades;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.mu_sig.open_trades.trade_state.MuSigTradeDataHeader;
import bisq.desktop.main.content.mu_sig.open_trades.trade_state.MuSigTradeStateController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class MuSigOpenTradesController extends ChatController<MuSigOpenTradesView, MuSigOpenTradesModel> {
    private final MuSigOpenTradeChannelService channelService;
    private final MuSigTradeService tradeService;
    private final SettingsService settingsService;
    private final ReputationService reputationService;
    private final ChatNotificationService chatNotificationService;
    private MuSigTradeStateController muSigTradeStateController;
    private Pin channelsPin, tradesPin, tradeRulesConfirmedPin;
    private MuSigOpenTradesWelcome muSigOpenTradesWelcome;
    private MuSigTradeDataHeader muSigTradeDataHeader;
    private final Map<String, Pin> isInMediationPinMap = new HashMap<>();

    public MuSigOpenTradesController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.MU_SIG_OPEN_TRADES, NavigationTarget.MU_SIG_OPEN_TRADES);

        channelService = chatService.getMuSigOpenTradeChannelService();
        tradeService = serviceProvider.getTradeService().getMuSigTradeService();
        settingsService = serviceProvider.getSettingsService();
        reputationService = serviceProvider.getUserService().getReputationService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        muSigTradeStateController = new MuSigTradeStateController(serviceProvider);
        muSigOpenTradesWelcome = new MuSigOpenTradesWelcome();
        muSigTradeDataHeader = new MuSigTradeDataHeader(serviceProvider, Res.get("bisqEasy.openTrades.chat.peer.description").toUpperCase());
    }

    @Override
    public MuSigOpenTradesModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new MuSigOpenTradesModel(chatChannelDomain);
    }

    @Override
    public MuSigOpenTradesView createAndGetView() {
        return new MuSigOpenTradesView(model,
                this,
                muSigTradeDataHeader.getRoot(),
                chatMessageContainerController.getView().getRoot(),
                channelSidebar.getRoot(),
                muSigTradeStateController.getView().getRoot(),
                muSigOpenTradesWelcome.getView().getRoot());
    }

    @Override
    public void onActivate() {
        model.getFilteredList().setPredicate(e -> false);

        tradesPin = tradeService.getTradeById().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, MuSigTrade trade) {
                handleTradeAdded(trade);
            }

            @Override
            public void remove(Object key) {
                if (key instanceof String tradeId) {
                    //handleTradeRemoved(tradeId);
                }
            }

            @Override
            public void clear() {
                handleTradesCleared();
            }
        });

        channelsPin = channelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void add(MuSigOpenTradeChannel channel) {
                handleChannelAdded(channel);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof MuSigOpenTradeChannel channel) {
                    handleChannelRemoved(channel);
                }
            }

            @Override
            public void clear() {
                handleChannelsCleared();
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

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

        maybeSelectFirst();
        updateVisibility();
    }

    @Override
    public void onDeactivate() {
        tradeRulesConfirmedPin.unbind();
        channelsPin.unbind();
        tradesPin.unbind();
        selectedChannelPin.unbind();
        isInMediationPinMap.values().forEach(Pin::unbind);
        isInMediationPinMap.clear();
        doCloseChatWindow();
        model.reset();
        resetSelectedChildTarget();
        selectionService.selectChannel(null);
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);

        UIThread.run(() -> {
            if (!hasTradeForChannel(chatChannel)) {
                model.getSelectedItem().set(null);
                muSigTradeStateController.setSelectedChannel(null);
                muSigTradeDataHeader.setSelectedChannel(null);
                maybeSelectFirst();
            } else if (chatChannel instanceof MuSigOpenTradeChannel tradeChannel) {
                UserProfile peerUserProfile = tradeChannel.getPeer();
                String peerUserName = peerUserProfile.getUserName();

                String shortTradeId = tradeChannel.getTradeId().substring(0, 8);
                model.getChatWindowTitle().set(Res.get("bisqEasy.openTrades.chat.window.title",
                        serviceProvider.getConfig().getAppName(), peerUserName, shortTradeId));

                model.getListItems().stream()
                        .filter(item -> item.getChannel().getId().equals(tradeChannel.getId()))
                        .findAny()
                        .ifPresent(item -> model.getSelectedItem().set(item));

                muSigTradeStateController.setSelectedChannel(tradeChannel);
                muSigTradeDataHeader.setSelectedChannel(tradeChannel);
            }
        });
    }

    void onSelectItem(MuSigOpenTradeListItem item) {
        if (item == null || !hasTradeForChannel(item.getChannel())) {
            selectionService.selectChannel(null);
        } else {
            onShowTradeRulesAcceptedWarning();
            MuSigOpenTradeChannel channel = item.getChannel();
            selectionService.selectChannel(channel);
            updateVisibility();
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

    // Trade
    private void handleTradeAdded(MuSigTrade trade) {
        channelService.findChannelByTradeId(trade.getId())
                .ifPresentOrElse(channel -> handleTradeAndChannelAdded(trade, channel),
                        () -> log.warn("Trade with id {} was added but associated channel is not found.", trade.getId()));
    }

    private void handleTradeRemoved(MuSigTrade trade) {
        String tradeId = trade.getId();
        channelService.findChannelByTradeId(tradeId)
                .ifPresentOrElse(channel -> handleTradeAndChannelRemoved(trade),
                        () -> {
                            if (findListItem(trade).isEmpty()) {
                                log.warn("Trade with id {} was removed but associated channel and listItem is not found. " +
                                        "We ignore that call.", tradeId);
                            } else {
                                log.warn("Trade with id {} was removed but associated channel is not found but a listItem with that trade is still present." +
                                        "We call handleTradeAndChannelRemoved.", tradeId);
                                handleTradeAndChannelRemoved(trade);
                            }
                        });
    }

    private void handleTradesCleared() {
        handleClearTradesAndChannels();
    }

    // Channel
    private void handleChannelAdded(MuSigOpenTradeChannel channel) {
        tradeService.findTrade(channel.getTradeId())
                .ifPresentOrElse(trade -> handleTradeAndChannelAdded(trade, channel),
                        () -> log.warn("Channel with tradeId {} was added but associated trade is not found.", channel.getTradeId()));
    }

    private void handleChannelRemoved(MuSigOpenTradeChannel channel) {
        String tradeId = channel.getTradeId();
        tradeService.findTrade(tradeId)
                .ifPresentOrElse(this::handleTradeAndChannelRemoved,
                        () -> {
                            Optional<MuSigOpenTradeListItem> listItem = findListItem(tradeId);
                            if (listItem.isEmpty()) {
                                log.debug("Channel with tradeId {} was removed but associated trade and the listItem is not found. " +
                                        "This is expected as we first remove the trade and then the channel.", tradeId);
                            } else {
                                log.warn("Channel with tradeId {} was removed but associated trade is not found but we still have the listItem with that trade. " +
                                        "We call handleTradeAndChannelRemoved.", tradeId);
                                handleTradeAndChannelRemoved(listItem.get().getTrade());
                            }
                        });
    }

    private void handleChannelsCleared() {
        handleClearTradesAndChannels();
    }

    // TradeAndChannel
    private void handleTradeAndChannelAdded(MuSigTrade trade, MuSigOpenTradeChannel channel) {
        UIThread.run(() -> {
            if (findListItem(trade).isPresent()) {
                log.debug("We got called handleTradeAndChannelAdded but we have that trade list item already. " +
                        "This is expected as we get called both when a trade is added and the associated channel.");
                return;
            }

            model.getListItems().add(new MuSigOpenTradeListItem(channel,
                    trade,
                    reputationService,
                    chatNotificationService,
                    userProfileService));

            String tradeId = trade.getId();
            if (isInMediationPinMap.containsKey(tradeId)) {
                isInMediationPinMap.get(tradeId).unbind();
            }
            Pin pin = channel.isInMediationObservable().addObserver(isInMediation -> {
                if (isInMediation != null) {
                    updateIsAnyTradeInMediation();
                }
            });
            isInMediationPinMap.put(tradeId, pin);

            updatePredicate();
            maybeSelectFirst();
            updateVisibility();
        });
    }

    private void handleTradeAndChannelRemoved(MuSigTrade trade) {
        UIThread.run(() -> {
            String tradeId = trade.getId();
            if (findListItem(trade).isEmpty()) {
                log.warn("We got called handleTradeAndChannelRemoved but we have not found any trade list item with tradeId {}", tradeId);
                return;
            }

            MuSigOpenTradeListItem item = findListItem(trade).get();
            item.dispose();
            model.getListItems().remove(item);

            if (isInMediationPinMap.containsKey(tradeId)) {
                isInMediationPinMap.get(tradeId).unbind();
                isInMediationPinMap.remove(trade.getId());
            }
            updateIsAnyTradeInMediation();

            updatePredicate();
            maybeSelectFirst();
            updateVisibility();
        });
    }

    private void handleClearTradesAndChannels() {
        UIThread.run(() -> {
            model.getListItems().forEach(MuSigOpenTradeListItem::dispose);
            model.getListItems().clear();

            isInMediationPinMap.values().forEach(Pin::unbind);
            isInMediationPinMap.clear();
            updateIsAnyTradeInMediation();

            updatePredicate();
            maybeSelectFirst();
            updateVisibility();
        });
    }

    // Misc
    private void updatePredicate() {
        model.getFilteredList().setPredicate(e -> tradeService.findTrade(e.getTradeId()).isPresent());
    }

    private void maybeSelectFirst() {
        if (!hasTradeForChannel(selectionService.getSelectedChannel().get()) &&
                !model.getSortedList().isEmpty()) {
            UIThread.runOnNextRenderFrame(() -> selectionService.selectChannel(model.getSortedList().get(0).getChannel()));
        }
    }

    private void updateVisibility() {
        boolean openTradesAvailable = !model.getFilteredList().isEmpty();
        model.getNoOpenTrades().set(!openTradesAvailable);

        boolean tradeRuleConfirmed = settingsService.getTradeRulesConfirmed().get();
        model.getTradeWelcomeVisible().set(openTradesAvailable && !tradeRuleConfirmed);
        model.getTradeStateVisible().set(openTradesAvailable && tradeRuleConfirmed);
        model.getChatVisible().set(openTradesAvailable && tradeRuleConfirmed);
    }

    private void updateIsAnyTradeInMediation() {
        UIThread.runOnNextRenderFrame(() -> {
            boolean value = channelService.getChannels().stream()
                    .anyMatch(MuSigOpenTradeChannel::isInMediation);
            model.getIsAnyTradeInMediation().set(value);
        });
    }

    private Optional<MuSigOpenTradeListItem> findListItem(MuSigTrade trade) {
        return findListItem(trade.getId());
    }

    private Optional<MuSigOpenTradeListItem> findListItem(String tradeId) {
        return model.getListItems().stream()
                .filter(item -> item.getTrade().getId().equals(tradeId))
                .findAny();
    }

    private boolean hasTradeForChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        return chatChannel instanceof MuSigOpenTradeChannel channel &&
                tradeService.findTrade(channel.getTradeId()).isPresent();
    }
}

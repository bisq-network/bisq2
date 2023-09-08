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
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.SettingsService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.profile.UserProfile;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Optional;

@Slf4j
public class BisqEasyOpenTradesController extends ChatController<BisqEasyOpenTradesView, BisqEasyOpenTradesModel> {
    private final BisqEasyOpenTradesModel bisqEasyOpenTradesModel;
    private final BisqEasyOpenTradeChannelService channelService;
    private final BisqEasyOpenTradeSelectionService selectionService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final SettingsService settingsService;

    private TradeStateController tradeStateController;
    private Pin channelItemPin, selectedChannelPin, tradeRulesConfirmedPin;
    private OpenTradesWelcome openTradesWelcome;

    public BisqEasyOpenTradesController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OPEN_TRADES, NavigationTarget.BISQ_EASY_OPEN_TRADES);

        channelService = chatService.getBisqEasyOpenTradeChannelService();
        selectionService = chatService.getBisqEasyOpenTradesChannelSelectionService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        settingsService = serviceProvider.getSettingsService();
        bisqEasyOpenTradesModel = getModel();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        tradeStateController = new TradeStateController(serviceProvider, this::openUserProfileSidebar);
        openTradesWelcome = new OpenTradesWelcome();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public BisqEasyOpenTradesModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyOpenTradesModel(chatChannelDomain);
    }

    @Override
    public BisqEasyOpenTradesView createAndGetView() {
        return new BisqEasyOpenTradesView(model,
                this,
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot(),
                tradeStateController.getView().getRoot(),
                openTradesWelcome.getView().getRoot());
    }

    @Override
    public void onActivate() {
        model.getFilteredList().setPredicate(e -> false);

        channelItemPin = channelService.getChannels().addListener(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOpenTradeChannel channel) {
                bisqEasyTradeService.findTrade(channel.getTradeId())
                        .ifPresent(trade -> {
                            if (findListItem(trade).isEmpty()) {
                                UIThread.run(() -> {
                                    model.getListItems().add(new BisqEasyOpenTradesView.ListItem(channel, trade));
                                    updateVisibility();
                                });
                            }
                        });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel) {
                    BisqEasyOpenTradeChannel channel = (BisqEasyOpenTradeChannel) element;
                    bisqEasyTradeService.findTrade(channel.getTradeId())
                            .ifPresent(trade -> {
                                Optional<BisqEasyOpenTradesView.ListItem> toRemove = findListItem(trade);
                                toRemove.ifPresent(item -> {
                                    UIThread.run(() -> {
                                        model.getListItems().remove(item);
                                        updateVisibility();
                                    });
                                });
                            });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getListItems().clear();
                    updateVisibility();
                });
            }
        });

        tradeRulesConfirmedPin = settingsService.getTradeRulesConfirmed().addObserver(e -> UIThread.run(this::updateVisibility));

        bisqEasyTradeService.getTrades().addListener(() -> {
            UIThread.run(() -> {
                model.getFilteredList()
                        .setPredicate(e -> bisqEasyTradeService.findTrade(e.getTradeId()).isPresent());
                updateVisibility();
            });
        });

        //todo handle case when no channels are available
        if (selectionService.getSelectedChannel().get() == null
                && !model.getListItems().isEmpty()) {
            selectionService.getSelectedChannel().set(model.getListItems().get(0).getChannel());
        }

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::chatChannelChanged);

        if (!model.getSortedList().isEmpty()) {
            BisqEasyOpenTradesView.ListItem listItem = model.getSortedList().get(0);
            BisqEasyOpenTradeChannel channel = listItem.getChannel();
            selectionService.selectChannel(channel);
            tradeStateController.setSelectedChannel(channel);

            // If there is only one item we do not select it in the tableview
            if (model.getSortedList().size() > 1 && settingsService.getTradeRulesConfirmed().get()) {
                model.getSelectedItem().set(listItem);
            }
        }
        updateVisibility();
    }

    @Nonnull
    private Optional<BisqEasyOpenTradesView.ListItem> findListItem(BisqEasyTrade trade) {
        return model.getListItems().stream()
                .filter(e -> e.getTrade().equals(trade))
                .findAny();
    }


    @Override
    public void onDeactivate() {
        tradeRulesConfirmedPin.unbind();
        channelItemPin.unbind();
        selectedChannelPin.unbind();
        model.getListItems().clear();
        resetSelectedChildTarget();
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof BisqEasyOpenTradeChannel) {
            super.chatChannelChanged(chatChannel);

            UIThread.run(() -> {
                BisqEasyOpenTradeChannel channel = (BisqEasyOpenTradeChannel) chatChannel;
                applyPeersIcon(channel);
                BisqEasyOffer offer = channel.getBisqEasyOffer();
                boolean isMaker = isMaker(offer);
                UserProfile peerUserProfile = ((BisqEasyOpenTradeChannel) chatChannel).getPeer();
                String peerUserName = peerUserProfile.getUserName();
                String title = isMaker ?
                        Res.get("bisqEasy.topPane.privateTradeChannel.maker.title", peerUserName, offer.getShortId()) :
                        Res.get("bisqEasy.topPane.privateTradeChannel.taker.title", peerUserName, offer.getShortId());
                model.getChannelTitle().set(title);
                model.getChatHeadline().set(Res.get("bisqEasy.openTrades.chat.headline"));
                model.getPeerUserProfileDisplay().set(new UserProfileDisplay(peerUserProfile));

                String shortTradeId = channel.getTradeId().substring(0, 8);
                model.getChatWindowTitle().set(Res.get("bisqEasy.openTrades.chat.window.title",
                        serviceProvider.getConfig().getAppName(), peerUserName, shortTradeId));
            });
        }
    }

    void onSelectTrade(String tradeId) {
        channelService.findChannelByOfferId(tradeId).ifPresent(channel -> {
            selectionService.selectChannel(channel);
            tradeStateController.setSelectedChannel(channel);
        });
    }

    void onToggleChatWindow() {
        if (model.getChatWindow().get() == null) {
            model.getChatWindow().set(new Stage());
        } else {
            model.getChatWindow().get().hide();
            onCloseChatWindow();
        }
    }

    void onCloseChatWindow() {
        if (model.getChatWindow().get() != null) {
            model.getChatWindow().get().hide();
        }
        model.getChatWindow().set(null);
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    private void updateVisibility() {
        boolean openTradesAvailable = !model.getFilteredList().isEmpty();
        model.getNoOpenTrades().set(!openTradesAvailable);

        boolean tradeRuleConfirmed = settingsService.getTradeRulesConfirmed().get();
        model.getTradeWelcomeVisible().set(openTradesAvailable && !tradeRuleConfirmed);
        model.getTradeStateVisible().set(openTradesAvailable && tradeRuleConfirmed);
        model.getChatVisible().set(openTradesAvailable && tradeRuleConfirmed);
        model.getTableViewDisabled().set(openTradesAvailable && !tradeRuleConfirmed);
    }
}

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

package bisq.desktop.primary.main.content.chat.channels;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.data.Pair;
import bisq.common.observable.Pin;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.overlay.ComboBoxOverlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyPublicChatChannelSelection extends PublicChatChannelSelection<
        BisqEasyPublicChatChannel,
        BisqEasyPublicChatChannelService,
        BisqEasyChatChannelSelectionService
        > {
    @Getter
    private final Controller controller;

    public BisqEasyPublicChatChannelSelection(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService);
    }

    protected static class Controller extends PublicChatChannelSelection.Controller<
            View,
            Model,
            BisqEasyPublicChatChannel,
            BisqEasyPublicChatChannelService,
            BisqEasyChatChannelSelectionService
            > {

        private Pin numVisibleChannelsPin;

        protected Controller(DefaultApplicationService applicationService) {
            super(applicationService, ChatChannelDomain.BISQ_EASY);
        }

        @Override
        protected BisqEasyPublicChatChannelService createAndGetChatChannelService(ChatChannelDomain chatChannelDomain) {
            return chatService.getBisqEasyPublicChatChannelService();
        }

        @Override
        protected BisqEasyChatChannelSelectionService createAndGetChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
            return chatService.getBisqEasyChatChannelSelectionService();
        }

        @Override
        protected View createAndGetView() {
            return new View(model, this);
        }

        @Override
        protected Model createAndGetModel(ChatChannelDomain chatChannelDomain) {
            return new Model();
        }

        @Override
        protected void updateUnseenMessagesMap(ChatChannel<?> chatChannel) {
            if (chatChannelService.isVisible((BisqEasyPublicChatChannel) chatChannel)) {
                super.updateUnseenMessagesMap(chatChannel);
            }
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.sortedList.setComparator(Comparator.comparing(ChatChannelSelection.View.ChannelItem::getChannelTitle));

            numVisibleChannelsPin = chatChannelService.getNumVisibleChannels().addObserver(n -> applyPredicate());

            List<Market> markets = MarketRepository.getAllFiatMarkets();

            Set<Market> visibleMarkets = model.filteredList.stream()
                    .map(e -> ((BisqEasyPublicChatChannel) e.getChatChannel()))
                    .map(BisqEasyPublicChatChannel::getMarket)
                    .collect(Collectors.toSet());
            markets.removeAll(visibleMarkets);
            List<View.MarketListItem> marketListItems = markets.stream()
                    .map(View.MarketListItem::new)
                    .collect(Collectors.toList());

            model.allMarkets.setAll(marketListItems);
            model.allMarketsSortedList.setComparator((o1, o2) -> Integer.compare(getNumMessages(o2.market), getNumMessages(o1.market)));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            numVisibleChannelsPin.unbind();
        }

        @Override
        protected void onSelected(ChatChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }

            chatChannelSelectionService.selectChannel(channelItem.getChatChannel());
        }

        public void onShowMarket(View.MarketListItem marketListItem) {
            if (marketListItem != null) {
                model.allMarkets.remove(marketListItem);
                chatChannelService.findChannel(marketListItem.market)
                        .ifPresent(channel -> {
                            chatChannelService.showChannel(channel);
                            chatChannelSelectionService.selectChannel(channel);
                        });
            }
        }

        public void onHideTradeChannel(BisqEasyPublicChatChannel channel) {
            Optional<BisqEasyPublicChatMessage> myOpenOffer = channel.getChatMessages().stream()
                    .filter(BisqEasyPublicChatMessage::hasTradeChatOffer)
                    .filter(publicTradeChatMessage -> userIdentityService.isUserIdentityPresent(publicTradeChatMessage.getAuthorUserProfileId()))
                    .findAny();
            if (myOpenOffer.isPresent()) {
                new Popup().warning(Res.get("tradeChat.leaveChannelWhenOffers.popup")).show();
            } else {
                chatChannelService.hideChannel(channel);

                model.allMarkets.add(0, new View.MarketListItem(channel.getMarket()));
                if (!model.sortedList.isEmpty()) {
                    chatChannelSelectionService.selectChannel(model.sortedList.get(0).getChatChannel());
                } else {
                    chatChannelSelectionService.selectChannel(null);
                }
            }
        }

        private int getNumMessages(Market market) {
            return chatChannelService.findChannel(market)
                    .map(e -> e.getChatMessages().size())
                    .orElse(0);
        }

        @Override
        protected void applyPredicate() {
            model.filteredList.setPredicate(item -> {
                checkArgument(item.getChatChannel() instanceof BisqEasyPublicChatChannel,
                        "Channel must be type of PublicTradeChannel");
                BisqEasyPublicChatChannel channel = (BisqEasyPublicChatChannel) item.getChatChannel();
                return chatChannelService.isVisible(channel);
            });
        }
    }

    protected static class Model extends PublicChatChannelSelection.Model {
        private final ObservableList<View.MarketListItem> allMarkets = FXCollections.observableArrayList();
        private final SortedList<View.MarketListItem> allMarketsSortedList = new SortedList<>(allMarkets);

        public Model() {
            super(ChatChannelDomain.BISQ_EASY);
        }
    }

    protected static class View extends PublicChatChannelSelection.View<Model, Controller> {
        protected final Label addChannelIcon;

        protected View(Model model, Controller controller) {
            super(model, controller);

            addChannelIcon = Icons.getIcon(AwesomeIcon.PLUS_SIGN_ALT, "14");
            addChannelIcon.setOpacity(0.5);
            addChannelIcon.setCursor(Cursor.HAND);
            addChannelIcon.setPadding(new Insets(24, 12, 0, 0));
            headerBox.getChildren().add(addChannelIcon);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            addChannelIcon.setOnMouseClicked(e ->
                    new ComboBoxOverlay<>(addChannelIcon,
                            model.allMarketsSortedList,
                            c -> getMarketListCell(),
                            controller::onShowMarket,
                            Res.get("tradeChat.addMarketChannel").toUpperCase(),
                            Res.get("search"),
                            350, 5, 23, 31.5)
                            .show());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            addChannelIcon.setOnMouseClicked(null);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.marketChannels");
        }

        protected ListCell<View.MarketListItem> getMarketListCell() {
            return new ListCell<>() {
                final Label label = new Label();
                final HBox hBox = new HBox();
                final Badge badge = new Badge(hBox);

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    badge.setTooltip(Res.get("social.marketChannels.numMessages"));
                    badge.setPosition(Pos.CENTER_RIGHT);

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(label, Spacer.fillHBox());
                }

                @Override
                protected void updateItem(View.MarketListItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        label.setText(item.toString());
                        int numMessages = controller.getNumMessages(item.market);
                        badge.setText(numMessages > 0 ? String.valueOf(numMessages) : "");
                        setGraphic(badge);
                    } else {
                        setGraphic(null);
                    }
                }
            };
        }


        @Override
        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                final Label removeIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
                final Badge numMessagesBadge;
                final StackPane iconAndBadge = new StackPane();
                final Label label = new Label();
                final HBox hBox = new HBox();
                final ColorAdjust nonSelectedEffect = new ColorAdjust();
                final ColorAdjust hoverEffect = new ColorAdjust();
                @Nullable
                private Subscription widthSubscription;
                @Nullable
                MapChangeListener<String, Integer> channelIdWithNumUnseenMessagesMapListener;

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(50);
                    setPadding(new Insets(0, 0, -20, 0));

                    numMessagesBadge = new Badge();
                    numMessagesBadge.setPosition(Pos.CENTER);

                    iconAndBadge.getChildren().addAll(numMessagesBadge, removeIcon);
                    iconAndBadge.setAlignment(Pos.CENTER);

                    label.getStyleClass().add("bisq-text-8");
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(label, Spacer.fillHBox(), iconAndBadge);

                    HBox.setMargin(iconAndBadge, new Insets(0, 12, 0, 0));

                    removeIcon.setCursor(Cursor.HAND);
                    removeIcon.setId("icon-label-grey");

                    nonSelectedEffect.setSaturation(-0.4);
                    nonSelectedEffect.setBrightness(-0.6);

                    hoverEffect.setSaturation(0.2);
                    hoverEffect.setBrightness(0.2);
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChatChannel() instanceof BisqEasyPublicChatChannel) {
                        BisqEasyPublicChatChannel bisqEasyPublicChatChannel = (BisqEasyPublicChatChannel) item.getChatChannel();
                        Market market = bisqEasyPublicChatChannel.getMarket();
                        Pair<String, String> pair = new Pair<>(market.getBaseCurrencyCode(),
                                market.getQuoteCurrencyCode());

                        Pair<StackPane, List<ImageView>> marketImageCompositionPair = MarketImageComposition.imageBoxForMarket(
                                pair.getFirst().toLowerCase(),
                                pair.getSecond().toLowerCase());
                        StackPane iconPane = marketImageCompositionPair.getFirst();
                        List<ImageView> icons = marketImageCompositionPair.getSecond();
                        label.setGraphic(iconPane);
                        label.setGraphicTextGap(8);
                        label.setText(item.getChannelTitle());
                        widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                            if (w.doubleValue() > 0) {
                                label.setPrefWidth(getWidth() - 60);
                            }
                        });

                        removeIcon.setOpacity(0);
                        removeIcon.setOnMouseClicked(e -> controller.onHideTradeChannel(bisqEasyPublicChatChannel));
                        setOnMouseClicked(e -> Transitions.fadeIn(removeIcon));
                        setOnMouseEntered(e -> {
                            Transitions.fadeIn(removeIcon);
                            Transitions.fadeOut(numMessagesBadge);
                            applyEffect(icons, item.isSelected(), true);
                        });
                        setOnMouseExited(e -> {
                            Transitions.fadeOut(removeIcon);
                            Transitions.fadeIn(numMessagesBadge);
                            applyEffect(icons, item.isSelected(), false);
                        });
                        applyEffect(icons, item.isSelected(), false);

                        channelIdWithNumUnseenMessagesMapListener = change -> onUnseenMessagesChanged(item, change.getKey(), numMessagesBadge);
                        model.channelIdWithNumUnseenMessagesMap.addListener(channelIdWithNumUnseenMessagesMapListener);
                        model.channelIdWithNumUnseenMessagesMap.keySet().forEach(key -> onUnseenMessagesChanged(item, key, numMessagesBadge));

                        setGraphic(hBox);
                    } else {
                        label.setGraphic(null);
                        removeIcon.setOnMouseClicked(null);
                        setOnMouseClicked(null);
                        setOnMouseEntered(null);
                        setOnMouseExited(null);
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                            widthSubscription = null;
                        }
                        if (channelIdWithNumUnseenMessagesMapListener != null) {
                            model.channelIdWithNumUnseenMessagesMap.removeListener(channelIdWithNumUnseenMessagesMapListener);
                            channelIdWithNumUnseenMessagesMapListener = null;
                        }
                        setGraphic(null);
                    }
                }

                private void applyEffect(List<ImageView> icons, boolean isSelected, boolean isHover) {
                    icons.forEach(icon -> {
                        if (isSelected) {
                            icon.setEffect(null);
                        } else {
                            if (isHover) {
                                icon.setEffect(hoverEffect);
                            } else {
                                icon.setEffect(nonSelectedEffect);
                            }
                        }
                    });
                }
            };
        }

        @EqualsAndHashCode
        private static class MarketListItem {
            private final Market market;

            public MarketListItem(Market market) {
                this.market = market;
            }

            @Override
            public String toString() {
                return market.toString();
            }
        }
    }
}
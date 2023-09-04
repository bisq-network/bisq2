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

package bisq.desktop.main.content.chat.channels;

import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.data.Pair;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.ComboBoxOverlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
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
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyPublicChannelSelectionMenu extends PublicChannelSelectionMenu<
        BisqEasyPublicChatChannel,
        BisqEasyPublicChatChannelService,
        BisqEasyChatChannelSelectionService
        > {
    @Getter
    private final Controller controller;

    public BisqEasyPublicChannelSelectionMenu(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    protected static class Controller extends PublicChannelSelectionMenu.Controller<
            View,
            Model,
            BisqEasyPublicChatChannel,
            BisqEasyPublicChatChannelService,
            BisqEasyChatChannelSelectionService
            > {

        private Pin numVisibleChannelsPin;

        protected Controller(ServiceProvider serviceProvider) {
            super(serviceProvider, ChatChannelDomain.BISQ_EASY);
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
        public void onActivate() {
            super.onActivate();

            model.getSortedChannels().setComparator(Comparator.comparing(ChannelSelectionMenu.View.ChannelItem::getChannelTitle));
            numVisibleChannelsPin = chatChannelService.getNumVisibleChannels().addObserver(n -> UIThread.run(this::applyPredicate));

            List<View.MarketChannelItem> marketChannelItems = chatChannelService.getChannels().stream()
                    .map(View.MarketChannelItem::new)
                    .collect(Collectors.toList());
            model.marketChannelItems.setAll(marketChannelItems);

            updateMarketItemsPredicate();

            model.sortedMarketChannelItems.setComparator((o1, o2) -> {
                Comparator<View.MarketChannelItem> byNumMessages = (left, right) -> Integer.compare(
                        getNumMessages(right.getMarket()),
                        getNumMessages(left.getMarket()));

                List<Market> majorMarkets = MarketRepository.getMajorMarkets();
                Comparator<View.MarketChannelItem> byMajorMarkets = (left, right) -> {
                    int indexOfLeftMarket = majorMarkets.indexOf(left.getMarket());
                    int indexOfRightMarket = majorMarkets.indexOf(right.getMarket());
                    if (indexOfLeftMarket > -1 && indexOfRightMarket > -1) {
                        return Integer.compare(indexOfLeftMarket, indexOfRightMarket);
                    } else {
                        return -1;
                    }
                };

                Comparator<View.MarketChannelItem> byName = (left, right) -> left.getMarket().toString().compareTo(right.toString());
                return byNumMessages
                        .thenComparing(byMajorMarkets)
                        .thenComparing(byName)
                        .compare(o1, o2);
            });
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            numVisibleChannelsPin.unbind();
        }

        void onJoinChannel(View.MarketChannelItem marketChannelItem) {
            if (marketChannelItem != null) {
                chatChannelService.findChannel(marketChannelItem.getMarket())
                        .ifPresent(channel -> {
                            chatChannelService.joinChannel(channel);
                            chatChannelSelectionService.selectChannel(channel);
                        });
                updateMarketItemsPredicate();
            }
        }

        void onLeaveChannel(BisqEasyPublicChatChannel channel) {
            Optional<BisqEasyPublicChatMessage> myOpenOffer = channel.getChatMessages().stream()
                    .filter(BisqEasyPublicChatMessage::hasBisqEasyOffer)
                    .filter(publicTradeChatMessage -> publicTradeChatMessage.isMyMessage(userIdentityService))
                    .findAny();
            if (myOpenOffer.isPresent()) {
                new Popup().warning(Res.get("bisqEasy.channelSelection.public.leave.warn")).show();
            } else {
                doLeaveChannel(channel);
                updateMarketItemsPredicate();
            }
        }

        private int getNumMessages(Market market) {
            return chatChannelService.findChannel(market)
                    .map(channel -> channel.getChatMessages().size())
                    .orElse(0);
        }

        @Override
        protected void applyPredicate() {
            model.filteredChannels.setPredicate(item -> {
                checkArgument(item.getChatChannel() instanceof BisqEasyPublicChatChannel,
                        "Channel must be type of PublicTradeChannel");
                BisqEasyPublicChatChannel channel = (BisqEasyPublicChatChannel) item.getChatChannel();
                return chatChannelService.isVisible(channel);
            });
        }

        private void updateMarketItemsPredicate() {
            model.filteredMarketChannelItems.setPredicate(item -> !chatChannelService.isVisible(item.getChannel()));
        }
    }

    @Getter
    protected static class Model extends PublicChannelSelectionMenu.Model {
        private final ObservableList<View.MarketChannelItem> marketChannelItems = FXCollections.observableArrayList();
        private final FilteredList<View.MarketChannelItem> filteredMarketChannelItems = new FilteredList<>(marketChannelItems);
        private final SortedList<View.MarketChannelItem> sortedMarketChannelItems = new SortedList<>(filteredMarketChannelItems);

        public Model() {
            super(ChatChannelDomain.BISQ_EASY);
        }
    }

    protected static class View extends PublicChannelSelectionMenu.View<Model, Controller> {
        protected final Label addChannelIcon;

        protected View(Model model, Controller controller) {
            super(model, controller);

            addChannelIcon = Icons.getIcon(AwesomeIcon.PLUS_SIGN_ALT, "14");
            addChannelIcon.setOpacity(0.5);
            addChannelIcon.setCursor(Cursor.HAND);
            addChannelIcon.setPadding(new Insets(24, 12, 0, 0));
            Tooltip tooltip = new BisqTooltip(Res.get("bisqEasy.channelSelection.public.switchMarketChannel"));
            tooltip.getStyleClass().add("dark-tooltip");
            addChannelIcon.setTooltip(tooltip);
            headerBox.getChildren().add(addChannelIcon);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            addChannelIcon.setOnMouseClicked(e ->
                    new ComboBoxOverlay<>(addChannelIcon,
                            model.sortedMarketChannelItems,
                            c -> getMarketListCell(),
                            controller::onJoinChannel,
                            Res.get("bisqEasy.channelSelection.public.switchMarketChannel").toUpperCase(),
                            Res.get("action.search"),
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
            return Res.get("bisqEasy.channelSelection.public.headline");
        }

        protected ListCell<MarketChannelItem> getMarketListCell() {
            return new ListCell<>() {
                final Label label = new Label();
                final HBox hBox = new HBox();
                final Badge badge = new Badge(hBox);

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    badge.setTooltip(Res.get("bisqEasy.channelSelection.public.numMessages"));
                    badge.setPosition(Pos.CENTER_RIGHT);

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(label, Spacer.fillHBox());
                }

                @Override
                protected void updateItem(MarketChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        label.setText(item.toString());
                        int numMessages = controller.getNumMessages(item.getMarket());
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
                final Label removeChannelIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
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

                    iconAndBadge.getChildren().addAll(numMessagesBadge, removeChannelIcon);
                    iconAndBadge.setAlignment(Pos.CENTER);

                    label.getStyleClass().add("bisq-text-8");
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(label, Spacer.fillHBox(), iconAndBadge);

                    HBox.setMargin(iconAndBadge, new Insets(0, 12, 0, 0));

                    removeChannelIcon.setCursor(Cursor.HAND);
                    removeChannelIcon.setId("icon-label-grey");
                    Tooltip tooltip = new BisqTooltip(Res.get("bisqEasy.channelSelection.public.removeMarketChannel"));
                    tooltip.getStyleClass().add("dark-tooltip");
                    removeChannelIcon.setTooltip(tooltip);

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

                        removeChannelIcon.setOpacity(0);
                        removeChannelIcon.setOnMouseClicked(e -> controller.onLeaveChannel(bisqEasyPublicChatChannel));
                        setOnMouseClicked(e -> Transitions.fadeIn(removeChannelIcon));
                        setOnMouseEntered(e -> {
                            Transitions.fadeIn(removeChannelIcon);
                            Transitions.fadeOut(numMessagesBadge);
                            applyEffect(icons, item.isSelected(), true);
                        });
                        setOnMouseExited(e -> {
                            Transitions.fadeOut(removeChannelIcon);
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
                        removeChannelIcon.setOnMouseClicked(null);
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
        @Getter
        private static class MarketChannelItem {
            private final Market market;
            private final BisqEasyPublicChatChannel channel;

            public MarketChannelItem(BisqEasyPublicChatChannel channel) {
                this.channel = channel;
                market = channel.getMarket();
            }

            @Override
            public String toString() {
                return market.toString();
            }
        }
    }
}
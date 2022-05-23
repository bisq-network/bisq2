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

package bisq.desktop.primary.main.content.components;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.overlay.ComboBoxOverlay;
import bisq.i18n.Res;
import bisq.social.chat.channels.PublicTradeChannel;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PublicTradeChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PublicTradeChannelSelection(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void deSelectChannel() {
        controller.deSelectChannel();
    }

    protected static class Controller extends ChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;

        protected Controller(DefaultApplicationService applicationService) {
            super(applicationService.getChatService());

            model = new Model();
            view = new View(model, this);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.channelItems.setAll(chatService.getPublicTradeChannels().stream()
                    .map(ChannelSelection.View.ChannelItem::new)
                    .collect(Collectors.toList()));

            selectedChannelPin = FxBindings.subscribe(chatService.getSelectedTradeChannel(),
                    channel -> {
                        if (channel instanceof PublicTradeChannel) {
                            model.selectedChannel.set(new ChannelSelection.View.ChannelItem(channel));
                        }
                    });

            List<Market> markets = chatService.getAllMarketsForTradeChannel();

            Set<Market> visibleMarkets = model.filteredList.stream()
                    .map(e -> ((PublicTradeChannel) e.getChannel()))
                    .filter(c -> c.getMarket().isPresent())
                    .map(c -> c.getMarket().get())
                    .collect(Collectors.toSet());
            markets.removeAll(visibleMarkets);
            List<View.MarketListItem> marketListItems = markets.stream()
                    .map(e -> new View.MarketListItem(Optional.of(e)))
                    .collect(Collectors.toList());

            Optional<PublicTradeChannel> anyMarketInVisible = model.filteredList.stream()
                    .map(e -> ((PublicTradeChannel) e.getChannel()))
                    .filter(c -> c.getMarket().isEmpty())
                    .findAny();
            if (anyMarketInVisible.isEmpty()) {
                marketListItems.add(View.MarketListItem.ANY);
            }

            model.allMarkets.setAll(marketListItems);
            model.allMarketsSortedList.setComparator((o1, o2) -> Integer.compare(getNumMessages(o2.market), getNumMessages(o1.market)));
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }

            chatService.selectTradeChannel(channelItem.getChannel());
        }

        public void deSelectChannel() {
            model.selectedChannel.set(null);
        }

        public void onShowMarket(View.MarketListItem marketListItem) {
            if (marketListItem != null) {
                model.allMarkets.remove(marketListItem);
                Optional<PublicTradeChannel> tradeChannel = chatService.showPublicTradeChannel(marketListItem.market);

                //todo somehow the predicate does not trigger an update, no idea why...
                // re-applying the list works
                model.channelItems.clear();
                model.channelItems.setAll(chatService.getPublicTradeChannels().stream()
                        .map(ChannelSelection.View.ChannelItem::new)
                        .collect(Collectors.toList()));

                tradeChannel.ifPresent(chatService::selectTradeChannel);
            }
        }

        public void onHideTradeChannel(PublicTradeChannel channel) {
            chatService.hidePublicTradeChannel(channel);
            channel.setVisible(false);

            //todo somehow the predicate does not trigger an update, no idea why...
            // re-applying the list works
            model.channelItems.clear();
            model.channelItems.setAll(chatService.getPublicTradeChannels().stream()
                    .map(ChannelSelection.View.ChannelItem::new)
                    .collect(Collectors.toList()));

            model.allMarkets.add(0, new View.MarketListItem(channel.getMarket()));
            if (!model.sortedList.isEmpty()) {
                chatService.selectTradeChannel(model.sortedList.get(0).getChannel());
            }
        }

        private int getNumMessages(Optional<Market> market) {
            return chatService.findPublicTradeChannel(PublicTradeChannel.getId(market))
                    .map(e -> e.getChatMessages().size())
                    .orElse(0);
        }
    }

    protected static class Model extends ChannelSelection.Model {
        ObservableList<View.MarketListItem> allMarkets = FXCollections.observableArrayList();
        SortedList<View.MarketListItem> allMarketsSortedList = new SortedList<>(allMarkets);
    }

    protected static class View extends ChannelSelection.View<Model, Controller> {
        private final Label gearsIcon;

        protected View(Model model, Controller controller) {
            super(model, controller);

            gearsIcon = Icons.getIcon(AwesomeIcon.GEARS, "14");
            gearsIcon.setOpacity(0.6);
            gearsIcon.setLayoutY(22);
            gearsIcon.setLayoutX(172);
            gearsIcon.setCursor(Cursor.HAND);

            getRoot().getChildren().add(gearsIcon);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            gearsIcon.setOnMouseClicked(e -> new ComboBoxOverlay<>(gearsIcon,
                    model.allMarketsSortedList,
                    c -> getMarketListCell(),
                    controller::onShowMarket,
                    350, 5, 0).show());
        }

        @Override
        protected void onViewDetached() {
            gearsIcon.setOnMouseClicked(null);
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
                private Subscription widthSubscription;
                final Label removeIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN_ALT, "14");
                final Label label = new Label();
                final HBox hBox = new HBox();

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));

                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.getChildren().addAll(label, Spacer.fillHBox(), removeIcon);

                    removeIcon.setCursor(Cursor.HAND);
                    HBox.setMargin(removeIcon, new Insets(0, 12, 0, 0));
                }

                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChannel() instanceof PublicTradeChannel publicTradeChannel) {
                        label.setText(item.getDisplayString());
                        widthSubscription = EasyBind.subscribe(widthProperty(), w -> {
                            if (w.doubleValue() > 0) {
                                label.setMaxWidth(getWidth() - 70);
                            }
                        });

                        removeIcon.setOpacity(0);
                        removeIcon.setOnMouseClicked(e -> controller.onHideTradeChannel(publicTradeChannel));
                        setOnMouseClicked(e -> Transitions.fadeIn(removeIcon));
                        setOnMouseEntered(e -> Transitions.fadeIn(removeIcon));
                        setOnMouseExited(e -> Transitions.fadeOut(removeIcon));

                        setGraphic(hBox);
                    } else {
                        removeIcon.setOnMouseClicked(null);
                        hBox.setOnMouseClicked(null);
                        hBox.setOnMouseEntered(null);
                        hBox.setOnMouseExited(null);
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                        }
                        setGraphic(null);
                    }
                }
            };
        }

        @EqualsAndHashCode
        private static class MarketListItem {
            public static final MarketListItem ANY = new MarketListItem(Optional.empty());
            private final Optional<Market> market;

            public MarketListItem(Optional<Market> market) {
                this.market = market;
            }

            @Override
            public String toString() {
                return market.map(Market::toString).orElse(Res.get("tradeChat.addMarketChannel.any"));
            }
        }
    }
}
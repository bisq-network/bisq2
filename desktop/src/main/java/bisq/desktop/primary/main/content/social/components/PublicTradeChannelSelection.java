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

package bisq.desktop.primary.main.content.social.components;

import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Market;
import bisq.common.monetary.MarketRepository;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.overlay.ComboBoxOverlay;
import bisq.i18n.Res;
import bisq.social.chat.channels.Channel;
import bisq.social.chat.channels.PublicTradeChannel;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

    protected static class Controller extends bisq.desktop.primary.main.content.social.components.ChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final TradeChannelsChooser tradeChannelsChooser;

        protected Controller(DefaultApplicationService applicationService) {
            super(applicationService.getChatService());

            tradeChannelsChooser = new TradeChannelsChooser(chatService, applicationService.getSettingsService());
            model = new Model();
            view = new View(model, this, tradeChannelsChooser.getRoot());
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        public void onActivate() {
            super.onActivate();
            channelsPin = FxBindings.<PublicTradeChannel, Channel<?>>bind(model.channels)
                    .to(chatService.getPublicTradeChannels());

            selectedChannelPin = FxBindings.subscribe(chatService.getSelectedTradeChannel(),
                    channel -> {
                        if (channel instanceof PublicTradeChannel) {
                            model.selectedChannel.set(channel);
                        }
                    });

            model.allMarkets.setAll(MarketRepository.getAllMarkets());
        }

        @Override
        protected void onSelected(Channel<?> channel) {
            if (channel == null) {
                return;
            }

            chatService.selectTradeChannel(channel);
        }

        public void onOpenMarketsChannelChooser() {
            //  new OverlayWindow(view.getRoot(), tradeChannelsChooser.getRoot()).show();
        }

        public void deSelectChannel() {
            model.selectedChannel.set(null);
        }

        public void addMarket(Market market) {
            if (market != null) {
                PublicTradeChannel tradeChannel = chatService.addPublicTradeChannel(market);
                chatService.selectTradeChannel(tradeChannel);
            }
        }
    }

    protected static class Model extends bisq.desktop.primary.main.content.social.components.ChannelSelection.Model {
        ObservableList<Market> allMarkets = FXCollections.observableArrayList();
    }

    protected static class View extends bisq.desktop.primary.main.content.social.components.ChannelSelection.View<Model, Controller> {
        private final Label plusIcon;
        private final ChangeListener<Number> widthListener;

        protected View(Model model, Controller controller, Pane marketChannelsChooser) {
            super(model, controller);

            plusIcon = Icons.getIcon(AwesomeIcon.PLUS_SIGN_ALT, "14");
            plusIcon.setOpacity(0.6);
            plusIcon.setLayoutY(15);
            plusIcon.setCursor(Cursor.HAND);

            titledPaneContainer.getChildren().add(plusIcon);
            widthListener = (observableValue, oldValue, newValue) -> layout();
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            layout();

            plusIcon.setOnMouseClicked(e -> new ComboBoxOverlay<>(plusIcon,
                    model.allMarkets,
                    c -> getMarketListCell(),
                    controller::addMarket,
                    350, 5, 0).show());
            titledPaneContainer.widthProperty().addListener(widthListener);
        }

        @Override
        protected void onViewDetached() {
            titledPaneContainer.widthProperty().removeListener(widthListener);
            plusIcon.setOnMouseClicked(null);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.marketChannels");
        }

        private void layout() {
            plusIcon.setLayoutX(root.getWidth() - 25);
        }


        protected ListCell<Market> getMarketListCell() {
            return new ListCell<>() {
                final Label label = new Label();
                final HBox hBox = new HBox();

                {
                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.setMouseTransparent(true);
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));
                }

                @Override
                protected void updateItem(Market item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        hBox.getChildren().clear();
                        label.setText(item.toString());
                        hBox.getChildren().add(label);
                        setGraphic(hBox);
                    } else {
                        hBox.getChildren().clear();
                        setGraphic(null);
                    }
                }
            };
        }
    }
}
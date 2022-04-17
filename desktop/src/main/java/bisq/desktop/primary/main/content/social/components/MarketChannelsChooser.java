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

import bisq.common.monetary.Market;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.primary.main.content.social.exchange.Badge;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.social.chat.ChatService;
import bisq.social.chat.MarketChannel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

@Slf4j
public class MarketChannelsChooser {
    private final Controller controller;

    public MarketChannelsChooser(ChatService chatService, SettingsService settingsService) {
        controller = new Controller(chatService, settingsService);
    }

    public ReadOnlyObjectProperty<MarketChannelItem> selectedMarketProperty() {
        return controller.model.selectedMarketChannelItem;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSelectedMarket(MarketChannelItem market) {
        controller.model.selectedMarketChannelItem.set(market);
    }

    public void setCellFactory(Callback<ListView<MarketChannelItem>, ListCell<MarketChannelItem>> value) {
        controller.model.cellFactory = Optional.of(value);
    }

    public void setButtonCell(ListCell<MarketChannelItem> value) {
        controller.model.buttonCell = Optional.of(value);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatService chatService;
        private final Model model;
        @Getter
        private final View view;

        private Controller(ChatService chatService, SettingsService settingsService) {
            this.chatService = chatService;
            model = new Model(settingsService);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.sortedList.setComparator(MarketChannelItem::compareTo);
            FxBindings.<MarketChannel, MarketChannelItem>bind(model.marketChannelItems)
                    .map(MarketChannelItem::new)
                    .to(chatService.getMarketChannels());
            findMarketChannelItem(model.settingsService.getSelectedMarket()).ifPresent(model.selectedMarketChannelItem::set);
        }

        @Override
        public void onDeactivate() {
        }

        private void onSelectMarket(MarketChannelItem selected) {
            if (selected != null) {
                model.selectedMarketChannelItem.set(selected);
                chatService.selectChannel(selected.channel);
            }
        }

        private Optional<MarketChannelItem> findMarketChannelItem(Market market) {
            return model.marketChannelItems.stream().filter(e -> e.getMarket().equals(market)).findAny();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<MarketChannelItem> selectedMarketChannelItem = new SimpleObjectProperty<>();
        private final ObservableList<MarketChannelItem> marketChannelItems = FXCollections.observableArrayList();
        private final SortedList<MarketChannelItem> sortedList = new SortedList<>(marketChannelItems);

        private final SettingsService settingsService;
        private Optional<Callback<ListView<MarketChannelItem>, ListCell<MarketChannelItem>>> cellFactory = Optional.empty();
        private Optional<ListCell<MarketChannelItem>> buttonCell = Optional.empty();

        public Model(SettingsService settingsService) {
            this.settingsService = settingsService;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final AutoCompleteComboBox<MarketChannelItem> comboBox;
        private final ChangeListener<MarketChannelItem> selectedMarketListener;

        private View(Model model, Controller controller) {
            super(new Pane(), model, controller);

            comboBox = new AutoCompleteComboBox<>(model.sortedList, Res.get("social.marketChannels"));
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable MarketChannelItem value) {
                    return value != null ? value.toString() : "";
                }

                @Override
                public MarketChannelItem fromString(String string) {
                    return new MarketChannelItem(new MarketChannel(Market.from(string)));
                }
            });
            comboBox.setCellFactory(getCellFactory());
            root.getChildren().addAll(comboBox);

            // From model
            selectedMarketListener = (o, old, newValue) -> comboBox.getSelectionModel().select(newValue);
        }

        @Override
        protected void onViewAttached() {
            model.cellFactory.ifPresent(comboBox::setCellFactory);
            comboBox.setOnChangeConfirmed(e -> controller.onSelectMarket(comboBox.getSelectionModel().getSelectedItem()));
            model.selectedMarketChannelItem.addListener(selectedMarketListener);
            comboBox.getSelectionModel().select(model.selectedMarketChannelItem.get());
            comboBox.prefWidthProperty().bind(root.widthProperty());
        }

        @Override
        protected void onViewDetached() {
            comboBox.setOnChangeConfirmed(null);
            model.selectedMarketChannelItem.removeListener(selectedMarketListener);
        }

        @NonNull
        private Callback<ListView<MarketChannelsChooser.MarketChannelItem>, ListCell<MarketChannelsChooser.MarketChannelItem>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<MarketChannelsChooser.MarketChannelItem> call(ListView<MarketChannelsChooser.MarketChannelItem> list) {
                    return new ListCell<>() {
                        @Override
                        public void updateItem(final MarketChannelsChooser.MarketChannelItem item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                Label market = new Label(item.toString());
                                HBox hBox = new HBox();
                                hBox.setAlignment(Pos.CENTER_LEFT);
                                hBox.getChildren().addAll(market, Spacer.fillHBox());

                                Badge badge = new Badge(hBox);
                                badge.setTooltip(Res.get("social.marketChannels.numMessages"));
                                badge.setPosition(Pos.CENTER_RIGHT);
                                int numMessages = item.getNumMessages();
                                if (numMessages > 0) {
                                    badge.setText(String.valueOf(numMessages));
                                }
                                setGraphic(badge);
                            } else {
                                setGraphic(null);
                            }
                        }
                    };
                }
            };
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Getter
    public final static class MarketChannelItem extends ChannelListItem<MarketChannel> implements Comparable<MarketChannelItem> {
        private final Market market;
        @EqualsAndHashCode.Exclude
        private int numMessages = new Random().nextInt(100);

        public MarketChannelItem(MarketChannel marketChannel) {
            super(marketChannel);
            this.market = marketChannel.getMarket();
        }

        @Override
        public String toString() {
            return market.toString();
        }

        @Override
        public int compareTo(@NonNull MarketChannelsChooser.MarketChannelItem o) {
            return Integer.compare(o.numMessages, numMessages);
        }
    }
}
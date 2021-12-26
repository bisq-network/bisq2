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

package network.misq.desktop.main.content.offerbook;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.controls.*;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class OfferbookView extends View<VBox, OfferbookModel, OfferbookController> {
    private TableView<OfferListItem> tableView;
    private RangeSliderBox baseAmountSliderBox, priceSliderBox;
    private AutocompleteComboBox<String> askCurrencyComboBox, bidCurrencyComboBox;
    private AutoTooltipButton flipButton;
    private AutoTooltipSlideToggleButton showAmountPriceFilterToggle;
    private HBox amountPriceFilterBox;
    private AutoTooltipButton createOfferButton;

    public OfferbookView(OfferbookModel model, OfferbookController controller) {
        super(new VBox(), model, controller);
    }

    @Override
    public void onAddedToStage() {
        controller.onViewAdded();

        baseAmountSliderBox.onViewAdded();
        // priceSliderBox.onViewAdded();
    }

    @Override
    protected void onRemovedFromStage() {
        controller.onViewRemoved();
        baseAmountSliderBox.onViewRemoved();
    }

    @Override
    protected void setupView() {
        Label askCurrencyLabel = new AutoTooltipLabel("I want (ask):");
        askCurrencyLabel.setPadding(new Insets(4, 8, 0, 0));

        askCurrencyComboBox = new AutocompleteComboBox<>();
        askCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        flipButton = new AutoTooltipButton("<- Flip ->");

        Label bidCurrencyLabel = new AutoTooltipLabel("I give (bid):");
        bidCurrencyLabel.setPadding(new Insets(4, 8, 0, 60));
        bidCurrencyComboBox = new AutocompleteComboBox<>();
        bidCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        HBox.setMargin(flipButton, new Insets(-2, 0, 0, 60));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        createOfferButton = new AutoTooltipButton("Create offer");
        HBox.setMargin(createOfferButton, new Insets(20, 20, 20, 20));

        HBox currencySelectionBox = new HBox();
        currencySelectionBox.setMinHeight(70);
        currencySelectionBox.setMaxHeight(currencySelectionBox.getMinHeight());
        currencySelectionBox.getChildren().addAll(askCurrencyLabel, askCurrencyComboBox, flipButton, bidCurrencyLabel,
                bidCurrencyComboBox, spacer, createOfferButton);

        showAmountPriceFilterToggle = new AutoTooltipSlideToggleButton("Filter by amount and price");
        showAmountPriceFilterToggle.setTextAlignment(TextAlignment.LEFT);
        showAmountPriceFilterToggle.setVisible(false); //todo deactivated as not updated to domain changes

        amountPriceFilterBox = new HBox();
        amountPriceFilterBox.setSpacing(80);
        amountPriceFilterBox.setPadding(new Insets(50, 0, 0, 0));

        baseAmountSliderBox = new RangeSliderBox("Filter by BTC amount", 300, model, controller);
        // priceSliderBox = new RangeSliderBox("Filter by price", 300, model, controller);
        amountPriceFilterBox.getChildren().addAll(baseAmountSliderBox/*, priceSliderBox*/);

        //todo deactivated as not updated to domain changes
        showAmountPriceFilterToggle.managedProperty().bind(showAmountPriceFilterToggle.visibleProperty());
        baseAmountSliderBox.visibleProperty().bind(showAmountPriceFilterToggle.visibleProperty());
        baseAmountSliderBox.managedProperty().bind(showAmountPriceFilterToggle.visibleProperty());
        amountPriceFilterBox.visibleProperty().bind(showAmountPriceFilterToggle.visibleProperty());
        amountPriceFilterBox.managedProperty().bind(showAmountPriceFilterToggle.visibleProperty());

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addPropertyColumn(model.getOfferedAmountHeaderProperty(), OfferListItem::getBidAmountProperty, Optional.of(OfferListItem::compareBidAmount));
        addPropertyColumn(model.getPriceHeaderProperty(), OfferListItem::getQuoteProperty, Optional.of(OfferListItem::compareQuote));
        addPropertyColumn(new SimpleStringProperty("Market price % offset"),
                OfferListItem::getMarketPriceOffsetProperty, Optional.of(OfferListItem::compareMarketPriceOffset));
        addPropertyColumn(model.getAskedAmountHeaderProperty(), OfferListItem::getAskAmountProperty, Optional.of(OfferListItem::compareAskAmount));
        addValueColumn("Details", OfferListItem::getFormattedTransferOptions, Optional.empty());
        addMakerColumn("");
        addTakeOfferColumn("");

        root.getChildren().addAll(currencySelectionBox, showAmountPriceFilterToggle, amountPriceFilterBox, tableView);
    }

    @Override
    protected void configModel() {
        askCurrencyComboBox.setAutocompleteItems(model.getCurrenciesProperty());
        askCurrencyComboBox.getSelectionModel().select(model.getSelectedAskCurrencyProperty().get());

        bidCurrencyComboBox.setAutocompleteItems(model.getCurrenciesProperty());
        bidCurrencyComboBox.getSelectionModel().select(model.getSelectedBidCurrencyProperty().get());

        amountPriceFilterBox.visibleProperty().bind(model.getAmountFilterModel().getVisible());
        showAmountPriceFilterToggle.selectedProperty().bind(model.getAmountFilterModel().getVisible());

        model.getSortedItems().comparatorProperty().bind(tableView.comparatorProperty());
        model.getMarketPriceByCurrencyMapProperty().addListener(observable -> tableView.sort());
        tableView.setItems(model.getSortedItems());
        tableView.sort();
    }

    @Override
    protected void configController() {
        flipButton.setOnAction(e -> {
            controller.onFlipCurrencies();
            String ask = askCurrencyComboBox.getSelectionModel().getSelectedItem();
            String bid = bidCurrencyComboBox.getSelectionModel().getSelectedItem();
            askCurrencyComboBox.getSelectionModel().select(bid);
            bidCurrencyComboBox.getSelectionModel().select(ask);
        });
        askCurrencyComboBox.setOnAction(e -> controller.onSelectAskCurrency(askCurrencyComboBox.getSelectionModel().getSelectedItem()));
        bidCurrencyComboBox.setOnAction(e -> controller.onSelectBidCurrency(bidCurrencyComboBox.getSelectionModel().getSelectedItem()));
        createOfferButton.setOnAction(e -> controller.onCreateOffer());
    }

    private void addMakerColumn(String header) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            final ImageView iconView = new ImageView();
                            final AutoTooltipButton button = new AutoTooltipButton("Show details");

                            {
                                button.setGraphic(iconView);
                                button.setMinWidth(200);
                                button.setMaxWidth(200);
                                button.setGraphicTextGap(10);
                            }

                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    button.setOnAction(e -> controller.onShowMakerDetails(item, localToScene(getLayoutBounds())));
                                    setPadding(new Insets(0, 15, 0, 0));
                                    setGraphic(button);
                                } else {
                                    button.setOnAction(null);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
        tableView.getColumns().add(column);
    }

    private void addTakeOfferColumn(String header) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            final ImageView iconView = new ImageView();
                            final AutoTooltipButton button = new AutoTooltipButton("Take offer");

                            {
                                button.setGraphic(iconView);
                                button.setMinWidth(200);
                                button.setMaxWidth(200);
                                button.setGraphicTextGap(10);
                            }

                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    button.setOnAction(e -> controller.onTakeOffer(item));
                                    setPadding(new Insets(0, 15, 0, 0));
                                    setGraphic(button);
                                } else {
                                    button.setOnAction(null);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
        tableView.getColumns().add(column);
    }

    private void addValueColumn(String header, Function<OfferListItem, String> displayStringSupplier, Optional<Comparator<OfferListItem>> optionalComparator) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    setText(displayStringSupplier.apply(item));
                                } else {
                                    setText("");
                                }
                            }
                        };
                    }
                });
        optionalComparator.ifPresent(comparator -> {
            column.setSortable(true);
            column.setComparator(comparator);
        });
        tableView.getColumns().add(column);
    }

    private void addPropertyColumn(StringProperty header, Function<OfferListItem, StringProperty> valueSupplier,
                                   Optional<Comparator<OfferListItem>> optionalComparator) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            OfferListItem previousItem;

                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    if (previousItem != null) {
                                        previousItem.deactivate();
                                    }
                                    previousItem = item;

                                    item.activate();
                                    textProperty().bind(valueSupplier.apply(item));
                                } else {
                                    if (previousItem != null) {
                                        previousItem.deactivate();
                                        previousItem = null;
                                    }
                                    textProperty().unbind();
                                    setText("");
                                }
                            }
                        };
                    }
                });
        optionalComparator.ifPresent(comparator -> {
            column.setSortable(true);
            column.setComparator(comparator);
        });
        tableView.getColumns().add(column);
    }
}

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

package network.misq.desktop.main.content.settings;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.controls.AutoTooltipTableColumn;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public class SettingsView extends View<VBox, SettingsModel, SettingsController> {
    private TableView<ConnectionListItem> tableView;

    public SettingsView(SettingsModel model, SettingsController controller) {
        super(new VBox(), model, controller);
    }

    @Override
    public void onAddedToStage() {
        controller.onViewAdded();
    }

    @Override
    protected void onRemovedFromStage() {
        controller.onViewRemoved();
    }

    @Override
    protected void setupView() {
        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // addPropertyColumn(model.getOfferedAmountHeaderProperty(), ConnectionListItem::getBidAmountProperty, Optional.of(ConnectionListItem::compareBidAmount));

        root.getChildren().addAll(tableView);
    }

    @Override
    protected void configModel() {
       // tableView.sort();
    }

    @Override
    protected void configController() {
    }

    private void addPropertyColumn(StringProperty header, Function<ConnectionListItem, StringProperty> valueSupplier,
                                   Optional<Comparator<ConnectionListItem>> optionalComparator) {
        AutoTooltipTableColumn<ConnectionListItem, ConnectionListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<ConnectionListItem, ConnectionListItem> call(
                            TableColumn<ConnectionListItem, ConnectionListItem> column) {
                        return new TableCell<>() {
                            ConnectionListItem previousItem;

                            @Override
                            public void updateItem(final ConnectionListItem item, boolean empty) {
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

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

package network.misq.desktop.components.table;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public class MisqTableView<S extends TableItem> extends TableView<S> {
    public MisqTableView(SortedList<S> sortedList) {
        super(sortedList);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sortedList.comparatorProperty().bind(comparatorProperty());
    }


    public MisqTableColumn<S, S> getPropertyColumn(StringProperty header,
                                                   int headerWidth,
                                                   Function<S, StringProperty> valueSupplier,
                                                   Optional<Comparator<S>> optionalComparator) {
        MisqTableColumn<S, S> column = new MisqTableColumn<>(header) {{
            setMinWidth(headerWidth);
        }};
        column.setCellValueFactory((data) -> new ReadOnlyObjectWrapper<>(data.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<S, S> call(TableColumn<S,
                            S> column) {
                        return new TableCell<>() {
                            S previousItem;

                            @Override
                            public void updateItem(final S item, boolean empty) {
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
        return column;
    }
}
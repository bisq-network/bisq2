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

import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import network.misq.desktop.components.controls.MisqLabel;
import network.misq.i18n.Res;

public class MisqTableView<S extends TableItem> extends TableView<S> {
    public MisqTableView(SortedList<S> sortedList) {
        super(sortedList);

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sortedList.comparatorProperty().bind(comparatorProperty());

        setPlaceholder(new MisqLabel(Res.common.get("table.placeholder.noData")));
    }
/*
    public MisqTableColumn<S> getPropertyColumn(StringProperty header,
                                                   Function<S, StringProperty> valueSupplier,
                                                   Optional<Comparator<S>> optionalComparator) {
        return getPropertyColumn(header, Optional.empty(), Optional.empty(), valueSupplier, optionalComparator);
    }

    public MisqTableColumn<S> getPropertyColumn(StringProperty header,
                                                   int minWidth,
                                                   Function<S, StringProperty> valueSupplier,
                                                   Optional<Comparator<S>> optionalComparator) {
        return getPropertyColumn(header, Optional.of(minWidth), Optional.empty(), valueSupplier, optionalComparator);
    }

    public MisqTableColumn<S> getPropertyColumn(StringProperty header,
                                                   int minWidth,
                                                   int maxWidth,
                                                   Function<S, StringProperty> valueSupplier,
                                                   Optional<Comparator<S>> optionalComparator) {
        return getPropertyColumn(header, Optional.of(minWidth), Optional.of(maxWidth), valueSupplier, optionalComparator);
    }

    public MisqTableColumn<S> getPropertyColumn(StringProperty header,
                                                   Optional<Integer> minWidth,
                                                   Optional<Integer> maxWidth,
                                                   Function<S, StringProperty> valueSupplier,
                                                   Optional<Comparator<S>> optionalComparator) {

        MisqTableColumn<S> column1 = new MisqTableColumn<S>()
                .headerProperty(header)
                .header(header.get())
                .minWidth(minWidth.get())
                .maxWidth(maxWidth.get())
                .value("valueSupplier")
                .comparator(optionalComparator.get())
                .valuePropertySupplier(valueSupplier);
        
        MisqTableColumn<S> column = new MisqTableColumn<>(header) {{
            minWidth.ifPresent(this::setMinWidth);
            maxWidth.ifPresent(this::setMaxWidth);
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
    }*/
}
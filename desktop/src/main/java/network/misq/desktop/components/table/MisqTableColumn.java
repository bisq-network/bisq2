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

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.components.controls.MisqLabel;
import network.misq.desktop.components.controls.controlsfx.control.PopOver;
import network.misq.desktop.components.overlay.PopOverWrapper;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class MisqTableColumn<S> extends TableColumn<S, S> {
    private Label helpIcon;
    private final PopOverWrapper popoverWrapper = new PopOverWrapper();
    private final MisqLabel titleLabel = new MisqLabel();
    private Optional<Function<S, String>> valueSupplier = Optional.empty();
    private Optional<Function<S, StringProperty>> valuePropertySupplier = Optional.empty();
    private Optional<Comparator<S>> comparator = Optional.empty();
    private Optional<String> value = Optional.empty();

    public static class Builder<S> {
        private Optional<String> title = Optional.empty();
        private Optional<StringProperty> titleProperty = Optional.empty();
        private Optional<Integer> minWidth = Optional.empty();
        private Optional<Integer> maxWidth = Optional.empty();
        private Optional<String> value = Optional.empty();
        private Optional<Function<S, String>> valueSupplier = Optional.empty();
        private Optional<Function<S, StringProperty>> valuePropertySupplier = Optional.empty();
        private Optional<Comparator<S>> comparator = Optional.empty();

        public MisqTableColumn<S> build() {
            MisqTableColumn<S> tableColumn = new MisqTableColumn<>();
            if (title.isPresent()) {
                tableColumn.applyTitle(title.get());
            } else {
                titleProperty.ifPresent(tableColumn::applyTitleProperty);
            }
            minWidth.ifPresent(tableColumn::setMinWidth);
            maxWidth.ifPresent(tableColumn::setMaxWidth);
            tableColumn.value = value;
            tableColumn.valueSupplier = valueSupplier;
            tableColumn.valuePropertySupplier = valuePropertySupplier;
            tableColumn.comparator = comparator;
            return tableColumn;
        }

        public Builder<S> title(String title) {
            this.title = Optional.of(title);
            return this;
        }

        public Builder<S> titleProperty(StringProperty titleProperty) {
            this.titleProperty = Optional.of(titleProperty);
            return this;
        }

        public Builder<S> minWidth(int minWidth) {
            this.minWidth = Optional.of(minWidth);
            return this;
        }

        public Builder<S> maxWidth(int maxWidth) {
            this.maxWidth = Optional.of(maxWidth);
            return this;
        }

        public Builder<S> valuePropertySupplier(Function<S, StringProperty> valuePropertySupplier) {
            this.valuePropertySupplier = Optional.of(valuePropertySupplier);
            return this;
        }

        public Builder<S> valueSupplier(Function<S, String> valueSupplier) {
            this.valueSupplier = Optional.of(valueSupplier);
            return this;
        }

        public Builder<S> comparator(Comparator<S> comparator) {
            this.comparator = Optional.of(comparator);
            return this;
        }


        public Builder<S> value(String value) {
            this.value = Optional.of(value);
            return this;
        }
    }


    public MisqTableColumn() {
        super();

        setCellValueFactory((data) -> new ReadOnlyObjectWrapper<>(data.getValue()));
        setCellFactory(
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
                                    if (previousItem instanceof TableItem tableItem) {
                                        tableItem.deactivate();
                                    }
                                    previousItem = item;

                                    if (item instanceof TableItem tableItem) {
                                        tableItem.activate();
                                    }
                                    if (value.isPresent()) {
                                        setText(value.get());
                                    } else if (valueSupplier.isPresent()) {
                                        setText(valueSupplier.get().apply(item));
                                    } else
                                        valuePropertySupplier.ifPresent(supplier ->
                                                textProperty().bind(supplier.apply(item)));
                                } else {
                                    if (previousItem != null) {
                                        if (previousItem instanceof TableItem tableItem) {
                                            tableItem.deactivate();
                                        }
                                        previousItem = null;
                                    }
                                    if (valuePropertySupplier.isPresent()) {
                                        textProperty().unbind();
                                    }
                                    setText("");
                                }
                            }
                        };
                    }
                });
        comparator.ifPresent(comparator -> {
            setSortable(true);
            setComparator(comparator);
        });
    }

  /*  public MisqTableColumn(String text) {
        super();

        title(text);
    }

    public MisqTableColumn(StringProperty titleProperty) {
        super();

        titleProperty(titleProperty);
    }

    public MisqTableColumn(String text, String help) {
        setTitleWithHelpText(text, help);
    }
*/

    public void applyTitle(String title) {
        titleLabel.setText(title);
        setGraphic(titleLabel);
    }

    public void applyTitleProperty(StringProperty titleProperty) {
        titleLabel.textProperty().bind(titleProperty);
        setGraphic(titleLabel);
    }


    public void setTitleWithHelpText(String title, String help) {
        helpIcon = new Label();
        AwesomeDude.setIcon(helpIcon, AwesomeIcon.QUESTION_SIGN, "1em");
        helpIcon.setOpacity(0.4);
        helpIcon.setOnMouseEntered(e -> popoverWrapper.showPopOver(() -> createInfoPopOver(help)));
        helpIcon.setOnMouseExited(e -> popoverWrapper.hidePopOver());

        MisqLabel label = new MisqLabel(title);
        HBox hBox = new HBox(label, helpIcon);
        hBox.setStyle("-fx-alignment: center-left");
        hBox.setSpacing(4);
        setGraphic(hBox);
    }

    private PopOver createInfoPopOver(String help) {
        Label helpLabel = new Label(help);
        helpLabel.setMaxWidth(300);
        helpLabel.setWrapText(true);
        return createInfoPopOver(helpLabel);
    }

    private PopOver createInfoPopOver(Node node) {
        node.getStyleClass().add("default-text");

        PopOver infoPopover = new PopOver(node);
        if (helpIcon.getScene() != null) {
            infoPopover.setDetachable(false);
            infoPopover.setArrowLocation(PopOver.ArrowLocation.LEFT_CENTER);

            infoPopover.show(helpIcon, -10);
        }
        return infoPopover;
    }

}

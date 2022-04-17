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

package bisq.desktop.components.table;

import javafx.scene.control.Button;
import bisq.desktop.components.controls.jfx.BisqInputTextField;
import bisq.desktop.components.controls.controlsfx.control.PopOver;
import bisq.desktop.components.overlay.PopOverWrapper;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class BisqTableColumn<S> extends TableColumn<S, S> {
    public enum CellFactory {
        TEXT,
        TEXT_INPUT,
        BUTTON,
        CHECKBOX
    }

    private Label helpIcon;
    private final PopOverWrapper popoverWrapper = new PopOverWrapper();
    private final Label titleLabel = new Label();
    private Optional<Function<S, Boolean>> isVisibleFunction = Optional.empty();
    private Optional<Function<S, String>> valueSupplier = Optional.empty();
    private Optional<Function<S, StringProperty>> valuePropertySupplier = Optional.empty();
    private Optional<Function<S, StringProperty>> valuePropertyBiDirBindingSupplier = Optional.empty();
    private final Optional<Comparator<S>> comparator = Optional.empty();
    private Optional<String> value = Optional.empty();
    private Consumer<S> onActionHandler = item -> {
    };
    private BiConsumer<S, Boolean> onToggleHandler = (item, selected) -> {
    };
    private Optional<Class<? extends Button>> buttonClass = Optional.empty();
    private BiConsumer<S, Button> updateItemWithButtonHandler = (item, button) -> {
    };
    private BiConsumer<S, BisqInputTextField> updateItemWithInputTextFieldHandler = (item, field) -> {
    };

    public static class Builder<S> {
        private Optional<String> title = Optional.empty();
        private Optional<StringProperty> titleProperty = Optional.empty();
        private Optional<Function<S, Boolean>> isVisibleFunction = Optional.empty();
        private Optional<Integer> minWidth = Optional.empty();
        private Optional<Integer> maxWidth = Optional.empty();
        private Optional<String> value = Optional.empty();
        private Optional<Function<S, String>> valueSupplier = Optional.empty();
        private Optional<Function<S, StringProperty>> valuePropertySupplier = Optional.empty();
        private Optional<Function<S, StringProperty>> valuePropertyBiDirBindingSupplier = Optional.empty();
        private Optional<Comparator<S>> comparator = Optional.empty();
        private CellFactory cellFactory = CellFactory.TEXT;
        private Consumer<S> onActionHandler = item -> {
        };
        private BiConsumer<S, Boolean> onToggleHandler = (item, selected) -> {
        };
        private Optional<Class<? extends Button>> buttonClass = Optional.empty();
        private BiConsumer<S, Button> updateItemWithButtonHandler = (item, button) -> {
        };
        private BiConsumer<S, BisqInputTextField> updateItemWithInputTextFieldHandler = (item, field) -> {
        };

        public BisqTableColumn<S> build() {
            BisqTableColumn<S> tableColumn = new BisqTableColumn<>(cellFactory);
            if (title.isPresent()) {
                tableColumn.applyTitle(title.get());
            } else {
                titleProperty.ifPresent(tableColumn::applyTitleProperty);
            }
            tableColumn.isVisibleFunction = isVisibleFunction;
            minWidth.ifPresent(tableColumn::setMinWidth);
            maxWidth.ifPresent(tableColumn::setMaxWidth);
            tableColumn.value = value;
            tableColumn.valueSupplier = valueSupplier;
            tableColumn.valuePropertySupplier = valuePropertySupplier;
            tableColumn.valuePropertyBiDirBindingSupplier = valuePropertyBiDirBindingSupplier;
            tableColumn.onActionHandler = onActionHandler;
            tableColumn.onToggleHandler = onToggleHandler;
            tableColumn.buttonClass = buttonClass;
            tableColumn.updateItemWithButtonHandler = updateItemWithButtonHandler;
            tableColumn.updateItemWithInputTextFieldHandler = updateItemWithInputTextFieldHandler;
            comparator.ifPresent(tableColumn::applyComparator);
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

        public Builder<S> isVisibleFunction(Function<S, Boolean> isVisibleFunction) {
            this.isVisibleFunction = Optional.of(isVisibleFunction);
            return this;
        }

        public Builder<S> minWidth(int minWidth) {
            this.minWidth = Optional.of(minWidth);
            return this;
        }

        public Builder<S> fixWidth(int fixWidth) {
            this.minWidth = Optional.of(fixWidth);
            this.maxWidth = Optional.of(fixWidth);
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

        public Builder<S> valuePropertyBiDirBindingSupplier(Function<S, StringProperty> stringProperty) {
            this.valuePropertyBiDirBindingSupplier = Optional.of(stringProperty);
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

        public Builder<S> cellFactory(CellFactory cellFactory) {
            this.cellFactory = cellFactory;
            return this;
        }

        public Builder<S> actionHandler(Consumer<S> actionHandler) {
            this.onActionHandler = actionHandler;
            return this;
        }

        public Builder<S> updateItemWithButtonHandler(BiConsumer<S, Button> handler) {
            this.updateItemWithButtonHandler = handler;
            return this;
        }

        public Builder<S> updateItemWithInputTextFieldHandler(BiConsumer<S, BisqInputTextField> handler) {
            this.updateItemWithInputTextFieldHandler = handler;
            return this;
        }

        public Builder<S> buttonClass(Class<? extends Button> buttonClass) {
            this.buttonClass = Optional.of(buttonClass);
            return this;
        }

        public Builder<S> toggleHandler(BiConsumer<S, Boolean> onToggleHandler) {
            this.onToggleHandler = onToggleHandler;
            return this;
        }
    }

    public void applyComparator(Comparator<S> comparator) {
        setSortable(true);
        setComparator(comparator);
    }

    public BisqTableColumn(CellFactory cellFactory) {
        super();

        setCellValueFactory((data) -> new ReadOnlyObjectWrapper<>(data.getValue()));
        switch (cellFactory) {
            case TEXT -> applyTextCellFactory();
            case TEXT_INPUT -> applyTextInputCellFactory();
            case BUTTON -> applyButtonCellFactory();
            case CHECKBOX -> applyCheckBoxCellFactory();
        }
    }

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

        Label label = new Label(title);
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

    public void applyTextCellFactory() {
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
                                    } else if (valuePropertySupplier.isPresent()) {
                                        valuePropertySupplier.ifPresent(supplier ->
                                                textProperty().bind(supplier.apply(item)));
                                    } else if (valuePropertyBiDirBindingSupplier.isPresent()) {
                                        valuePropertyBiDirBindingSupplier.ifPresent(supplier ->
                                                textProperty().bindBidirectional(supplier.apply(item)));
                                    }
                                } else {
                                    if (previousItem != null) {
                                        if (previousItem instanceof TableItem tableItem) {
                                            tableItem.deactivate();
                                        }
                                        previousItem = null;
                                    }
                                    valuePropertySupplier.ifPresent(supplier -> textProperty().unbind());
                                    valuePropertyBiDirBindingSupplier.ifPresent(supplier -> textProperty().unbindBidirectional(supplier));

                                    setText("");
                                }
                            }
                        };
                    }
                });
    }

    public void applyTextInputCellFactory() {
        setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<S, S> call(TableColumn<S,
                            S> column) {
                        return new TableCell<>() {
                            S previousItem;

                            private final BisqInputTextField inputTextField = new BisqInputTextField();

                            @Override
                            public void updateItem(final S item, boolean empty) {
                                super.updateItem(item, empty);
                                updateItemWithInputTextFieldHandler.accept(item, inputTextField);

                                if (item != null && !empty) {
                                    isVisibleFunction.ifPresent(function -> inputTextField.setVisible(function.apply(item)));
                                    setGraphic(inputTextField);

                                    if (previousItem instanceof TableItem tableItem) {
                                        tableItem.deactivate();
                                    }
                                    previousItem = item;

                                    if (item instanceof TableItem tableItem) {
                                        tableItem.activate();
                                    }
                                    if (value.isPresent()) {
                                        inputTextField.setText(value.get());
                                    } else if (valueSupplier.isPresent()) {
                                        inputTextField.setText(valueSupplier.get().apply(item));
                                    } else if (valuePropertySupplier.isPresent()) {
                                        valuePropertySupplier.ifPresent(supplier ->
                                                inputTextField.textProperty().bind(supplier.apply(item)));
                                    } else if (valuePropertyBiDirBindingSupplier.isPresent()) {
                                        valuePropertyBiDirBindingSupplier.ifPresent(supplier ->
                                                inputTextField.textProperty().bindBidirectional(supplier.apply(item)));
                                    }
                                } else {
                                    if (previousItem != null) {
                                        if (previousItem instanceof TableItem tableItem) {
                                            tableItem.deactivate();
                                        }
                                        previousItem = null;
                                    }
                                    valuePropertySupplier.ifPresent(supplier -> textProperty().unbind());
                                    valuePropertyBiDirBindingSupplier.ifPresent(supplier -> textProperty().unbindBidirectional(supplier));

                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
    }

    public void applyButtonCellFactory() {
        setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<S, S> call(TableColumn<S,
                            S> column) {
                        return new TableCell<>() {
                            S previousItem;

                            private Button button;

                            {
                                try {
                                    button = buttonClass.orElse(Button.class).getDeclaredConstructor().newInstance();
                                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void updateItem(final S item, boolean empty) {
                                super.updateItem(item, empty);
                                updateItemWithButtonHandler.accept(item, button);

                                if (item != null && !empty) {
                                    button.setOnAction(event -> onActionHandler.accept(item));
                                    isVisibleFunction.ifPresent(function -> button.setVisible(function.apply(item)));
                                    setGraphic(button);

                                    if (previousItem instanceof TableItem tableItem) {
                                        tableItem.deactivate();
                                    }
                                    previousItem = item;

                                    if (item instanceof TableItem tableItem) {
                                        tableItem.activate();
                                    }
                                    if (value.isPresent()) {
                                        button.setText(value.get());
                                    } else if (valueSupplier.isPresent()) {
                                        button.setText(valueSupplier.get().apply(item));
                                    } else if (valuePropertySupplier.isPresent()) {
                                        valuePropertySupplier.ifPresent(supplier ->
                                                button.textProperty().bind(supplier.apply(item)));
                                    } else if (valuePropertyBiDirBindingSupplier.isPresent()) {
                                        valuePropertyBiDirBindingSupplier.ifPresent(supplier ->
                                                button.textProperty().bindBidirectional(supplier.apply(item)));
                                    }
                                } else {
                                    if (previousItem != null) {
                                        if (previousItem instanceof TableItem tableItem) {
                                            tableItem.deactivate();
                                        }
                                        previousItem = null;
                                    }
                                    valuePropertySupplier.ifPresent(supplier -> textProperty().unbind());
                                    valuePropertyBiDirBindingSupplier.ifPresent(supplier -> textProperty().unbindBidirectional(supplier));

                                    button.setOnAction(null);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
    }

    public void applyCheckBoxCellFactory() {
        setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<S, S> call(TableColumn<S,
                            S> column) {
                        return new TableCell<>() {
                            S previousItem;
                            private final CheckBox checkBox = new CheckBox();

                            @Override
                            public void updateItem(final S item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    checkBox.setOnAction(event -> onToggleHandler.accept(item, checkBox.isSelected()));
                                    isVisibleFunction.ifPresent(function -> checkBox.setVisible(function.apply(item)));
                                    setGraphic(checkBox);
                                    if (previousItem instanceof TableItem tableItem) {
                                        tableItem.deactivate();
                                    }
                                    previousItem = item;

                                    if (item instanceof TableItem tableItem) {
                                        tableItem.activate();
                                    }
                                    if (value.isPresent()) {
                                        checkBox.setText(value.get());
                                    } else if (valueSupplier.isPresent()) {
                                        checkBox.setText(valueSupplier.get().apply(item));
                                    } else if (valuePropertySupplier.isPresent()) {
                                        valuePropertySupplier.ifPresent(supplier ->
                                                checkBox.textProperty().bind(supplier.apply(item)));
                                    } else if (valuePropertyBiDirBindingSupplier.isPresent()) {
                                        valuePropertyBiDirBindingSupplier.ifPresent(supplier ->
                                                checkBox.textProperty().bindBidirectional(supplier.apply(item)));
                                    }
                                } else {
                                    if (previousItem != null) {
                                        if (previousItem instanceof TableItem tableItem) {
                                            tableItem.deactivate();
                                        }
                                        previousItem = null;
                                    }
                                    valuePropertySupplier.ifPresent(supplier -> textProperty().unbind());
                                    valuePropertyBiDirBindingSupplier.ifPresent(supplier -> textProperty().unbindBidirectional(supplier));

                                    checkBox.setOnAction(null);
                                    checkBox.setSelected(false);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
    }
}

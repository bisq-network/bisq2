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

package bisq.desktop.components.controls;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class BisqComboBox<T> {
    private final Controller<T> controller;

    public BisqComboBox() {
        controller = new Controller<T>();
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setItems(ObservableList<T> items) {
        controller.model.items.setAll(items);
    }

    public void setDescription(String description) {
        controller.model.description.set(description);
    }

    public void setPrefWidth(double prefWidth) {
        controller.model.prefWidth.set(prefWidth);
    }

    public void setPrompt(String prompt) {
        controller.model.prompt.set(prompt);
    }

    public void setVisibleRowCount(int visibleRowCount) {
        controller.model.visibleRowCount.set(visibleRowCount);
    }

    public final void setConverter(StringConverter<T> stringConverter) {
        controller.model.stringConverter = Optional.ofNullable(stringConverter);
    }

    public final void setOnAction(Runnable onActionHandler) {
        controller.model.onActionHandler = Optional.ofNullable(onActionHandler);
    }

    public T getSelectedItem() {
        return controller.model.selectedItem.get();
    }

    public void selectItem(T selectItem) {
        controller.selectItem(selectItem);
    }

    private static class Controller<T> implements bisq.desktop.common.view.Controller {
        private final Model<T> model;
        @Getter
        private final View<T> view;
        private UIScheduler time;
        private Subscription selectedItemSub;

        private Controller() {
            model = new Model<>();
            view = new View<>(model, this);
        }


        @Override
        public void onActivate() {
            model.text.addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    // log.error("ChangeListener {}", newValue);
                    onTextChanged(newValue);
                }
            });
            selectedItemSub = EasyBind.subscribe(model.selectedItem, selectItem -> {
                if (selectItem != null) {
                    model.text.set(getConvertedText(model.selectedItem.get()));
                }
            });
        }

        @Override
        public void onDeactivate() {
            selectedItemSub.unsubscribe();
        }

        private void selectItem(T selectItem) {
            model.selectedItem.set(selectItem);
        }

        // View events
        private void onFocusChange(boolean hasFocus) {
            // Hack to deal with quick switches of focus on//off due opening of popup (popup gets focus, 
            // then we give it back to text input...). So we ignore the repeated focus changes and only take the last.
            if (time != null) {
                time.stop();
            }
            model.hasFocus = hasFocus;
            time = UIScheduler.run(() -> {
                if (!hasFocus && model.showListView.get()) {
                    model.showListView.set(false);
                }
            }).after(50);
        }

        private void onTextChanged(String text) {
            //  log.error("model.hasFocus ={}, applyFilter={}", model.hasFocus, model.useFilter);
            if (model.hasFocus && model.useFilter && text != null) {
                boolean wasListViewVisible = model.showListView.get();
                if (!wasListViewVisible) {
                    model.text.set("");
                    model.selectedItem.set(null);
                }
                model.showListView.set(true);
                model.filteredItems.setPredicate(item -> text.isEmpty() ||
                        getConvertedText(item).toLowerCase().contains(text.toLowerCase()));
            }
        }

        private void onToggleListView() {
            doToggleListView();
        }

        private void onSelectItem(T selectItem) {
            // log.error("onSelectItem selectItem={} / model={}", selectItem, model.selectedItem.get());
            model.useFilter = false;
            model.selectedItem.set(selectItem);
            model.useFilter = true;
        }

        private void doToggleListView() {
            boolean wasListViewVisible = model.showListView.get();
            if (!wasListViewVisible) {
                model.text.set("");
                model.selectedItem.set(null);
            }
            model.showListView.set(!wasListViewVisible);
        }

        private String getConvertedText(T item) {
            return item != null ? model.stringConverter
                    .map(e -> e.toString(item))
                    .orElse(item.toString()) : "";
        }
    }

    private static class Model<T> implements bisq.desktop.common.view.Model {
        private boolean useFilter;
        private boolean hasFocus;
        private final BooleanProperty showListView = new SimpleBooleanProperty();
        private final DoubleProperty prefWidth = new SimpleDoubleProperty(300);
        private final StringProperty description = new SimpleStringProperty("");
        private final StringProperty prompt = new SimpleStringProperty("");
        private final StringProperty text = new SimpleStringProperty();
        private final ObservableList<T> items = FXCollections.observableArrayList();
        private final SortedList<T> sortedList = new SortedList<>(items);
        private final FilteredList<T> filteredItems = new FilteredList<>(sortedList);
        private final IntegerProperty visibleRowCount = new SimpleIntegerProperty();
        private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();

        private Optional<StringConverter<T>> stringConverter = Optional.empty();
        private Optional<Runnable> onActionHandler = Optional.empty();

        private Model() {
        }
    }

    public static class View<T> extends bisq.desktop.common.view.View<Pane, Model<T>, Controller<T>> {
        private final ChangeListener<Boolean> focusListener;
        private final TextInputBox textInputBox;
        private ComboBoxList<T> comboBoxList;
        private Subscription subscription;

        private View(Model<T> model, Controller<T> controller) {
            super(new Pane(), model, controller);

            textInputBox = new TextInputBox(model.description.get(), model.prompt.get());
            double prefWidth = model.prefWidth.get();
            textInputBox.setPrefWidth(prefWidth);
            root.setPrefWidth(prefWidth);

            ImageView arrow = ImageUtil.getImageViewById("arrow-down");
            arrow.setLayoutX(prefWidth - 22);
            arrow.setLayoutY(22);
            arrow.setMouseTransparent(true);

            root.getChildren().addAll(textInputBox, arrow);

            focusListener = (o, oldValue, newValue) -> {
                controller.onFocusChange(newValue);
            };
        }

        @Override
        protected void onViewAttached() {
            textInputBox.setOnMousePressedHandler(e -> controller.onToggleListView());
            subscription = EasyBind.subscribe(model.showListView, visible -> {
                if (visible) {
                    if (comboBoxList != null) {
                        comboBoxList.close();
                    }
                    comboBoxList = new ComboBoxList<>(root,
                            model.filteredItems,
                            controller::onSelectItem);
                    comboBoxList.show();
                } else if (comboBoxList != null) {
                    comboBoxList.close();
                    comboBoxList = null;
                }
            });
            textInputBox.textProperty().bindBidirectional(model.text);
            textInputBox.promptTextProperty().bind(model.prompt);
            textInputBox.descriptionTextProperty().bind(model.description);
            textInputBox.inputTextFieldFocusedProperty().addListener(focusListener);
            textInputBox.getInputTextField().setOnKeyPressed(e -> {
                if (comboBoxList != null) {
                    if (e.getCode() == KeyCode.DOWN) {
                        comboBoxList.selectNext();
                    } else if (e.getCode() == KeyCode.UP) {
                        comboBoxList.selectPrevious();
                    }
                }
            });
        }

        @Override
        protected void onViewDetached() {
            textInputBox.setOnMousePressedHandler(null);
            subscription.unsubscribe();
            textInputBox.textProperty().unbindBidirectional(model.text);
            textInputBox.promptTextProperty().unbind();
            textInputBox.descriptionTextProperty().unbind();
            textInputBox.inputTextFieldFocusedProperty().removeListener(focusListener);
            textInputBox.getInputTextField().setOnKeyPressed(null);
        }
    }

    @Slf4j
    public static class ComboBoxList<T> extends Pane {
        private final Region owner;
        private final Scene rootScene;
        private final ObservableList<T> items;
        private Window window;
        private Scene scene;
        @Getter
        private final ListView<T> listView;
        private final ListChangeListener<T> numItemsListener;
        private final Polygon background = new Polygon();
        @Setter
        private int maxRows = 10;
        private final Stage stage = new Stage();
        private final ChangeListener<Number> positionListener;
        private UIScheduler fixPositionsScheduler;
        private double x, y, listOffset, width, arrowX_l, arrowX_m, arrowY_m, arrowX_r;

        public ComboBoxList(Region owner, ObservableList<T> items, Consumer<T> selectionHandler) {
            this.owner = owner;
            rootScene = owner.getScene();
            this.items = items;

            // On Linux the owner stage does not move the child stage as it does on Mac
            // So we need to apply centerPopup. Further, with fast movements the handler loses
            // the latest position, with a delay it fixes that.
            // Also, on Mac sometimes the popups are positioned outside the main app, so keep it for all OS
            positionListener = (observable, oldValue, newValue) -> {
                updatePosition();
                if (fixPositionsScheduler != null) {
                    fixPositionsScheduler.stop();
                }
                fixPositionsScheduler = UIScheduler.run(this::updatePosition).after(300);
            };

            setupStage();
            setupKeyHandler();

            setStyle("-fx-background-color: transparent");

            setupBackground();
            setPrefWidth(width + 50);

            listView = new ListView<>();
            listView.setLayoutX(x);
            listView.setLayoutY(y + listOffset);
            listView.setItems(items);
            listView.setPrefWidth(width);
            listView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<T>() {
                @Override
                public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                    log.error("newValue " + newValue);
                    selectionHandler.accept(newValue);
                }
            });
            numItemsListener = c -> updateHeight();
            items.addListener(numItemsListener);
            getChildren().addAll(background, listView);
            // getChildren().addAll(listView);

            updatePosition();
            UIThread.runOnNextRenderFrame(this::updateHeight);
        }

        public void show() {
            stage.show();  // The new stage receives the focus. But we want to give focus back to search field input
            window.requestFocus();
        }

        public void close() {
            stage.hide();
            cleanup();
        }

        public T getSelectItem() {
            return listView.getSelectionModel().getSelectedItem();
        }

        public void selectNext() {
            log.error("selectNext");
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                log.error("selectNext 0");
                listView.getSelectionModel().select(0);
            } else if (selectedIndex < items.size() - 1) {
                listView.getSelectionModel().selectNext();
                int currentIndex = selectedIndex + 1;
                if (currentIndex >= maxRows) {
                    listView.scrollTo(currentIndex);
                }
            }
        }

        public void selectPrevious() {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                listView.getSelectionModel().selectPrevious();
                int currentIndex = selectedIndex - 1;
                if (currentIndex <= maxRows) {
                    listView.scrollTo(currentIndex);
                }
            }
        }

        private void setupStage() {
            scene = new Scene(this);
            scene.getStylesheets().setAll(rootScene.getStylesheets());
            scene.setFill(Color.TRANSPARENT);

            stage.setScene(scene);
            stage.initOwner(owner.getScene().getWindow());
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setOnCloseRequest(event -> {
                event.consume();
                close();
            });
            stage.sizeToScene();
            stage.setWidth(width + 50);

            window = rootScene.getWindow();
            window.xProperty().addListener(positionListener);
            window.yProperty().addListener(positionListener);
            window.widthProperty().addListener(positionListener);
        }

        private void setupBackground() {
            x = 25;
            y = 25;
            listOffset = 8;
            width = owner.getWidth() - 10;
            // relative to visible top-left point 
            arrowX_l = 22;
            arrowX_m = 31.5;
            arrowY_m = 17.5;
            arrowX_r = 41;

            // We use a 25px offset for dropshadow
            DropShadow dropShadow = new DropShadow();
            dropShadow.setBlurType(BlurType.GAUSSIAN);
            dropShadow.setHeight(25);
            dropShadow.setWidth(25);
            dropShadow.setRadius(25);
            dropShadow.setSpread(0.5);
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
            background.setFill(Paint.valueOf("#212121"));
            background.setEffect(dropShadow);
        }

        private void updateHeight() {
            double height = 0;
            if (items.isEmpty()) {
                background.getPoints().clear();
                stage.setHeight(height);
            } else {
                height = Math.min(maxRows, items.size()) * 40 + listOffset;
                background.getPoints().setAll(
                        x, y,
                        x + arrowX_l, y,
                        x + arrowX_m, arrowY_m,
                        x + arrowX_r, y,
                        x + width, y,
                        x + width, y + height,
                        x, y + height);
                stage.setHeight(height + 50);
            }
            listView.setPrefHeight(height - listOffset);
        }

        private void updatePosition() {
            stage.setX(owner.localToScreen(owner.getBoundsInLocal()).getMinX() - 20);
            stage.setY(owner.localToScreen(owner.getBoundsInLocal()).getMaxY());
        }

        private void cleanup() {
            if (fixPositionsScheduler != null) {
                fixPositionsScheduler.stop();
            }
            items.removeListener(numItemsListener);
            window.xProperty().removeListener(positionListener);
            window.yProperty().removeListener(positionListener);
            window.widthProperty().removeListener(positionListener);
        }

        private void setupKeyHandler() {
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    close();
                }
            });

            owner.getScene().setOnKeyPressed(e -> {
                log.error("e.getCode() " + e.getCode());
                close();
            });
        }
    }
}
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

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.lang.ref.WeakReference;

@Slf4j
public class UserProfileSelection {
    private final Controller controller;

    public UserProfileSelection(UserIdentityService userIdentityService) {
        controller = new Controller(userIdentityService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setIsLeftAligned(boolean isLeftAligned) {
        controller.model.isLeftAligned.set(isLeftAligned);
    }

    public void setMaxComboBoxWidth(int width) {
        controller.view.setMaxComboBoxWidth(width);
    }

    public void setConverter(StringConverter<ListItem> value) {
        controller.view.setConverter(value);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserIdentityService userIdentityService;
        private Pin selectedUserProfilePin;
        private Pin userProfilesPin;

        private Controller(UserIdentityService userIdentityService) {
            this.userIdentityService = userIdentityService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentity(),
                    userProfile -> model.selectedUserProfile.set(new ListItem(userProfile)));
            userProfilesPin = FxBindings.<UserIdentity, ListItem>bind(model.userProfiles)
                    .map(ListItem::new)
                    .to(userIdentityService.getUserIdentities());
        }

        @Override
        public void onDeactivate() {
            // Need to clear list otherwise we get issues with binding when multiple 
            // instances are used.
            model.userProfiles.clear();

            selectedUserProfilePin.unbind();
            userProfilesPin.unbind();
        }

        private void onSelected(ListItem selectedItem) {
            if (selectedItem != null) {
                userIdentityService.selectChatUserIdentity(selectedItem.userIdentity);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ListItem> selectedUserProfile = new SimpleObjectProperty<>();
        private final ObservableList<ListItem> userProfiles = FXCollections.observableArrayList();
        private final DoubleProperty comboBoxWidth = new SimpleDoubleProperty();
        private final BooleanProperty isLeftAligned = new SimpleBooleanProperty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final UserProfileComboBox comboBox;
        private Subscription selectedUserProfilePin, isLeftAlignedPin, comboBoxWidthPin;

        private View(Model model, Controller controller) {
            super(new Pane(), model, controller);

            comboBox = new UserProfileComboBox(model.userProfiles, Res.get("social.userProfile.comboBox.description"));
            comboBox.setLayoutY(UserProfileComboBox.Y_OFFSET);
            root.getChildren().setAll(comboBox);
        }

        @Override
        protected void onViewAttached() {
            comboBox.setOnChangeConfirmed(e -> controller.onSelected(comboBox.getSelectionModel().getSelectedItem()));
            selectedUserProfilePin = EasyBind.subscribe(model.selectedUserProfile,
                    selected -> UIThread.runOnNextRenderFrame(() -> comboBox.getSelectionModel().select(selected)));
            isLeftAlignedPin = EasyBind.subscribe(model.isLeftAligned, comboBox::setIsLeftAligned);
            comboBoxWidthPin = EasyBind.subscribe(model.comboBoxWidth, w -> comboBox.setComboBoxWidth(w.doubleValue()));
        }

        @Override
        protected void onViewDetached() {
            comboBox.prefWidthProperty().unbind();
            selectedUserProfilePin.unsubscribe();
            isLeftAlignedPin.unsubscribe();
            comboBoxWidthPin.unsubscribe();
        }

        public void setMaxComboBoxWidth(int width) {
            comboBox.setComboBoxWidth(width);
        }

        public void setConverter(StringConverter<ListItem> value) {
            comboBox.setConverter(value);
        }
    }

    @EqualsAndHashCode
    @Getter
    public static class ListItem {
        private final UserIdentity userIdentity;

        private ListItem(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
        }

        @Override
        public String toString() {
            return userIdentity.getUserName();
        }
    }

    private static class UserProfileComboBox extends AutoCompleteComboBox<ListItem> {
        private final static int DEFAULT_COMBO_BOX_WIDTH = 200;
        private final static int Y_OFFSET = 30;

        private boolean isLeftAligned;

        public UserProfileComboBox(ObservableList<ListItem> items, String description) {
            super(items, description);

            setPrefWidth(DEFAULT_COMBO_BOX_WIDTH);

            setCellFactory(param -> new ListCell<>() {
                private ChangeListener<Number> labelWidthListener;
                private final ImageView imageView;
                private final Label label;
                private final HBox hBox;

                {
                    label = new Label();
                    label.setMouseTransparent(true);

                    imageView = new ImageView();
                    imageView.setFitWidth(UserProfileSkin.ICON_SIZE);
                    imageView.setFitHeight(UserProfileSkin.ICON_SIZE);
                    setPrefHeight(50);
                    setPadding(new Insets(10, 0, 0, 10));

                    hBox = new HBox(10);
                    hBox.setPadding(new Insets(0, 10, 0, 10));
                    if (isLeftAligned) {
                        hBox.setAlignment(Pos.CENTER_RIGHT);
                        hBox.getChildren().addAll(label, imageView);
                    } else {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        hBox.getChildren().addAll(imageView, label);
                    }
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        imageView.setImage(RoboHash.getImage(item.userIdentity.getPubKeyHash()));
                        label.setText(item.userIdentity.getUserName());

                        labelWidthListener = (observable, oldValue, newValue) -> {
                            if (newValue.doubleValue() > 0) {
                                UserProfileComboBox.this.setComboBoxWidth(calculateWidth(label));
                                label.widthProperty().removeListener(labelWidthListener);
                            }
                        };
                        label.widthProperty().addListener(labelWidthListener);

                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                        if (labelWidthListener != null) {
                            label.widthProperty().removeListener(labelWidthListener);
                        }

                    }
                }
            });
        }

        private void setIsLeftAligned(boolean isLeftAligned) {
            this.isLeftAligned = isLeftAligned;
            ((UserProfileSkin) skin).setIsLeftAligned(isLeftAligned);
        }

        private void setComboBoxWidth(double width) {
            if (width == 0) {
                width = DEFAULT_COMBO_BOX_WIDTH;
            }
            setPrefWidth(width);
            ((UserProfileSkin) skin).setComboBoxWidth(width);
        }

        private void setMaxComboBoxWidth(double width) {
            if (width == 0) {
                width = DEFAULT_COMBO_BOX_WIDTH;
            }
            setPrefWidth(width);
            ((UserProfileSkin) skin).setMaxComboBoxWidth(width);
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            if (skin == null) {
                skin = new UserProfileSkin(this, description, prompt);
                editor = skin.getMaterialTextField().getField();
            }
            return skin;
        }

        private double calculateWidth(Label label) {
            double width = label.getWidth() + UserProfileSkin.ICON_SIZE + 80;
            return Math.max(width, UserProfileComboBox.this.getPrefWidth());
        }
    }

    private static class UserProfileSkin extends AutoCompleteComboBox.Skin<ListItem> {
        private final static int ICON_SIZE = 30;
        private final static int ICON_PADDING = 17;
        private final static int ARROW_WIDTH = 10;
        private final static int ARROW_ICON_PADDING = 10;
        private final static int TEXT_PADDING = 6;
        private final Label label;
        private final ImageView imageView;
        private final ChangeListener<Number> userNameLabelWidthListener;
        private boolean isLeftAligned;

        public UserProfileSkin(ComboBox<ListItem> control, String description, String prompt) {
            super(control, description, prompt);

            int offset = 5;
            arrowX_l = DEFAULT_ARROW_X_L - offset;
            arrowX_m = DEFAULT_ARROW_X_M - offset;
            arrowX_r = DEFAULT_ARROW_X_R - offset;

            imageView = new ImageView();
            imageView.setFitWidth(ICON_SIZE);
            imageView.setFitHeight(ICON_SIZE);
            imageView.setLayoutY(7);

            arrow.setLayoutY(19);
            label = new Label();
            label.getStyleClass().add("bisq-text-19");
            label.setLayoutY(14);
            buttonPane.getChildren().setAll(label, arrow, imageView);
            buttonPane.setCursor(Cursor.HAND);
            buttonPane.setLayoutY(-UserProfileComboBox.Y_OFFSET);

            control.getSelectionModel().selectedItemProperty().addListener(new WeakReference<>((ChangeListener<ListItem>) (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    UserIdentity userIdentity = newValue.userIdentity;
                    if (userIdentity != null) {
                        imageView.setImage(RoboHash.getImage(userIdentity.getPubKeyHash()));
                        label.setText(control.getConverter().toString(newValue));
                        buttonPane.layout();
                    }
                }
            }).get());

            UserProfileComboBox userProfileComboBox = (UserProfileComboBox) control;
            userNameLabelWidthListener = new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    if (newValue.doubleValue() > 0) {
                        userProfileComboBox.setComboBoxWidth(userProfileComboBox.calculateWidth(label));
                        label.widthProperty().removeListener(userNameLabelWidthListener);
                    }
                }
            };
            label.widthProperty().addListener(userNameLabelWidthListener);
        }

        private void setIsLeftAligned(boolean isLeftAligned) {
            this.isLeftAligned = isLeftAligned;
        }

        private void setComboBoxWidth(double width) {
            buttonPane.setPrefWidth(width);
        }

        private void setMaxComboBoxWidth(double width) {
            setComboBoxWidth(width);
            label.setMaxWidth(width - UserProfileSkin.ICON_SIZE - 80);
        }

        @Override
        protected void layoutChildren(final double x, final double y, final double w, final double h) {
            if (isLeftAligned) {
                double offset = comboBox.getWidth() - 5;
                arrowX_l = offset - DEFAULT_ARROW_X_L;
                arrowX_m = offset - DEFAULT_ARROW_X_M;
                arrowX_r = offset - DEFAULT_ARROW_X_R;
            }
            super.layoutChildren(x, y, w, h);

            if (isLeftAligned) {
                if (label.getWidth() > 0) {
                    double iconX = buttonPane.getPrefWidth() - ICON_PADDING - ICON_SIZE;
                    imageView.setX(iconX);
                    double arrowX = iconX - ARROW_ICON_PADDING - ARROW_WIDTH;
                    arrow.setLayoutX(arrowX);
                    label.setLayoutX(arrowX - TEXT_PADDING - label.getWidth());
                }
            } else {
                if (label.getWidth() > 0) {
                    imageView.setX(ICON_PADDING);
                    arrow.setLayoutX(ICON_PADDING + ICON_SIZE + ARROW_ICON_PADDING);
                    label.setLayoutX(ICON_PADDING + ICON_SIZE + ARROW_ICON_PADDING + ARROW_WIDTH + TEXT_PADDING);
                }
            }
        }

        @Override
        protected int getRowHeight() {
            return 50;
        }

        @Override
        public Node getDisplayNode() {
            return null;
        }
    }
}
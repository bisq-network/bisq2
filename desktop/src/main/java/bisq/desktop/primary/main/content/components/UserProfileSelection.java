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

    public void setComboBoxWidth(double width) {
        controller.model.comboBoxWidth.set(width);
    }

    public void setIsLeftAligned(boolean isLeftAligned) {
        controller.model.isLeftAligned.set(isLeftAligned);
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
            selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserProfile(),
                    userProfile -> model.selectedUserProfile.set(new ListItem(userProfile)));
            userProfilesPin = FxBindings.<UserIdentity, ListItem>bind(model.userProfiles)
                    .map(ListItem::new)
                    .to(userIdentityService.getUserIdentities());
        }

        @Override
        public void onDeactivate() {
            if (selectedUserProfilePin != null) {
                selectedUserProfilePin.unbind();
            }
            userProfilesPin.unbind();
        }

        @Override
        public boolean useCaching() {
            return false;
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

            root.setMinHeight(60);

            comboBox = new UserProfileComboBox(model.userProfiles, Res.get("social.userProfile.comboBox.description"));
            comboBox.setLayoutY(UserProfileComboBox.Y_OFFSET);
            root.getChildren().addAll(comboBox);
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
            comboBox.setOnChangeConfirmed(null);
            selectedUserProfilePin.unsubscribe();
            isLeftAlignedPin.unsubscribe();
            comboBoxWidthPin.unsubscribe();
        }
    }

    @EqualsAndHashCode
    public static class ListItem {
        private final UserIdentity userIdentity;

        private ListItem(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
        }

        @Override
        public String toString() {
            return userIdentity.getNickName();
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
                    label.getStyleClass().add("bisq-input-box-text-input");

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
                        hBox.getChildren().addAll(imageView, label);
                    }
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        imageView.setImage(RoboHash.getImage(item.userIdentity.getPubKeyHash()));
                        label.setText(item.userIdentity.getNickName());

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

        @Override
        protected Skin<?> createDefaultSkin() {
            if (skin == null) {
                skin = new UserProfileSkin(this, description, prompt);
                editor = skin.getTextInputBox().getInputTextField();
            }
            return skin;
        }

        private double calculateWidth(Label label) {
            double width = label.getWidth() + UserProfileSkin.ICON_SIZE + 70;
            return Math.max(width, UserProfileComboBox.this.getPrefWidth());
        }

    }

    private static class UserProfileSkin extends AutoCompleteComboBox.Skin<ListItem> {
        private final static int ICON_SIZE = 30;
        private final static int ICON_PADDING = 17;
        private final static int ARROW_WIDTH = 10;
        private final static int ARROW_ICON_PADDING = 10;
        private final static int TEXT_PADDING = 6;
        private final Label userNameLabel;
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

            arrow.setLayoutY(23);
            userNameLabel = new Label();
            userNameLabel.setId("user-name-label");
            userNameLabel.setLayoutY(12);
            buttonPane.getChildren().setAll(userNameLabel, arrow, imageView);
            buttonPane.setCursor(Cursor.HAND);
            buttonPane.setLayoutY(-UserProfileComboBox.Y_OFFSET);

            control.getSelectionModel().selectedItemProperty().addListener(new WeakReference<>((ChangeListener<ListItem>) (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    UserIdentity userIdentity = newValue.userIdentity;
                    if (userIdentity != null) {
                        imageView.setImage(RoboHash.getImage(userIdentity.getPubKeyHash()));
                        userNameLabel.setText(userIdentity.getNickName());
                        buttonPane.layout();
                    }
                }
            }).get());

            UserProfileComboBox userProfileComboBox = (UserProfileComboBox) control;
            userNameLabelWidthListener = new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    if (newValue.doubleValue() > 0) {
                        userProfileComboBox.setComboBoxWidth(userProfileComboBox.calculateWidth(userNameLabel));
                        userNameLabel.widthProperty().removeListener(userNameLabelWidthListener);
                    }
                }
            };
            userNameLabel.widthProperty().addListener(userNameLabelWidthListener);
        }

        private void setIsLeftAligned(boolean isLeftAligned) {
            this.isLeftAligned = isLeftAligned;
        }

        private void setComboBoxWidth(double width) {
            buttonPane.setPrefWidth(width);
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
                if (userNameLabel.getWidth() > 0) {
                    double iconX = buttonPane.getPrefWidth() - ICON_PADDING - ICON_SIZE;
                    imageView.setX(iconX);
                    double arrowX = iconX - ARROW_ICON_PADDING - ARROW_WIDTH;
                    arrow.setLayoutX(arrowX);
                    userNameLabel.setLayoutX(arrowX - TEXT_PADDING - userNameLabel.getWidth());
                }
            } else {
                if (userNameLabel.getWidth() > 0) {
                    imageView.setX(ICON_PADDING);
                    arrow.setLayoutX(ICON_PADDING + ICON_SIZE + ARROW_ICON_PADDING);
                    userNameLabel.setLayoutX(ICON_PADDING + ICON_SIZE + ARROW_ICON_PADDING + ARROW_WIDTH + TEXT_PADDING);
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
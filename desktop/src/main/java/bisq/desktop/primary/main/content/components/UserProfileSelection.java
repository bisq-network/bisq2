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
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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

    public UserProfileSelection(ChatUserService chatUserService) {
        controller = new Controller(chatUserService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatUserService chatUserService;
        private Pin selectedUserProfilePin;
        private Pin userProfilesPin;

        private Controller(ChatUserService chatUserService) {
            this.chatUserService = chatUserService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(chatUserService.getSelectedUserProfile(),
                    userProfile -> model.selectedUserProfile.set(new ListItem(userProfile)));
            userProfilesPin = FxBindings.<ChatUserIdentity, ListItem>bind(model.userProfiles)
                    .map(ListItem::new)
                    .to(chatUserService.getUserProfiles());
        }

        @Override
        public void onDeactivate() {
            if (selectedUserProfilePin != null) {
                selectedUserProfilePin.unbind();
            }
            userProfilesPin.unbind();
        }

        private void onSelected(ListItem selectedItem) {
            if (selectedItem != null) {
                chatUserService.selectUserProfile(selectedItem.chatUserIdentity);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ListItem> selectedUserProfile = new SimpleObjectProperty<>();
        private final ObservableList<ListItem> userProfiles = FXCollections.observableArrayList();

        private Model() {
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final UserProfileComboBox comboBox;
        private Subscription subscription;

        private View(Model model, Controller controller) {
            super(new Pane(), model, controller);

            comboBox = new UserProfileComboBox(model.userProfiles, Res.get("social.userProfile.comboBox.description"));
            root.getChildren().addAll(comboBox);
        }

        @Override
        protected void onViewAttached() {
            comboBox.prefWidthProperty().bind(root.widthProperty());
            comboBox.setOnChangeConfirmed(e -> controller.onSelected(comboBox.getSelectionModel().getSelectedItem()));
            subscription = EasyBind.subscribe(model.selectedUserProfile,
                    selected -> UIThread.runOnNextRenderFrame(() -> comboBox.getSelectionModel().select(selected)));
        }

        @Override
        protected void onViewDetached() {
            comboBox.prefWidthProperty().unbind();
            comboBox.setOnChangeConfirmed(null);
            subscription.unsubscribe();
        }
    }

    @EqualsAndHashCode
    public static class ListItem {
        private final ChatUserIdentity chatUserIdentity;

        private ListItem(ChatUserIdentity chatUserIdentity) {
            this.chatUserIdentity = chatUserIdentity;
        }

        @Override
        public String toString() {
            return chatUserIdentity.getNickName();
        }
    }

    private static class UserProfileComboBox extends AutoCompleteComboBox<ListItem> {
        public UserProfileComboBox(ObservableList<ListItem> items, String description) {
            super(items, description);

            setCellFactory(param -> new ListCell<>() {
                private final ImageView imageView;
                private final Label label;
                private final HBox hBox;

                {
                    label = new Label();
                    label.setMouseTransparent(true);
                    label.getStyleClass().add("bisq-input-box-text-input");
                    HBox.setMargin(label, new Insets(14, 0, 0, 10));

                    imageView = new ImageView();
                    imageView.setFitWidth(50);
                    imageView.setFitHeight(50);
                    setStyle("-fx-pref-height: 50; -fx-padding: 0 0 0 0;");

                    hBox = new HBox();
                    hBox.getChildren().addAll(imageView, label);
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        imageView.setImage(RoboHash.getImage(item.chatUserIdentity.getIdentity().proofOfWork()));
                        label.setText(item.chatUserIdentity.getNickName());
                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                    }
                }
            });
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            if (skin == null) {
                skin = new UserProfileSkin(this, description, prompt);
                editor = skin.getTextInputBox().getInputTextField();
            }
            return skin;
        }
    }

    private static class UserProfileSkin extends AutoCompleteComboBox.Skin<ListItem> {
        private final Label userNameLabel;

        public UserProfileSkin(ComboBox<ListItem> control, String description, String prompt) {
            super(control, description, prompt);

            ImageView imageView = new ImageView();
            imageView.setFitWidth(35);
            imageView.setFitHeight(35);
            imageView.setLayoutY(7);

            arrow.setLayoutY(23);
            userNameLabel = new Label();
            userNameLabel.setId("user-name-label");
            userNameLabel.setLayoutY(12);
            buttonPane.getChildren().clear();
            buttonPane.getChildren().addAll(userNameLabel, arrow, imageView);
            buttonPane.setCursor(Cursor.HAND);

            control.getSelectionModel().selectedItemProperty().addListener(new WeakReference<>(
                    (ChangeListener<ListItem>) (observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            ChatUserIdentity chatUserIdentity = newValue.chatUserIdentity;
                            if (chatUserIdentity != null) {
                                imageView.setImage(RoboHash.getImage(chatUserIdentity.getIdentity().proofOfWork()));
                                userNameLabel.setText(chatUserIdentity.getNickName());
                                buttonPane.layout();
                                //  Tooltip.install(buttonPane, new Tooltip(chatUserIdentity.getTooltipString()));
                            }
                        }
                    }).get());

        /*    buttonPane.setOnMouseEntered(e -> {
                userNameLabel.setVisible(true);
                arrow.setVisible(true);
            });
            buttonPane.setOnMouseExited(e -> {
                userNameLabel.setVisible(false);
                arrow.setVisible(false);
            });
            userNameLabel.setVisible(false);
            arrow.setVisible(false);*/

            //  UIThread.runOnNextRenderFrame(()-> userNameLabel.layout());
        }

        @Override
        protected void layoutChildren(final double x, final double y,
                                      final double w, final double h) {
            super.layoutChildren(x, y, w, h);

            if (userNameLabel.getWidth() > 0) {
                userNameLabel.setLayoutX(-userNameLabel.getWidth() - 35);
                arrow.setLayoutX(-25);
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
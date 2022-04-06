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

package bisq.desktop.primary.main.content.social.chat.components;

import bisq.common.data.ByteArray;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.layout.Layout;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileComboBox {
    private final Controller controller;

    public UserProfileComboBox(UserProfileService userProfileService) {
        controller = new Controller(userProfileService);
    }

    public ComboBox<ListItem> getComboBox() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private Pin selectedUserProfilePin;
        private Pin userProfilesPin;

        private Controller(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(userProfileService.getPersistableStore().getSelectedUserProfile(),
                    userProfile -> model.selectedUserProfile.set(new ListItem(userProfile)));
            userProfilesPin = FxBindings.<UserProfile, ListItem>bind(model.userProfiles)
                    .map(ListItem::new)
                    .to(userProfileService.getPersistableStore().getUserProfiles());
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
                userProfileService.selectUserProfile(selectedItem.userProfile);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<ListItem> selectedUserProfile = new SimpleObjectProperty<>();
        ObservableList<ListItem> userProfiles = FXCollections.observableArrayList();

        private Model() {
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<BisqComboBox<ListItem>, Model, Controller> {
        private Subscription subscription;

        private View(Model model, Controller controller) {
            super(new BisqComboBox<>(model.userProfiles), model, controller);

            root.setButtonCell(getListCell());
            root.setCellFactory(param -> getListCell());
        }

        @Override
        protected void onViewAttached() {
            root.setOnAction(e -> controller.onSelected(root.getSelectionModel().getSelectedItem()));
            subscription = EasyBind.subscribe(model.selectedUserProfile,
                    selected -> UIThread.runOnNextRenderFrame(() -> root.getSelectionModel().select(selected)));
        }

        @Override
        protected void onViewDetached() {
            root.setOnAction(null);
            subscription.unsubscribe();
        }

        private ListCell<ListItem> getListCell() {
            return new ListCell<>() {
                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        BisqLabel userName = new BisqLabel(item.userName);
                        ImageView roboIconImageView = new ImageView(item.roboHashNode);
                        userName.setPadding(new Insets(4, 0, 0, 0));
                        roboIconImageView.setFitWidth(50);
                        roboIconImageView.setFitHeight(50);
                        setGraphic(Layout.hBoxWith(roboIconImageView, userName));
                    } else {
                        setGraphic(null);
                    }
                }
            };
        }
    }

    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem {
        private final UserProfile userProfile;
        @EqualsAndHashCode.Include
        private final String userName;
        private final Image roboHashNode;

        private ListItem(UserProfile userProfile) {
            this.userProfile = userProfile;
            userName = userProfile.identity().domainId();

            roboHashNode = RoboHash.getImage(new ByteArray(userProfile.chatUser().getPubKeyHash()));
        }
    }
}
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

package bisq.desktop.primary.main.content.social.components;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.stream.Collectors;

@Slf4j
public class UserProfileDisplay {
    private final Controller controller;

    public UserProfileDisplay(UserProfileService userProfileService) {
        controller = new Controller(userProfileService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private Pin pin;

        private Controller(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
            pin = FxBindings.subscribe(userProfileService.getPersistableStore().getSelectedUserProfile(),
                    userProfile -> {
                        model.userName.set(userProfile.identity().domainId());
                        model.id.set(Res.get("social.createUserProfile.id", userProfile.identity().id()));
                        String entitledRoles = userProfile.entitlements().stream().map(e -> Res.get(e.entitlementType().name())).collect(Collectors.joining(", "));
                        model.entitlements.set(Res.get("social.createUserProfile.entitledRoles", entitledRoles));
                        model.entitlementsVisible.set(!userProfile.entitlements().isEmpty());
                        model.roboHashNode.set(RoboHash.getImage(userProfile.identity().pubKeyHashAsByteArray(), false));
                    });
        }

        @Override
        public void onViewDetached() {
            pin.unbind();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        StringProperty userName = new SimpleStringProperty();
        StringProperty id = new SimpleStringProperty();
        BooleanProperty entitlementsVisible = new SimpleBooleanProperty();
        StringProperty entitlements = new SimpleStringProperty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final BisqLabel userName, id, entitlements;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);

            userName = new BisqLabel();
            userName.getStyleClass().add("headline-label");
            userName.setPadding(new Insets(10, 0, 0, 0));

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(75);
            roboIconImageView.setFitHeight(75);

            id = new BisqLabel();
            id.getStyleClass().add("offer-label-small"); //todo
            id.setPadding(new Insets(-5, 0, 0, 0));

            entitlements = new BisqLabel();
            entitlements.getStyleClass().add("offer-label-small"); //todo
            entitlements.setPadding(new Insets(-5, 0, 0, 0));

            root.getChildren().addAll(userName, roboIconImageView, id, entitlements);
        }

        @Override
        public void onViewAttached() {
            userName.textProperty().bind(model.userName);
            id.textProperty().bind(model.id);
            entitlements.textProperty().bind(model.entitlements);
            entitlements.visibleProperty().bind(model.entitlementsVisible);
            entitlements.managedProperty().bind(model.entitlementsVisible);

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbind();
            id.textProperty().unbind();
            entitlements.textProperty().unbind();
            entitlements.visibleProperty().unbind();
            entitlements.managedProperty().unbind();
            roboHashNodeSubscription.unsubscribe();
        }
    }
}
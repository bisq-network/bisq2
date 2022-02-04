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
import bisq.desktop.robohash.RoboHash;
import bisq.social.userprofile.UserProfileService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileDisplay {
    private final Controller controller;

    public UserProfileDisplay(UserProfileService userProfileService) {
        controller = new Controller(userProfileService);
    }

    public View getView() {
        return controller.view;
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
                        model.roboHashNode.set(RoboHash.getSmall(userProfile.identity().pubKeyHash(), false));
                    });
        }

        @Override
        public void onViewDetached() {
            pin.unbind();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        ObjectProperty<Node> roboHashNode = new SimpleObjectProperty<>();
        StringProperty userName = new SimpleStringProperty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Pane roboIconPane;
        private final BisqLabel userName;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);

            userName = new BisqLabel();
            userName.getStyleClass().add("headline-label");
            userName.setPadding(new Insets(10,0,0,0));
            roboIconPane = new HBox();
            root.getChildren().addAll(userName, roboIconPane);
        }

        @Override
        public void onViewAttached() {
            userName.textProperty().bind(model.userName);

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon == null) {
                    roboIconPane.getChildren().clear();
                } else {
                    roboIconPane.getChildren().setAll(roboIcon);
                }
            });
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbindBidirectional(model.userName);
            roboHashNodeSubscription.unsubscribe();
        }
    }
}
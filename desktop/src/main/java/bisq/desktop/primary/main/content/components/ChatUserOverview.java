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

import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.robohash.RoboHash;
import bisq.user.profile.PublicUserProfile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class ChatUserOverview implements Comparable<ChatUserOverview> {
    private final Controller controller;

    public ChatUserOverview(PublicUserProfile publicUserProfile) {
        this(publicUserProfile, false);
    }

    public ChatUserOverview(PublicUserProfile publicUserProfile, boolean ignored) {
        controller = new Controller(publicUserProfile);
        controller.model.ignored = ignored;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public boolean isIgnored() {
        return controller.model.ignored;
    }

    public PublicUserProfile getChatUser() {
        return controller.model.publicUserProfile;
    }

    @Override
    public int compareTo(ChatUserOverview o) {
        return controller.model.publicUserProfile.getNym().compareTo(o.controller.model.publicUserProfile.getNym());
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(PublicUserProfile publicUserProfile) {
            model = new Model(publicUserProfile);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            PublicUserProfile publicUserProfile = model.publicUserProfile;
            if (publicUserProfile == null) {
                return;
            }

            model.id.set(publicUserProfile.getId());
            model.userName.set(publicUserProfile.getUserName());
            model.roboHashNode.set(RoboHash.getImage(publicUserProfile.getPubKeyHash()));
        }

        @Override
        public void onDeactivate() {
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final PublicUserProfile publicUserProfile;
        private boolean ignored;
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty userName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();

        private Model(PublicUserProfile publicUserProfile) {
            this.publicUserProfile = publicUserProfile;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final ImageView roboIcon;
        private final Label userName, id;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);

            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);

            userName = new Label();
            userName.setMaxWidth(100);
            Tooltip.install(userName, new Tooltip(model.publicUserProfile.getTooltipString()));

            roboIcon = new ImageView();
            roboIcon.setFitWidth(37.5);
            roboIcon.setFitHeight(37.5);
            Tooltip.install(roboIcon, new Tooltip(model.publicUserProfile.getTooltipString()));

            HBox hBox = Layout.hBoxWith(roboIcon, userName);
            hBox.setAlignment(Pos.CENTER_LEFT);

            ImageView trust = new ImageView();
            trust.setId("trust");
            trust.setX(20);
            trust.setY(20);

            StackPane icons = new StackPane();
            icons.getChildren().addAll(roboIcon, trust);

            root.getChildren().addAll(icons, userName);

            // todo add tooltip overlay for id and entitlements
            id = new Label();
            id.getStyleClass().add("offer-label-small");
            id.setPadding(new Insets(-5, 0, 0, 0));
        }

        @Override
        protected void onViewAttached() {
            userName.textProperty().bind(model.userName);
            id.textProperty().bind(model.id);

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    this.roboIcon.setImage(roboIcon);
                }
            });
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbind();
            id.textProperty().unbind();
            roboHashNodeSubscription.unsubscribe();
        }
    }
}
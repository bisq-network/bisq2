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

package bisq.desktop.main.content.chat.sidebar;

import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

// TODO used in a List, should be extracted to a ListItem

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChannelSidebarUserProfile implements Comparable<ChannelSidebarUserProfile> {
    @EqualsAndHashCode.Include
    private final Controller controller;

    public ChannelSidebarUserProfile(BannedUserService bannedUserService, UserProfile userProfile) {
        this(bannedUserService, userProfile, false);
    }

    public ChannelSidebarUserProfile(BannedUserService bannedUserService, UserProfile userProfile, boolean ignored) {
        controller = new Controller(userProfile, bannedUserService);
        controller.model.ignored = ignored;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ImageView getCatIcon() {
        return controller.view.getCatIcon();
    }

    public boolean isIgnored() {
        return controller.model.ignored;
    }

    public UserProfile getUserProfile() {
        return controller.model.userProfile;
    }

    @Override
    public int compareTo(ChannelSidebarUserProfile o) {
        return controller.model.userProfile.getUserName().compareTo(o.controller.model.userProfile.getUserName());
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class Controller implements bisq.desktop.common.view.Controller {
        @EqualsAndHashCode.Include
        private final Model model;

        private final BannedUserService bannedUserService;
        @Getter
        private final View view;

        private Controller(UserProfile userProfile, BannedUserService bannedUserService) {
            this.bannedUserService = bannedUserService;
            model = new Model(userProfile);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            UserProfile userProfile = model.userProfile;
            if (userProfile == null) {
                return;
            }

            String userName = userProfile.getUserName();
            model.userName.set(isUserProfileBanned() ? Res.get("user.userProfile.userName.banned", userName) : userName);
            model.catHashImage.set(CatHash.getImage(userProfile));
        }

        @Override
        public void onDeactivate() {
        }

        public boolean isUserProfileBanned() {
            return bannedUserService.isUserProfileBanned(model.userProfile);
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class Model implements bisq.desktop.common.view.Model {
        @EqualsAndHashCode.Include
        private final UserProfile userProfile;

        private boolean ignored;
        private final ObjectProperty<Image> catHashImage = new SimpleObjectProperty<>();
        private final StringProperty userName = new SimpleStringProperty();

        private Model(UserProfile userProfile) {
            this.userProfile = userProfile;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        @Getter
        private final ImageView catIcon;
        private final Label userName;
        private Subscription catHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new HBox(10), model, controller);

            root.setAlignment(Pos.CENTER_LEFT);

            boolean isUserProfileBanned = controller.isUserProfileBanned();

            userName = new Label();
            userName.getStyleClass().add("text-fill-white");
            userName.setMaxWidth(100);
            if (isUserProfileBanned) {
                userName.getStyleClass().add("error");
            }

            String banPrefix = isUserProfileBanned ? Res.get("user.userProfile.tooltip.banned") + "\n" : "";
            String tooltipString = banPrefix + model.userProfile.getTooltipString();
            Tooltip.install(userName, new BisqTooltip(tooltipString));

            catIcon = new ImageView();
            catIcon.setFitWidth(37.5);
            catIcon.setFitHeight(37.5);
            Tooltip.install(catIcon, new BisqTooltip(tooltipString));
            if (isUserProfileBanned) {
                // coloring icon red
                /*Blend blush = new Blend(BlendMode.MULTIPLY,
                        new ColorAdjust(),
                        new ColorInput(0,
                                0,
                                37.5,
                                37.5,
                                Color.RED));
                catIcon.setClip(new Circle(18.75, 18.75, 18.75));
                catIcon.setEffect(blush);*/
            }

            root.getChildren().addAll(catIcon, userName);
        }

        @Override
        protected void onViewAttached() {
            userName.textProperty().bind(model.userName);
            catHashNodeSubscription = EasyBind.subscribe(model.catHashImage, image -> {
                if (image != null) {
                    this.catIcon.setImage(image);
                }
            });
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbind();
            catHashNodeSubscription.unsubscribe();
        }
    }
}
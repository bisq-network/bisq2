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
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
        return controller.view.getCatHashImageView();
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
            model.setUserName(isUserProfileBanned() ? Res.get("user.userProfile.userName.banned", userName) : userName);
            model.setCatHashImage(CatHash.getImage(userProfile));
        }

        @Override
        public void onDeactivate() {
            model.setCatHashImage(null);
        }

        public boolean isUserProfileBanned() {
            return bannedUserService.isUserProfileBanned(model.userProfile);
        }
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class Model implements bisq.desktop.common.view.Model {
        @EqualsAndHashCode.Include
        private final UserProfile userProfile;
        private boolean ignored;
        @Setter
        private Image catHashImage;
        @Setter
        private String userName;

        private Model(UserProfile userProfile) {
            this.userProfile = userProfile;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        @Getter
        private final ImageView catHashImageView;
        private final Label userName;

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

            catHashImageView = new ImageView();
            catHashImageView.setFitWidth(37.5);
            catHashImageView.setFitHeight(37.5);
            Tooltip.install(catHashImageView, new BisqTooltip(tooltipString));
            root.getChildren().addAll(catHashImageView, userName);
        }

        @Override
        protected void onViewAttached() {
            userName.setText(model.getUserName());
            catHashImageView.setImage(model.getCatHashImage());
        }

        @Override
        protected void onViewDetached() {
        }
    }
}
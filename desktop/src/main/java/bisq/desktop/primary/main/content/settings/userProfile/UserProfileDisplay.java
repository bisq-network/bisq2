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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.DataService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class UserProfileDisplay {
    private final Controller controller;

    public UserProfileDisplay(UserIdentityService userIdentityService, UserIdentity userIdentity) {
        controller = new Controller(userIdentityService, userIdentity);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserIdentityService userIdentityService;

        private Controller(UserIdentityService userIdentityService, UserIdentity userIdentity) {
            this.userIdentityService = userIdentityService;
            model = new Model();

            UserProfile userProfile = userIdentity.getUserProfile();
            model.identityId = userProfile.getId();
            model.statement.set(userProfile.getStatement());
            model.terms.set(userProfile.getTerms());
            model.reputationScore = userProfile.getBurnScoreAsString();
            model.profileAge = userProfile.getAccountAgeAsString();
            model.nymId = userProfile.getNym();
            model.nickName = userProfile.getNickName();
            model.roboHashNode = RoboHash.getImage(userProfile.getPubKeyHash());

            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            userIdentityService.getSelectedUserProfile().addObserver(userIdentity -> {
                model.statement.set(userIdentity.getUserProfile().getStatement());
                model.terms.set(userIdentity.getUserProfile().getTerms());
            });
        }

        @Override
        public void onDeactivate() {
        }

        @Override
        public boolean useCaching() {
            return false;
        }

        public void onEdit() {
            Navigation.navigateTo(NavigationTarget.EDIT_PROFILE);
        }

        public void onDelete() {
            if (userIdentityService.getUserIdentities().size() < 2) {
                new Popup().warning(Res.get("settings.userProfile.deleteProfile.lastProfile.warning")).show();
            } else {
                new Popup().warning(Res.get("settings.userProfile.deleteProfile.warning"))
                        .onAction(this::doDelete)
                        .actionButtonText(Res.get("settings.userProfile.deleteProfile.warning.yes"))
                        .closeButtonText(Res.get("cancel"))
                        .show();
            }
        }

        private CompletableFuture<DataService.BroadCastDataResult> doDelete() {
            return userIdentityService.deleteUserProfile(userIdentityService.getSelectedUserProfile().get())
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            new Popup().error(throwable.getMessage()).show();
                        }
                    });
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private Image roboHashNode;
        private String nymId;
        private String nickName;
        private String identityId;
        private final StringProperty statement = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private String reputationScore;
        private String profileAge;
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<GridPane, Model, Controller> {
        private final MaterialTextArea terms;
        private final MaterialTextField statement;
        private int rowIndex;
        private final Button editButton, deletedButton;

        private View(Model model, UserProfileDisplay.Controller controller) {
            super(new GridPane(), model, controller);

            root.setHgap(20);
            root.setVgap(20);

            ColumnConstraints col1 = new ColumnConstraints();
            ColumnConstraints col2 = new ColumnConstraints();
            root.getColumnConstraints().addAll(col1, col2);
            col1.setPercentWidth(50);
            col2.setPercentWidth(50);

            Label headlineLabel = new Label(Res.get("settings.userProfile.selectedProfile"));
            headlineLabel.getStyleClass().add("bisq-text-3");
            GridPane.setMargin(headlineLabel, new Insets(0, 0, -10, 0));
            root.add(headlineLabel, 0, 0, 2, 1);

            addField(Res.get("social.chatUser.nickName"), model.nickName, 0, ++rowIndex);
            addField(Res.get("social.chatUser.nymId"), model.nymId, 0, ++rowIndex);

            ImageView roboIconImageView = new ImageView(model.roboHashNode);
            roboIconImageView.setFitWidth(120);
            roboIconImageView.setFitHeight(120);
            root.add(roboIconImageView, 1, rowIndex - 1, 2, 2);

            addField(Res.get("social.chatUser.identityId"), model.identityId, 0, ++rowIndex);
            addField(Res.get("social.chatUser.profileAge"), model.profileAge, 1, rowIndex);
            addField(Res.get("social.chatUser.reputationScore"), model.reputationScore, 0, ++rowIndex);
            terms = addTextArea(Res.get("social.chatUser.terms"), model.terms.get(), 1, rowIndex);
            statement = addField(Res.get("social.chatUser.statement"), model.statement.get(), 0, ++rowIndex);

            deletedButton = new Button(Res.get("settings.userProfile.deleteProfile"));
            deletedButton.getStyleClass().addAll("outlined-button", "grey-outlined-button");
            editButton = new Button(Res.get("edit"));
            editButton.getStyleClass().addAll("outlined-button");
            HBox buttons = new HBox(10, deletedButton, editButton);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            GridPane.setHalignment(buttons, HPos.RIGHT);
            root.add(buttons, 0, ++rowIndex, 2, 1);
        }

        @Override
        protected void onViewAttached() {
            statement.textProperty().bind(model.statement);
            terms.textProperty().bind(model.terms);
            editButton.setOnAction(e -> controller.onEdit());
            deletedButton.setOnAction(e -> controller.onDelete());
        }

        @Override
        protected void onViewDetached() {
            statement.textProperty().unbind();
            terms.textProperty().unbind();
            editButton.setOnAction(null);
            deletedButton.setOnAction(null);
        }

        private MaterialTextField addField(String description, String value, int columnIndex, int rowIndex) {
            MaterialTextField field = new MaterialTextField(description);
            field.setEditable(false);
            field.setText(value);
            field.setDisable(value == null || value.isEmpty());
            root.add(field, columnIndex, rowIndex, 1, 1);
            return field;
        }

        private MaterialTextArea addTextArea(String description, String value, int columnIndex, int rowIndex) {
            MaterialTextArea field = new MaterialTextArea(description);
            field.setEditable(false);
            field.setText(value);
            field.setDisable(value == null || value.isEmpty());
            field.setFixedHeight(2 * 56 + 20); // MaterialTextField has height 56
            root.add(field, columnIndex, rowIndex, 1, 2);
            return field;
        }
    }
}
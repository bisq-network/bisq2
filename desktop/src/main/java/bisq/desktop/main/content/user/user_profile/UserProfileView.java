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

package bisq.desktop.main.content.user.user_profile;

import bisq.common.util.StringUtils;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

@Slf4j
public class UserProfileView extends View<HBox, UserProfileModel, UserProfileController> {
    private final Button createNewProfileButton, deletedButton, saveButton;
    private final MaterialTextField nymId, profileId, profileAge, reputationScoreField, statement;
    private final ImageView roboIconImageView;
    private final MaterialTextArea terms;
    private final VBox formVBox;
    private final AutoCompleteComboBox<UserIdentity> comboBox;
    private Subscription reputationScorePin, selectedChatUserIdentityPin;

    public UserProfileView(UserProfileModel model, UserProfileController controller) {
        super(new HBox(20), model, controller);

        root.setPadding(new Insets(40, 0, 0, 0));

        roboIconImageView = new ImageView();
        roboIconImageView.setFitWidth(125);
        roboIconImageView.setFitHeight(125);
        root.getChildren().add(roboIconImageView);

        formVBox = new VBox(20);
        HBox.setHgrow(formVBox, Priority.ALWAYS);
        root.getChildren().add(formVBox);

        createNewProfileButton = new Button(Res.get("user.userProfile.createNewProfile"));
        createNewProfileButton.getStyleClass().addAll("outlined-button");

        comboBox = new AutoCompleteComboBox<>(model.getUserIdentities(), Res.get("user.bondedRoles.userProfile.select"));
        comboBox.setPrefWidth(300);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserIdentity userIdentity) {
                return userIdentity != null ? StringUtils.truncate(userIdentity.getUserName(), 30) : "";
            }

            @Override
            public UserIdentity fromString(String string) {
                return null;
            }
        });

        HBox selectionButtonHBox = new HBox(20, comboBox, Spacer.fillHBox(), createNewProfileButton);
        formVBox.getChildren().add(selectionButtonHBox);

        nymId = addField(Res.get("user.userProfile.nymId"));
        nymId.setIconTooltip(Res.get("user.userProfile.nymId.tooltip"));

        profileId = addField(Res.get("user.userProfile.profileId"));
        profileId.setIconTooltip(Res.get("user.userProfile.profileId.tooltip"));

        profileAge = addField(Res.get("user.userProfile.profileAge"));
        profileAge.setIconTooltip(Res.get("user.userProfile.profileAge.tooltip"));

        reputationScoreField = addField(Res.get("user.userProfile.reputation"));

        statement = addField(Res.get("user.userProfile.statement"), Res.get("user.userProfile.statement.prompt"));
        statement.setEditable(true);
        statement.showEditIcon();
        statement.getIconButton().setOpacity(0.3);

        terms = addTextArea(Res.get("user.userProfile.terms"), Res.get("user.userProfile.terms.prompt"));
        terms.setEditable(true);
        terms.showEditIcon();
        terms.getIconButton().setOpacity(0.3);

        saveButton = new Button(Res.get("action.save"));
        saveButton.setDefaultButton(true);

        deletedButton = new Button(Res.get("user.userProfile.deleteProfile"));

        // todo For now we hide the deletedButton because it comes with some complexities
        // See https://github.com/bisq-network/bisq2/issues/794
        HBox buttonsHBox = new HBox(20, saveButton/*, deletedButton*/);
        formVBox.getChildren().add(buttonsHBox);
    }

    @Override
    protected void onViewAttached() {
        nymId.textProperty().bind(model.getNymId());
        profileId.textProperty().bind(model.getProfileId());
        profileAge.textProperty().bind(model.getProfileAge());
        reputationScoreField.textProperty().bind(model.getReputationScoreValue());
        statement.textProperty().bindBidirectional(model.getStatement());
        terms.textProperty().bindBidirectional(model.getTerms());
        roboIconImageView.imageProperty().bind(model.getRoboHash());
        saveButton.disableProperty().bind(model.getSaveButtonDisabled());

        reputationScorePin = EasyBind.subscribe(model.getReputationScore(), reputationScore -> {
            if (reputationScore != null) {
                reputationScoreField.setIconTooltip(reputationScore.getDetails());
            }
        });

        deletedButton.setOnAction(e -> controller.onDelete());
        saveButton.setOnAction(e -> controller.onSave());
        createNewProfileButton.setOnAction(e -> controller.onAddNewChatUser());
        comboBox.setOnChangeConfirmed(e -> {
            if (comboBox.getSelectionModel().getSelectedItem() == null) {
                comboBox.getSelectionModel().select(model.getSelectedUserIdentity().get());
                return;
            }
            controller.onSelected(comboBox.getSelectionModel().getSelectedItem());
        });

        selectedChatUserIdentityPin = EasyBind.subscribe(model.getSelectedUserIdentity(),
                userIdentity -> comboBox.getSelectionModel().select(userIdentity));
    }

    @Override
    protected void onViewDetached() {
        nymId.textProperty().unbind();
        profileId.textProperty().unbind();
        profileAge.textProperty().unbind();
        reputationScoreField.textProperty().unbind();
        statement.textProperty().unbindBidirectional(model.getStatement());
        terms.textProperty().unbindBidirectional(model.getTerms());
        roboIconImageView.imageProperty().unbind();
        saveButton.disableProperty().unbind();

        reputationScorePin.unsubscribe();
        selectedChatUserIdentityPin.unsubscribe();

        deletedButton.setOnAction(null);
        saveButton.setOnAction(null);
        createNewProfileButton.setOnAction(null);
        comboBox.setOnChangeConfirmed(null);
    }

    private MaterialTextField addField(String description) {
        return addField(description, null);
    }

    private MaterialTextField addField(String description, @Nullable String prompt) {
        MaterialTextField field = new MaterialTextField(description, prompt);
        field.setEditable(false);
        formVBox.getChildren().add(field);
        return field;
    }

    private MaterialTextArea addTextArea(String description, String prompt) {
        MaterialTextArea field = new MaterialTextArea(description, prompt);
        field.setEditable(false);
        formVBox.getChildren().add(field);
        return field;
    }

    @EqualsAndHashCode
    @Getter
    public static class ListItem {
        private final UserIdentity userIdentity;

        public ListItem(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
        }
    }
}

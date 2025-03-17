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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitPane;
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

import static bisq.user.profile.UserProfile.*;

@Slf4j
public class UserProfileView extends View<HBox, UserProfileModel, UserProfileController> {

    private static final TextMaxLengthValidator TERMS_MAX_LENGTH_VALIDATOR =
            new TextMaxLengthValidator(MAX_LENGTH_TERMS, Res.get("user.userProfile.terms.tooLong", MAX_LENGTH_TERMS));
    private static final TextMaxLengthValidator STATEMENT_MAX_LENGTH_VALIDATOR =
            new TextMaxLengthValidator(MAX_LENGTH_STATEMENT, Res.get("user.userProfile.statement.tooLong", MAX_LENGTH_STATEMENT));

    private static final String STATEMENT_PROMPT = Res.get("user.userProfile.statement.prompt");
    private static final String TERMS_PROMPT = Res.get("user.userProfile.terms.prompt");

    private final Button createNewProfileButton, deleteButton, saveButton;
    private final SplitPane deleteWrapper;
    private final MaterialTextField nymId, profileId, profileAge, livenessState, reputationScoreField, statement;
    private final ImageView catHashImageView;
    private final MaterialTextArea terms;
    private final VBox formVBox;
    private final AutoCompleteComboBox<UserIdentity> comboBox;
    private final BisqTooltip deleteTooltip;
    private final Hyperlink learnMore;
    private Subscription reputationScorePin, useDeleteTooltipPin, selectedChatUserIdentityPin, isValidSelectionPin;

    public UserProfileView(UserProfileModel model, UserProfileController controller) {
        super(new HBox(), model, controller);

        HBox contentBox = new HBox(20);
        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setPadding(new Insets(0, 40, 20, 40));
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));

        catHashImageView = new ImageView();
        catHashImageView.setFitWidth(UserProfileModel.CAT_HASH_IMAGE_SIZE);
        catHashImageView.setFitHeight(catHashImageView.getFitWidth());
        catHashImageView.getStyleClass().add("hand-cursor");
        contentBox.getChildren().add(catHashImageView);

        formVBox = new VBox(25);
        HBox.setHgrow(formVBox, Priority.ALWAYS);
        contentBox.getChildren().add(formVBox);

        createNewProfileButton = new Button(Res.get("user.userProfile.createNewProfile"));
        createNewProfileButton.getStyleClass().addAll("outlined-button");

        learnMore = new Hyperlink(Res.get("user.userProfile.learnMore"));

        VBox buttons = new VBox(5, createNewProfileButton, learnMore);
        buttons.setAlignment(Pos.TOP_RIGHT);

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

        HBox selectionButtonHBox = new HBox(20, comboBox, Spacer.fillHBox(), buttons);
        formVBox.getChildren().add(selectionButtonHBox);

        nymId = addField(Res.get("user.userProfile.nymId"));
        nymId.setIconTooltip(Res.get("user.userProfile.nymId.tooltip"));

        profileId = addField(Res.get("user.userProfile.profileId"));
        profileId.setIconTooltip(Res.get("user.userProfile.profileId.tooltip"));

        profileAge = addField(Res.get("user.userProfile.profileAge"));
        profileAge.setIconTooltip(Res.get("user.userProfile.profileAge.tooltip"));

        livenessState = addField(Res.get("user.userProfile.livenessState.description"));
        livenessState.setIconTooltip(Res.get("user.userProfile.livenessState.tooltip"));

        reputationScoreField = addField(Res.get("user.userProfile.reputation"));

        statement = addField(Res.get("user.userProfile.statement"), STATEMENT_PROMPT);
        statement.setEditable(true);
        statement.showEditIcon();
        statement.getIconButton().setOpacity(0.3);
        statement.setValidators(STATEMENT_MAX_LENGTH_VALIDATOR);

        terms = addTextArea(Res.get("user.userProfile.terms"), TERMS_PROMPT);
        terms.setEditable(true);
        terms.showEditIcon();
        terms.getIconButton().setOpacity(0.3);
        terms.setValidators(TERMS_MAX_LENGTH_VALIDATOR);

        saveButton = new Button(Res.get("action.save"));
        saveButton.setDefaultButton(true);

        deleteButton = new Button(Res.get("user.userProfile.deleteProfile"));
        deleteWrapper = new SplitPane(deleteButton);
        deleteTooltip = new BisqTooltip(Res.get("user.userProfile.deleteProfile.cannotDelete"), BisqTooltip.Style.MEDIUM_DARK);

        HBox buttonsHBox = new HBox(20, saveButton, deleteWrapper);
        formVBox.getChildren().add(buttonsHBox);
    }

    @Override
    protected void onViewAttached() {
        nymId.textProperty().bind(model.getNymId());
        profileId.textProperty().bind(model.getProfileId());
        profileAge.textProperty().bind(model.getProfileAge());
        livenessState.textProperty().bind(model.getLivenessState());
        reputationScoreField.textProperty().bind(model.getReputationScoreValue());
        statement.textProperty().bindBidirectional(model.getStatement());
        terms.textProperty().bindBidirectional(model.getTerms());
        catHashImageView.imageProperty().bind(model.getCatHashImage());

        useDeleteTooltipPin = EasyBind.subscribe(model.getUseDeleteTooltip(), useDeleteTooltip ->
                deleteWrapper.setTooltip(useDeleteTooltip ? deleteTooltip : null));
        reputationScorePin = EasyBind.subscribe(model.getReputationScore(), reputationScore -> {
            if (reputationScore != null) {
                reputationScoreField.setIconTooltip(reputationScore.getTooltipString());
            }
        });

        deleteButton.setOnAction(e -> onDeleteButtonPressed());
        saveButton.setOnAction(e -> onSaveButtonPressed());
        createNewProfileButton.setOnAction(e -> controller.onAddNewChatUser());
        learnMore.setOnAction(e -> controller.onLearnMore());
        comboBox.setOnChangeConfirmed(e -> {
            if (comboBox.getSelectionModel().getSelectedItem() == null) {
                comboBox.getSelectionModel().select(model.getSelectedUserIdentity().get());
                return;
            }
            controller.onSelected(comboBox.getSelectionModel().getSelectedItem());
        });
        comboBox.validateOnNoItemSelectedWithMessage(Res.get("user.bondedRoles.userProfile.select.invalid"));
        catHashImageView.setOnMouseClicked(e -> controller.onOpenProfileCard());

        selectedChatUserIdentityPin = EasyBind.subscribe(model.getSelectedUserIdentity(),
                userIdentity -> comboBox.getSelectionModel().select(userIdentity));

        isValidSelectionPin = EasyBind.subscribe(comboBox.getIsValidSelection(), isValidSelection -> UIThread.run(() -> {
            if (!isValidSelection) {
                disableEditableTextBoxes();
                controller.resetSelection();
            } else {
                enableEditableTextBoxes();
            }
        }));
    }

    @Override
    protected void onViewDetached() {
        nymId.textProperty().unbind();
        profileId.textProperty().unbind();
        profileAge.textProperty().unbind();
        livenessState.textProperty().unbind();
        reputationScoreField.textProperty().unbind();
        statement.textProperty().unbindBidirectional(model.getStatement());
        terms.textProperty().unbindBidirectional(model.getTerms());
        catHashImageView.imageProperty().unbind();
        saveButton.disableProperty().unbind();
        deleteButton.disableProperty().unbind();
        deleteWrapper.tooltipProperty().unbind();

        reputationScorePin.unsubscribe();
        useDeleteTooltipPin.unsubscribe();
        selectedChatUserIdentityPin.unsubscribe();
        isValidSelectionPin.unsubscribe();

        deleteButton.setOnAction(null);
        saveButton.setOnAction(null);
        createNewProfileButton.setOnAction(null);
        learnMore.setOnAction(null);
        comboBox.setOnChangeConfirmed(null);
        comboBox.getIsValidSelection().set(true);
        catHashImageView.setOnMouseClicked(null);

        comboBox.resetValidation();
        statement.resetValidation();
        terms.resetValidation();
        catHashImageView.setImage(null);
    }

    private void disableEditableTextBoxes() {
        statement.setEditable(false);
        statement.setPromptText("");
        terms.setEditable(false);
        terms.setPromptText("");
    }

    private void enableEditableTextBoxes() {
        statement.setEditable(true);
        statement.setPromptText(STATEMENT_PROMPT);
        terms.setEditable(true);
        terms.setPromptText(TERMS_PROMPT);
    }

    private void onSaveButtonPressed() {
        if (runOnSaveValidations()) {
            controller.onSave();
        }
    }

    private void onDeleteButtonPressed() {
        if(!comboBox.getIsValidSelection().get()) {
            new Popup().invalid(Res.get("user.userProfile.popup.noSelectedProfile")).show();
            return;
        }
        controller.onDeleteProfile();
    }

    private boolean runOnSaveValidations() {
        if (!comboBox.getIsValidSelection().get()) {
            new Popup().invalid(Res.get("user.userProfile.popup.noSelectedProfile")).show();
            return false;
        }
        var validComboboxSelection = comboBox.validate();
        var validStatement = statement.validate();
        var validTerms = terms.validate();
        return validComboboxSelection && validStatement && validTerms;
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

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem {
        @EqualsAndHashCode.Include
        private final UserIdentity userIdentity;

        public ListItem(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
        }
    }
}

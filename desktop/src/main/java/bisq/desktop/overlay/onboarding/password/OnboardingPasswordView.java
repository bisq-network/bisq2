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

package bisq.desktop.overlay.onboarding.password;

import bisq.desktop.common.validation.PasswordValidator;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardingPasswordView extends View<VBox, OnboardingPasswordModel, OnboardingPasswordController> {
    private final MaterialPasswordField password, confirmedPassword;
    private final Button setPasswordButton, skipButton;

    public OnboardingPasswordView(OnboardingPasswordModel model, OnboardingPasswordController controller, PasswordValidator confirmedPasswordValidator) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(50, 30, 10, 30));
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        Label headline = new Label(Res.get("onboarding.password.headline.setPassword"));
        headline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        Label subtitleLabel = new Label(Res.get("onboarding.password.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        subtitleLabel.setMinHeight(40);
        subtitleLabel.setMaxWidth(375);

        password = new MaterialPasswordField(Res.get("onboarding.password.enterPassword"));
        password.setValidator(new PasswordValidator());
        password.setMaxWidth(315);

        confirmedPassword = new MaterialPasswordField(Res.get("onboarding.password.confirmPassword"));
        confirmedPassword.setValidator(confirmedPasswordValidator);
        confirmedPassword.setMaxWidth(password.getMaxWidth());

        setPasswordButton = new Button(Res.get("onboarding.password.button.savePassword"));
        setPasswordButton.setDefaultButton(true);

        skipButton = new Button(Res.get("onboarding.password.button.skip"));
        skipButton.getStyleClass().add("outlined-button");
        HBox buttons = new HBox(20, setPasswordButton, skipButton);
        buttons.setAlignment(Pos.CENTER);
        root.getChildren().setAll(headline, subtitleLabel, password, confirmedPassword, buttons);
    }

    @Override
    protected void onViewAttached() {
        password.passwordProperty().bindBidirectional(model.getPassword());
        password.isMaskedProperty().bindBidirectional(model.getPasswordIsMasked());
        confirmedPassword.passwordProperty().bindBidirectional(model.getConfirmedPassword());
        confirmedPassword.isMaskedProperty().bindBidirectional(model.getConfirmedPasswordIsMasked());
        setPasswordButton.disableProperty().bind(model.getSetPasswordButtonDisabled());
        setPasswordButton.setOnAction(e -> controller.onSetPassword());
        skipButton.setOnAction(e -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
        password.passwordProperty().unbindBidirectional(model.getPassword());
        password.isMaskedProperty().unbindBidirectional(model.getPasswordIsMasked());
        confirmedPassword.passwordProperty().unbindBidirectional(model.getConfirmedPassword());
        confirmedPassword.isMaskedProperty().unbindBidirectional(model.getConfirmedPasswordIsMasked());
        setPasswordButton.disableProperty().unbind();
        setPasswordButton.setOnAction(null);
        skipButton.setOnAction(null);
    }
}

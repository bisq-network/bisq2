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

package bisq.desktop.primary.overlay.onboarding.password;

import bisq.desktop.common.utils.validation.PasswordValidator;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.primary.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardingPasswordView extends View<VBox, OnboardingPasswordModel, OnboardingPasswordController> {
    private final MaterialPasswordField password, confirmedPassword;
    private final Button setPasswordButton, skipButton;

    public OnboardingPasswordView(OnboardingPasswordModel model, OnboardingPasswordController controller, PasswordValidator confirmedPasswordValidator) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(25);
        root.setPadding(new Insets(10, 30, 10, 30));
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        Label headline = new Label(Res.get("user.password.headline.setPassword"));
        headline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        Label subtitleLabel = new Label(Res.get("onboarding.password.subTitle"));
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        password = new MaterialPasswordField(Res.get("user.password.enterPassword"));
        password.setValidator(new PasswordValidator());

        confirmedPassword = new MaterialPasswordField(Res.get("user.password.confirmPassword"));
        confirmedPassword.setValidator(confirmedPasswordValidator);

        setPasswordButton = new Button(Res.get("user.password.button.savePassword"));
        setPasswordButton.setDefaultButton(true);

        skipButton = new Button(Res.get("onboarding.password.button.skip"));
        skipButton.getStyleClass().add("outlined-button");
        HBox buttons = new HBox(20, setPasswordButton, skipButton);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(headline, new Insets(40, 0, 0, 0));
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

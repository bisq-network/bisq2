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

package bisq.desktop.main.content.wallet.setup_wallet_wizard.verify;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Arrays;

@Slf4j
public class SetupWalletWizardVerifyView extends View<StackPane, SetupWalletWizardVerifyModel, SetupWalletWizardVerifyController> {
    private static final int ANSWER_BUTTONS_COUNT = 3;
    private static final int BUTTON_MIN_WIDTH = 160;
    private static final int CONTENT_SPACING = 30;
    private static final int BUTTON_SPACING = 20;

    private final VBox content;
    private final Label questionLabel;
    private final Button[] answerButtons = new Button[ANSWER_BUTTONS_COUNT];
    private final ChangeListener<Number> questionIndexListener, answerIndexListener;
    private final Button createWalletSuccessButton, wrongWordGoBackButton;
    private final WizardOverlay wrongWordOverlay, createWalletSuccessOverlay;
    private Subscription showCreateWalletSuccessPin, transitionSubscriptionPin, shouldShowWrongWordOverlayPin;
    private UIScheduler slideNextQuestionScheduler;

    public SetupWalletWizardVerifyView(SetupWalletWizardVerifyModel model,
                                       SetupWalletWizardVerifyController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(CONTENT_SPACING);
        content.setAlignment(Pos.CENTER);

        Label headlineLabel = new Label(Res.get("wallet.verifySeeds.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        questionLabel = new Label();
        questionLabel.getStyleClass().addAll("bisq-text-1", "text-fill-grey-dimmed");

        HBox answerButtonsRow = new HBox(BUTTON_SPACING);
        answerButtonsRow.setAlignment(Pos.CENTER);
        VBox.setMargin(answerButtonsRow, new Insets(10, 0, 0, 0));

        for (int i = 0; i < ANSWER_BUTTONS_COUNT; i++) {
            Button btn = new Button();
            btn.setMinWidth(BUTTON_MIN_WIDTH);
            int idx = i;
            btn.setOnAction(e -> controller.onAnswerSelected(idx));
            answerButtons[i] = btn;
            answerButtonsRow.getChildren().add(btn);
        }

        wrongWordGoBackButton = new Button(Res.get("wallet.verifySeeds.wrongWord.closeButton"));
        wrongWordGoBackButton.setDefaultButton(true);
        wrongWordOverlay = new WizardOverlay(root)
                .warning()
                .headline("wallet.verifySeeds.wrongWord.title")
                .description("wallet.verifySeeds.wrongWord.description")
                .buttons(wrongWordGoBackButton)
                .build();

        createWalletSuccessButton = new Button(Res.get("wallet.verifySeeds.button.success.nextStep"));
        createWalletSuccessButton.setDefaultButton(true);
        createWalletSuccessOverlay = new WizardOverlay(root)
                .info()
                .headline("wallet.verifySeeds.success.title")
                .description("wallet.verifySeeds.success.description")
                .buttons(createWalletSuccessButton)
                .build();

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, questionLabel, answerButtonsRow, Spacer.fillVBox());
        root.getChildren().addAll(content, createWalletSuccessOverlay, wrongWordOverlay);

        questionIndexListener = (obs, oldVal, newVal) -> updateQuestion();
        answerIndexListener = (obs, oldVal, newVal) -> updateButtonStylesAndNextState();
    }

    @Override
    protected void onViewAttached() {
        showCreateWalletSuccessPin = EasyBind.subscribe(model.getCurrentScreenState(), state -> {
            boolean shouldShow = state == SetupWalletWizardVerifyModel.ScreenState.SUCCESS;
            createWalletSuccessOverlay.updateOverlayVisibility(content,
                    shouldShow,
                    controller::onKeyPressedWhileShowingSuccessOverlay);
        });

        transitionSubscriptionPin = EasyBind.subscribe(model.getShouldTransitionToNextQuestion(), shouldTransition -> {
            if (shouldTransition) {
                showNextQuestionWithDelayAndAnimation();
            }
        });

        shouldShowWrongWordOverlayPin = EasyBind.subscribe(model.getShouldShowWrongWordOverlay(), shouldShow ->
            wrongWordOverlay.updateOverlayVisibility(content,
                    shouldShow,
                    controller::onKeyPressedWhileShowingWrongWordOverlay));

        createWalletSuccessButton.setOnAction(e -> controller.onCreateWallet());
        wrongWordGoBackButton.setOnAction(e -> controller.onWrongWord());

        model.getCurrentQuestionIndex().addListener(questionIndexListener);
        model.getSelectedAnswerIndex().addListener(answerIndexListener);
        updateQuestion();
    }

    @Override
    protected void onViewDetached() {
        showCreateWalletSuccessPin.unsubscribe();
        transitionSubscriptionPin.unsubscribe();
        shouldShowWrongWordOverlayPin.unsubscribe();

        createWalletSuccessButton.setOnAction(null);
        wrongWordGoBackButton.setOnAction(null);

        model.getCurrentQuestionIndex().removeListener(questionIndexListener);
        model.getSelectedAnswerIndex().removeListener(answerIndexListener);

        if (slideNextQuestionScheduler != null) {
            slideNextQuestionScheduler.stop();
            slideNextQuestionScheduler = null;
        }
    }

    private void updateQuestion() {
        int qIdx = model.getCurrentQuestionIndex().get();
        model.getSelectedAnswerIndex().set(SetupWalletWizardVerifyModel.INVALID_INDEX);
        if (qIdx >= model.getQuestionPositions().size()) {
            questionLabel.setText(Res.get("wallet.verifySeeds.success.title"));
            Arrays.stream(answerButtons).forEach(btn -> btn.setVisible(false));
            return;
        }
        int pos = model.getQuestionPositions().get(qIdx);
        questionLabel.setText(Res.get("wallet.verifySeeds.question", (pos + 1)));
        for (int i = 0; i < ANSWER_BUTTONS_COUNT; i++) {
            answerButtons[i].setText(model.getAnswerChoices().get(qIdx).get(i));
            answerButtons[i].setVisible(true);
            answerButtons[i].setDisable(false);
        }
        updateButtonStylesAndNextState();
    }

    private void updateButtonStylesAndNextState() {
        int selected = model.getSelectedAnswerIndex().get();
        for (int i = 0; i < ANSWER_BUTTONS_COUNT; i++) {
            answerButtons[i].setDefaultButton(i == selected);
        }
        // Only delegate when the user has actually selected something
        if (selected != SetupWalletWizardVerifyModel.INVALID_INDEX) {
            controller.onNextWordSelected();
        }
    }

    private void showNextQuestionWithDelayAndAnimation() {
        if (Transitions.useAnimations()) {
            slideNextQuestionScheduler = UIScheduler.run(this::slideNextQuestion).after(400);
        } else {
            controller.onGoToNextQuestion();
        }
    }

    private void slideNextQuestion() {
        Transitions.slideOutHorizontal(content, Duration.millis(450), () -> {
            controller.onGoToNextQuestion();
            Transitions.slideInRight(content, 450, () -> {});
        }, false);
    }
}

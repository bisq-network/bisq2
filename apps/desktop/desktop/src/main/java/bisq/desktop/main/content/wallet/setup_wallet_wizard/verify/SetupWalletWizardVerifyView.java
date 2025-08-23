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

import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Arrays;

@Slf4j
public class SetupWalletWizardVerifyView extends View<StackPane, SetupWalletWizardVerifyModel, SetupWalletWizardVerifyController> {
    private static final int FEEDBACK_WIDTH = 700;
    private static final int ANSWER_BUTTONS_COUNT = 3;
    private static final int BUTTON_MIN_WIDTH = 160;
    private static final int CONTENT_SPACING = 30;
    private static final int BUTTON_SPACING = 20;

    private final VBox content, createWalletSuccess;
    private final Label questionLabel;
    private final Button[] answerButtons = new Button[ANSWER_BUTTONS_COUNT];
    private final ChangeListener<Number> questionIndexListener, answerIndexListener;
    private final Button createWalletSuccessButton, goBackButton;
    private final WizardOverlay wrongWordOverlay;
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

        Label warningIcon = new Label();
        Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "1.7em");
        goBackButton = new Button(Res.get("wallet.verifySeeds.wrongWord.closeButton"));
        goBackButton.setDefaultButton(true);
        wrongWordOverlay = new WizardOverlay(root,
                "wallet.verifySeeds.wrongWord.title",
                warningIcon,
                "wallet.verifySeeds.wrongWord.description",
                goBackButton);

        questionIndexListener = (obs, oldVal, newVal) -> updateQuestion();
        answerIndexListener = (obs, oldVal, newVal) -> updateButtonStylesAndNextState();

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, questionLabel, answerButtonsRow, Spacer.fillVBox());

        createWalletSuccessButton = new Button(Res.get("wallet.verifySeeds.button.success.nextStep"));
        createWalletSuccess = new VBox(20);
        configCreateWalletSuccess();
        StackPane.setMargin(createWalletSuccess, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));

        root.getChildren().addAll(content, createWalletSuccess, wrongWordOverlay);
    }

    @Override
    protected void onViewAttached() {
        showCreateWalletSuccessPin = EasyBind.subscribe(model.getCurrentScreenState(), state -> {
            boolean show = state == SetupWalletWizardVerifyModel.ScreenState.SUCCESS;
            createWalletSuccess.setVisible(show);
            if (show) {
                Transitions.blurStrong(content, 0);
                Transitions.slideInTop(createWalletSuccess, 450);
            } else {
                Transitions.removeEffect(content);
            }
        });

        transitionSubscriptionPin = EasyBind.subscribe(model.getShouldTransitionToNextQuestion(), shouldTransition -> {
            if (shouldTransition) {
                showNextQuestionWithDelayAndAnimation();
            }
        });

        shouldShowWrongWordOverlayPin = EasyBind.subscribe(model.getShouldShowWrongWordOverlay(), shouldShow ->
            wrongWordOverlay.updateOverlayVisibility(content,
                    shouldShow,
                    controller::onKeyPressedWhileShowingOverlay));

        createWalletSuccessButton.setOnAction(e -> controller.onCreateWallet());
        goBackButton.setOnAction(e -> controller.onWrongWord());

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
        goBackButton.setOnAction(null);

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

    private void configCreateWalletSuccess() {
        VBox contentBox = getFeedbackContentBox();

        createWalletSuccess.setVisible(false);
        createWalletSuccess.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("wallet.verifySeeds.success.title"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("wallet.verifySeeds.success.description"));
        configFeedbackSubtitleLabel(subtitleLabel);

        createWalletSuccessButton.setDefaultButton(true);
        VBox.setMargin(createWalletSuccessButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headlineLabel, subtitleLabel, createWalletSuccessButton);
        createWalletSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private VBox getFeedbackContentBox() {
        VBox contentBox = new VBox(40);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("trade-wizard-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(FEEDBACK_WIDTH);
        return contentBox;
    }

    private void configFeedbackSubtitleLabel(Label subtitleLabel) {
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().addAll("bisq-text-21", "text-fill-grey-dimmed");
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

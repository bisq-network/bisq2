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
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SetupWalletWizardVerifyView extends View<StackPane, SetupWalletWizardVerifyModel, SetupWalletWizardVerifyController> {
    private static final int FEEDBACK_WIDTH = 700;
    private static final int ANSWER_BUTTONS_COUNT = 3;
    private static final int BUTTON_MIN_WIDTH = 160;
    private static final int CONTENT_SPACING = 10;
    private static final int BUTTON_SPACING = 20;

    private final VBox content;
    private final Label headlineLabel, questionLabel;
    private final HBox answerButtonsRow;
    private final Button[] answerButtons = new Button[ANSWER_BUTTONS_COUNT];
    private final Button nextWordButton;
    private final ChangeListener<Number> questionIndexListener, answerIndexListener;

    private final VBox createWalletSuccess;
    private final Button createWalletSuccessButton;
    private Subscription showCreateWalletSuccessPin;

    public SetupWalletWizardVerifyView(SetupWalletWizardVerifyModel model,
                                       SetupWalletWizardVerifyController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(CONTENT_SPACING);
        content.setAlignment(Pos.CENTER);

        headlineLabel = new Label(Res.get("wallet.verifySeeds.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headlineLabel, new Insets(0, 0, 20, 0));

        questionLabel = new Label();
        questionLabel.getStyleClass().add("bisq-text-1");
        VBox.setMargin(questionLabel, new Insets(0, 0, 20, 0));

        answerButtonsRow = new HBox(BUTTON_SPACING);
        answerButtonsRow.setAlignment(Pos.CENTER);
        VBox.setMargin(answerButtonsRow, new Insets(0, 0, 20, 0));

        for (int i = 0; i < ANSWER_BUTTONS_COUNT; i++) {
            Button btn = new Button();
            btn.setMinWidth(BUTTON_MIN_WIDTH);
            int idx = i;
            btn.setOnAction(e -> controller.onAnswerSelected(idx));
            answerButtons[i] = btn;
            answerButtonsRow.getChildren().add(btn);
        }

        nextWordButton = new Button(Res.get("wallet.verifySeeds.button.question.nextWord"));
        nextWordButton.setDefaultButton(true);
        nextWordButton.setOnAction(e -> controller.onNextWordSelected());

        questionIndexListener = (obs, oldVal, newVal) -> updateQuestion();
        answerIndexListener = (obs, oldVal, newVal) -> updateButtonStylesAndNextState();

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, questionLabel, answerButtonsRow, nextWordButton, Spacer.fillVBox());

        createWalletSuccessButton = new Button(Res.get("wallet.verifySeeds.button.success.nextStep"));
        createWalletSuccess = new VBox(20);
        configCreateWalletSuccess();
        StackPane.setMargin(createWalletSuccess, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));

        root.getChildren().addAll(content, createWalletSuccess);
    }

    @Override
    protected void onViewAttached() {
        model.getCurrentQuestionIndex().addListener(questionIndexListener);
        model.getSelectedAnswerIndex().addListener(answerIndexListener);
        updateQuestion();

        createWalletSuccessButton.setOnAction(e -> controller.onCreateWallet());

        showCreateWalletSuccessPin = EasyBind.subscribe(model.getCurrentScreenState(),
                state -> {
                    boolean show = state == SetupWalletWizardVerifyModel.ScreenState.SUCCESS;
                    createWalletSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(createWalletSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        showCreateWalletSuccessPin.unsubscribe();
        createWalletSuccessButton.setOnAction(null);
        model.getCurrentQuestionIndex().removeListener(questionIndexListener);
        model.getSelectedAnswerIndex().removeListener(answerIndexListener);
    }

    private void updateQuestion() {
        int qIdx = model.getCurrentQuestionIndex().get();
        model.getSelectedAnswerIndex().set(-1);
        if (qIdx >= model.getQuestionPositions().size()) {
            questionLabel.setText(Res.get("wallet.verifySeeds.success.title"));
            for (Button btn : answerButtons) btn.setVisible(false);
            return;
        }
        if (qIdx == SetupWalletWizardVerifyModel.QUESTIONS_COUNT - 1) { // 'Finish' for last button
            nextWordButton.setText(Res.get("wallet.verifySeeds.button.question.nextWord.last"));
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
            if (i == selected) {
                answerButtons[i].getStyleClass().add("outlined-button");
            } else {
                answerButtons[i].getStyleClass().remove("outlined-button");
            }
        }
        // Enable next button only if an answer is selected
        nextWordButton.setDisable(selected == -1);
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
        VBox contentBox = new VBox(20);
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
        subtitleLabel.getStyleClass().add("bisq-text-21");
    }
}
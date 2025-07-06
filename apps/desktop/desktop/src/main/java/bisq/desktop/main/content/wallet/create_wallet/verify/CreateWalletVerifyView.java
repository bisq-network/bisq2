package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.wallet.create_wallet.SeedState;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class CreateWalletVerifyView extends View<StackPane, CreateWalletVerifyModel, CreateWalletVerifyController> {
    private static final int ANSWER_BUTTONS_COUNT = 3;
    private static final int BUTTON_MIN_WIDTH = 160;
    private static final int CONTENT_SPACING = 20;
    private static final int BUTTON_SPACING = 20;

    private final VBox content;
    private final Label headlineLabel, questionLabel;
    private final HBox answerButtonsRow;
    private final Button nextWordButton;
    private final Button[] answerButtons = new Button[ANSWER_BUTTONS_COUNT];
    private final ChangeListener<Number> questionIndexListener, answerIndexListener;
    private final ChangeListener<CreateWalletVerifyModel.ScreenState> screenStateListener;

    public CreateWalletVerifyView(CreateWalletVerifyModel model,
                                  CreateWalletVerifyController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(CONTENT_SPACING);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));

        headlineLabel = new Label(Res.get("wallet.verifySeeds.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headlineLabel, new Insets(0, 0, 20, 0));

        questionLabel = new Label();
        questionLabel.getStyleClass().add("bisq-text-1");
        VBox.setMargin(questionLabel, new Insets(0, 0, 20, 0));

        answerButtonsRow = new HBox(BUTTON_SPACING);
        answerButtonsRow.setAlignment(Pos.CENTER);
        VBox.setMargin(answerButtonsRow, new Insets(0, 0, 40, 0));

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
        screenStateListener = (obs, oldVal, newVal) -> onScreenStateChanged(newVal);

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, questionLabel, answerButtonsRow, nextWordButton, Spacer.fillVBox());

        root.getChildren().add(content);
    }

    @Override
    protected void onViewAttached() {
        model.getCurrentQuestionIndex().addListener(questionIndexListener);
        model.getSelectedAnswerIndex().addListener(answerIndexListener);
        model.getCurrentScreenState().addListener(screenStateListener);
        updateQuestion();
        onScreenStateChanged(model.getCurrentScreenState().get());
    }

    @Override
    protected void onViewDetached() {
        model.getCurrentQuestionIndex().removeListener(questionIndexListener);
        model.getSelectedAnswerIndex().removeListener(answerIndexListener);
        model.getCurrentScreenState().removeListener(screenStateListener);
    }

    private void updateQuestion() {
        int qIdx = model.getCurrentQuestionIndex().get();
        model.getSelectedAnswerIndex().set(-1);
        if (qIdx >= model.getQuestionPositions().size()) {
            questionLabel.setText(Res.get("wallet.verifySeeds.success.title"));
            for (Button btn : answerButtons) btn.setVisible(false);
            return;
        }
        int pos = model.getQuestionPositions().get(qIdx);
        questionLabel.setText((qIdx + 1) + ". " + Res.get("wallet.verifySeeds.question", + (pos + 1)));
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
                answerButtons[i].setDefaultButton(true);
            } else {
                answerButtons[i].setDefaultButton(false);
            }
        }
        // Enable next button only if an answer is selected
        nextWordButton.setDisable(selected == -1);
    }

    private void onScreenStateChanged(CreateWalletVerifyModel.ScreenState newState) {
        content.getChildren().clear();
        switch (newState) {
            case WRONG:
                Label wrongLabel = new Label(Res.get("wallet.verifySeeds.wrongWord.title"));
                wrongLabel.getStyleClass().add("bisq-text-1");
                Label instructLabel = new Label(Res.get("wallet.verifySeeds.wrongWord.description"));
                instructLabel.getStyleClass().add("bisq-text-3");
                instructLabel.setWrapText(true);
                instructLabel.setTextAlignment(TextAlignment.CENTER);
                content.getChildren().clear();
                content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, wrongLabel, instructLabel, Spacer.fillVBox());
                break;
            case SUCCESS:
                Label successLabel = new Label(Res.get("wallet.verifySeeds.success.title"));
                successLabel.getStyleClass().add("bisq-text-1");
                Label successInstruct = new Label(Res.get("wallet.verifySeeds.success.description"));
                successInstruct.getStyleClass().add("bisq-text-3");
                successInstruct.setWrapText(true);
                successInstruct.setTextAlignment(TextAlignment.CENTER);
                content.getChildren().clear();
                content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, successLabel, successInstruct, Spacer.fillVBox());
                break;

            case QUIZ:
                // Restore quiz UI
                content.getChildren().clear();
                content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, questionLabel, answerButtonsRow, nextWordButton, Spacer.fillVBox());
                updateQuestion();
                break;

            case LOADING:
                Label loadingLabel = new Label(Res.get("wallet.loading"));
                loadingLabel.getStyleClass().add("bisq-text-1");
                root.getChildren().add(loadingLabel);
                break;

            case ERROR:
                Label errorLabel = new Label(Res.get("wallet.backupSeeds.error.failedToLoa"));
                errorLabel.getStyleClass().add("bisq-text-error");
                root.getChildren().addAll(errorLabel);
                break;
        }
    }
}
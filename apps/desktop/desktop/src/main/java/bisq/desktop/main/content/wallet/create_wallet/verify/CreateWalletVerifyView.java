package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
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

@Slf4j
public class CreateWalletVerifyView extends View<StackPane, CreateWalletVerifyModel, CreateWalletVerifyController> {
    private final VBox content;
    private final Label questionLabel;
    private final HBox answerButtonsRow;
    private final Button nextWordButton;
    private final Button[] answerButtons = new Button[3];
    private final ChangeListener<Number> questionIndexListener;

    public CreateWalletVerifyView(CreateWalletVerifyModel model,
                                  CreateWalletVerifyController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));

        questionLabel = new Label();
        questionLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        VBox.setMargin(questionLabel, new Insets(0, 0, 20, 0));

        answerButtonsRow = new HBox(20);
        answerButtonsRow.setAlignment(Pos.CENTER);
        VBox.setMargin(answerButtonsRow, new Insets(0, 0, 40, 0));

        for (int i = 0; i < 3; i++) {
            Button btn = new Button();
            btn.setMinWidth(160);
            btn.setStyle("-fx-font-size: 16px;");
            int idx = i;
            btn.setOnAction(e -> controller.onAnswerSelected(idx));
            answerButtons[i] = btn;
            answerButtonsRow.getChildren().add(btn);
        }

        nextWordButton = new Button("Next word");
        nextWordButton.setDefaultButton(true);
        nextWordButton.setOnAction(e -> controller.onNextWordSelected());

        content.getChildren().addAll(Spacer.fillVBox(), questionLabel, answerButtonsRow, nextWordButton, Spacer.fillVBox());

        root.getChildren().add(content);

        questionIndexListener = (obs, oldVal, newVal) -> updateQuestion();
    }

    @Override
    protected void onViewAttached() {
        model.getCurrentQuestionIndex().addListener(questionIndexListener);
        model.getSelectedAnswerIndex().addListener((obs, oldVal, newVal) -> updateButtonStylesAndNextState());
        model.getCurrentScreenState().addListener((obs, oldState, newState) -> onScreenStateChanged(newState));
        updateQuestion();
        // Ensure correct UI for current state
        // onScreenStateChanged(model.getCurrentScreenState().get());
    }

    @Override
    protected void onViewDetached() {
        model.getCurrentQuestionIndex().removeListener(questionIndexListener);
        root.setOnKeyPressed(null);
    }

    private void updateQuestion() {
        int qIdx = model.getCurrentQuestionIndex().get();
        model.getSelectedAnswerIndex().set(-1);
        if (qIdx >= model.getQuestionPositions().size()) {
            questionLabel.setText("All questions completed!");
            for (Button btn : answerButtons) btn.setVisible(false);
            return;
        }
        int pos = model.getQuestionPositions().get(qIdx);
        questionLabel.setText((qIdx + 1) + ". What is the seed word in position " + (pos + 1) + "?");
        for (int i = 0; i < 3; i++) {
            answerButtons[i].setText(model.getAnswerChoices().get(qIdx).get(i));
            answerButtons[i].setVisible(true);
            answerButtons[i].setDisable(false);
            answerButtons[i].setStyle("-fx-font-size: 16px;");
        }
        updateButtonStylesAndNextState();
    }

    private void updateButtonStylesAndNextState() {
        int selected = model.getSelectedAnswerIndex().get();
        for (int i = 0; i < 3; i++) {
            if (i == selected) {
                answerButtons[i].setStyle("-fx-font-size: 16px; -fx-background-color: #4caf50; -fx-text-fill: white;");
            } else {
                answerButtons[i].setStyle("-fx-font-size: 16px;");
            }
        }
        // Enable next button only if an answer is selected
        nextWordButton.setDisable(selected == -1);
    }

    private void onScreenStateChanged(CreateWalletVerifyModel.ScreenState newState) {
        content.getChildren().clear();
        switch (newState) {
            case WRONG:
                Label wrongLabel = new Label("Wrong word!");
                wrongLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
                Label instructLabel = new Label("Please go back to previous screen and write down 12 words properly");
                instructLabel.setStyle("-fx-font-size: 16px;");
                instructLabel.setWrapText(true);
                instructLabel.setTextAlignment(TextAlignment.CENTER);
                content.getChildren().clear();
                content.getChildren().addAll(Spacer.fillVBox(), wrongLabel, instructLabel, Spacer.fillVBox());
                break;
            case SUCCESS:
                Label successLabel = new Label("Success!");
                successLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #388e3c;");
                Label successInstruct = new Label("You verified 6 words and your wallet is initalized");
                successInstruct.setStyle("-fx-font-size: 16px;");
                successInstruct.setWrapText(true);
                successInstruct.setTextAlignment(TextAlignment.CENTER);
                content.getChildren().clear();
                content.getChildren().addAll(Spacer.fillVBox(), successLabel, successInstruct, Spacer.fillVBox());
                break;
            case QUIZ:
            default:
                // Restore quiz UI
                content.getChildren().clear();
                content.getChildren().addAll(Spacer.fillVBox(), questionLabel, answerButtonsRow, nextWordButton, Spacer.fillVBox());
                updateQuestion();
                break;
        }
    }
}
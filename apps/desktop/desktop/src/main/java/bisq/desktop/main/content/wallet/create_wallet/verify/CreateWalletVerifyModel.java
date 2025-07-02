package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class CreateWalletVerifyModel implements Model {
    private final IntegerProperty currentQuestionIndex = new SimpleIntegerProperty(0);
    private final List<Integer> questionPositions = new ArrayList<>(); // 0-based positions
    private final List<List<String>> answerChoices = new ArrayList<>();
    private final List<Integer> correctAnswerIndices = new ArrayList<>();
    private final StringProperty[] seedWords = new StringProperty[12];

    private final IntegerProperty selectedAnswerIndex = new SimpleIntegerProperty(-1);

    public enum ScreenState {
        QUIZ,
        WRONG,
        SUCCESS
    }

    private final ObjectProperty<ScreenState> currentScreenState = new SimpleObjectProperty<>(ScreenState.QUIZ);

    public CreateWalletVerifyModel() {
        for (int i = 0; i < 12; i++) {
            seedWords[i] = new SimpleStringProperty("");
        }
    }

    public void setupQuestions(List<String> allSeedWords) {
        questionPositions.clear();
        answerChoices.clear();
        correctAnswerIndices.clear();
        // Generate 6 unique random positions (0-11)
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < 12; i++) positions.add(i);
        Collections.shuffle(positions);
        questionPositions.addAll(positions.subList(0, 6));
        for (int q = 0; q < 6; q++) {
            int correctPos = questionPositions.get(q);
            String correctWord = allSeedWords.get(correctPos);
            // Pick 2 random incorrect words
            List<String> choices = new ArrayList<>();
            choices.add(correctWord);
            List<String> incorrect = new ArrayList<>(allSeedWords);
            incorrect.remove(correctWord);
            Collections.shuffle(incorrect);
            choices.add(incorrect.get(0));
            choices.add(incorrect.get(1));
            Collections.shuffle(choices);
            answerChoices.add(choices);
            correctAnswerIndices.add(choices.indexOf(correctWord));
        }
        selectedAnswerIndex.set(-1);
    }
} 
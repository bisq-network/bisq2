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
public class SetupWalletWizardVerifyModel implements Model {
    public static final int QUESTIONS_COUNT = 6;
    static final int INVALID_INDEX = -1;
    private static final int SEED_WORD_COUNT = 12;

    private final IntegerProperty currentQuestionIndex = new SimpleIntegerProperty(0);
    private final List<Integer> questionPositions = new ArrayList<>(); // 0-based positions
    private final List<List<String>> answerChoices = new ArrayList<>();
    private final List<Integer> correctAnswerIndices = new ArrayList<>();
    private final StringProperty[] seedWords = new StringProperty[SEED_WORD_COUNT];

    private final IntegerProperty selectedAnswerIndex = new SimpleIntegerProperty(INVALID_INDEX);
    private final BooleanProperty shouldTransitionToNextQuestion = new SimpleBooleanProperty(false);

    public enum ScreenState {
        QUIZ,
        SUCCESS
    }

    private final ObjectProperty<ScreenState> currentScreenState = new SimpleObjectProperty<>(ScreenState.QUIZ);

    public SetupWalletWizardVerifyModel() {
        for (int i = 0; i < SEED_WORD_COUNT; i++) {
            seedWords[i] = new SimpleStringProperty("");
        }
    }

    public void setupQuestions(List<String> allSeedWords) {
        questionPositions.clear();
        answerChoices.clear();
        correctAnswerIndices.clear();
        // Generate QUESTIONS_COUNT unique random positions (0-11)
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < SEED_WORD_COUNT; i++) positions.add(i);
        Collections.shuffle(positions);
        questionPositions.addAll(positions.subList(0, QUESTIONS_COUNT));
        for (int q = 0; q < QUESTIONS_COUNT; q++) {
            int correctPos = questionPositions.get(q);
            String correctWord = allSeedWords.get(correctPos);

            List<String> choices = new ArrayList<>();
            choices.add(correctWord);

            // Pick 2 random incorrect words            
            List<String> incorrect = new ArrayList<>(allSeedWords);
            incorrect.remove(correctWord);
            Collections.shuffle(incorrect);
            choices.add(incorrect.get(0));
            choices.add(incorrect.get(1));

            Collections.shuffle(choices);
            answerChoices.add(choices);
            correctAnswerIndices.add(choices.indexOf(correctWord));
        }
        selectedAnswerIndex.set(INVALID_INDEX);
        currentQuestionIndex.set(0);
    }

    public void reset() {
        currentQuestionIndex.set(0);
        selectedAnswerIndex.set(-1);
    }
}

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
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
public class SetupWalletWizardVerifyModel implements Model {
    public static final int QUESTIONS_COUNT = 6;
    static final int INVALID_INDEX = -1;

    private final IntegerProperty currentQuestionIndex = new SimpleIntegerProperty(0);
    private final List<Integer> questionPositions = new ArrayList<>(); // 0-based positions
    private final List<List<String>> answerChoices = new ArrayList<>();
    private final List<Integer> correctAnswerIndices = new ArrayList<>();

    private final IntegerProperty selectedAnswerIndex = new SimpleIntegerProperty(INVALID_INDEX);
    private final BooleanProperty shouldTransitionToNextQuestion = new SimpleBooleanProperty(false);
    private final BooleanProperty shouldShowWrongWordOverlay = new SimpleBooleanProperty(false);
    private final BooleanProperty questionsReady = new SimpleBooleanProperty(false);

    public enum ScreenState {
        QUIZ,
        SUCCESS
    }

    private final ObjectProperty<ScreenState> currentScreenState = new SimpleObjectProperty<>(ScreenState.QUIZ);

    public void setupQuestions(List<String> allSeedWords) {
        setupQuestions(allSeedWords, new Random());
    }

    void setupQuestions(List<String> allSeedWords, Random random) {
        questionPositions.clear();
        answerChoices.clear();
        correctAnswerIndices.clear();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < allSeedWords.size(); i++) {
            positions.add(i);
        }
        Collections.shuffle(positions, random);
        questionPositions.addAll(positions.subList(0, QUESTIONS_COUNT));
        for (int q = 0; q < QUESTIONS_COUNT; q++) {
            int correctPos = questionPositions.get(q);
            String correctWord = allSeedWords.get(correctPos);

            List<String> choices = new ArrayList<>();
            choices.add(correctWord);

            // A phrase may repeat a word, so the two wrong choices come from the distinct other words
            List<String> incorrect = new ArrayList<>(allSeedWords.stream().distinct().filter(word -> !word.equals(correctWord)).toList());
            Collections.shuffle(incorrect, random);
            choices.add(incorrect.get(0));
            choices.add(incorrect.get(1));

            Collections.shuffle(choices, random);
            answerChoices.add(choices);
            correctAnswerIndices.add(choices.indexOf(correctWord));
        }
        selectedAnswerIndex.set(INVALID_INDEX);
        currentQuestionIndex.set(0);
        questionsReady.set(true);
    }

    public void reset() {
        questionsReady.set(false);
        currentQuestionIndex.set(0);
        selectedAnswerIndex.set(INVALID_INDEX);
        shouldTransitionToNextQuestion.set(false);
        shouldShowWrongWordOverlay.set(false);
    }
}

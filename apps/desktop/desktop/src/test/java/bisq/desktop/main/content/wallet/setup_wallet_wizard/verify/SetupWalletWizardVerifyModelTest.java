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

import bisq.desktop.main.content.wallet.setup_wallet_wizard.SeedWords;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupWalletWizardVerifyModelTest {
    @Test
    void questionsCoverTheWordsTheDaemonReturned() {
        for (int count : List.of(12, 24)) {
            List<String> words = IntStream.range(0, count).mapToObj(i -> "word" + i).toList();
            SetupWalletWizardVerifyModel model = new SetupWalletWizardVerifyModel();

            model.setupQuestions(words);

            assertEquals(SetupWalletWizardVerifyModel.QUESTIONS_COUNT, model.getQuestionPositions().size());
            assertEquals(SetupWalletWizardVerifyModel.QUESTIONS_COUNT, new HashSet<>(model.getQuestionPositions()).size());
            assertTrue(model.getQuestionPositions().stream().allMatch(p -> p >= 0 && p < count));
            for (int q = 0; q < SetupWalletWizardVerifyModel.QUESTIONS_COUNT; q++) {
                List<String> choices = model.getAnswerChoices().get(q);
                String correct = words.get(model.getQuestionPositions().get(q));
                assertEquals(3, choices.size());
                assertEquals(correct, choices.get(model.getCorrectAnswerIndices().get(q)));
            }
            assertTrue(model.getQuestionsReady().get());
        }
    }

    @Test
    void aLongerPhraseIsQuizzedBeyondItsFirstTwelveWords() {
        List<String> words = IntStream.range(0, 24).mapToObj(i -> "word" + i).toList();
        SetupWalletWizardVerifyModel model = new SetupWalletWizardVerifyModel();

        model.setupQuestions(words, new Random(1));

        assertTrue(model.getQuestionPositions().stream().anyMatch(p -> p >= 12));
    }

    @Test
    void aRepeatedWordIsNeverOfferedTwiceInOneQuestion() {
        List<String> words = List.of("echo", "alpha", "echo", "bravo", "echo", "charlie",
                "echo", "delta", "echo", "foxtrot", "echo", "golf");
        for (int seed = 0; seed < 50; seed++) {
            SetupWalletWizardVerifyModel model = new SetupWalletWizardVerifyModel();

            model.setupQuestions(words, new Random(seed));

            for (int q = 0; q < SetupWalletWizardVerifyModel.QUESTIONS_COUNT; q++) {
                List<String> choices = model.getAnswerChoices().get(q);
                assertEquals(3, new HashSet<>(choices).size(), "seed " + seed + " question " + q + ": " + choices);
                assertEquals(words.get(model.getQuestionPositions().get(q)), choices.get(model.getCorrectAnswerIndices().get(q)));
            }
        }
    }

    @Test
    void onlyBip39WordCountsAreAccepted() {
        for (int count : List.of(12, 15, 18, 21, 24)) {
            assertTrue(SeedWords.isValidCount(count), "count " + count);
        }
        for (int count : List.of(0, 9, 11, 13, 27)) {
            assertFalse(SeedWords.isValidCount(count), "count " + count);
        }
        assertThrows(IllegalArgumentException.class, () -> SeedWords.requireValidCount(List.of("a", "b", "c")));
    }
}

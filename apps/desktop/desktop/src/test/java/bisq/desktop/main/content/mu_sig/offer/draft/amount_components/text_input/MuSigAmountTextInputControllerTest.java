/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.mu_sig.offer.draft.amount_components.text_input;

import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class MuSigAmountTextInputControllerTest extends TestFxHeadlessSupport {
    private final List<Optional<Monetary>> userEdits = new ArrayList<>();
    private MuSigAmountTextInputController controller;
    private TextField textField;
    private Button otherFocusTarget;

    @Start
    void start(Stage stage) {
        controller = new MuSigAmountTextInputController(true, false);
        controller.setUserEditHandler(userEdits::add);
        otherFocusTarget = new Button("elsewhere");
        VBox root = new VBox(controller.getView().getRoot(), otherFocusTarget);
        stage.setScene(new Scene(root, 640, 480));
        stage.show();
        stage.toFront();
        controller.onActivate();
        textField = (TextField) controller.getView().getRoot().lookup(".text-field");
    }

    @Test
    void caretMovesAndProgrammaticSelectionNeverCommitAnAmount(FxRobot robot) {
        // 100.1234 renders as 100.12; a selection-only change must not feed that rounded text back.
        robot.interact(() -> controller.setAmount(Fiat.fromFaceValue(100.1234, "USD")));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(textField.getText()).isEqualTo("100.12");
        userEdits.clear();

        robot.interact(() -> {
            textField.positionCaret(1);
            textField.selectRange(0, 3);
            textField.selectRange(textField.getLength(), textField.getLength());
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(userEdits).as("selection changes are not user edits").isEmpty();
        assertThat(controller.amountProperty().get().getValue())
                .as("the domain amount keeps its full precision")
                .isEqualTo(Fiat.fromFaceValue(100.1234, "USD").getValue());
    }

    @Test
    void blurRendersTheAuthoritativeAmountEvenWhenTheDomainInstanceIsUnchanged(FxRobot robot) {
        // 400 is the maximum and already rendered. Typing 500 is clamped back to the very same
        // 400 instance, so the formatter's value does not change and its own text update never
        // runs; the field must still be reconciled when editing finishes.
        Monetary maximum = Fiat.fromFaceValue(400, "USD");
        robot.interact(() -> controller.setAmount(maximum));
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> {
            textField.requestFocus();
            textField.selectAll();
        });
        robot.write("500");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(userEdits.get(userEdits.size() - 1)).contains(Fiat.fromFaceValue(500, "USD"));
        robot.interact(() -> controller.setAmount(maximum));
        WaitForAsyncUtils.waitForFxEvents();
        userEdits.clear();

        robot.interact(() -> otherFocusTarget.requestFocus());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(textField.getText()).isEqualTo("400.00");
        assertThat(controller.amountProperty().get()).isSameAs(maximum);
        assertThat(userEdits).isEmpty();
    }

    @Test
    void blurRendersTheAuthoritativeAmountWithoutEmittingAnEdit(FxRobot robot) {
        robot.interact(() -> controller.setAmount(Fiat.fromFaceValue(300, "USD")));
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> {
            textField.requestFocus();
            textField.selectAll();
        });
        robot.write("400");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(userEdits.get(userEdits.size() - 1)).contains(Fiat.fromFaceValue(400, "USD"));

        // The domain clamps the typed 400 to 500 while the field is still focused.
        robot.interact(() -> controller.setAmount(Fiat.fromFaceValue(500, "USD")));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(textField.getText()).as("an in-progress edit is not overwritten").isEqualTo("400");
        userEdits.clear();

        robot.interact(() -> otherFocusTarget.requestFocus());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(textField.getText()).as("editing finished: show the amount used downstream").isEqualTo("500.00");
        assertThat(controller.amountProperty().get()).isEqualTo(Fiat.fromFaceValue(500, "USD"));
        assertThat(userEdits).as("rendering the authoritative amount is not a user edit").isEmpty();
    }
}

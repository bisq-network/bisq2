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

package bisq.desktop.components.controls;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UnorderedListTest {
    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX already initialized.
        }
    }

    @Test
    @DisplayName("parses preamble list and trailing paragraph")
    void parses_preamble_list_and_trailing_paragraph() throws Exception {
        String text = "Most fiat payment methods carry a risk of bank chargebacks.\n"
                + "  Trade limits are determined by several factors:\n"
                + "  - The chargeback risk associated with the selected payment method\n"
                + "  - Whether the buyer has imported a Bisq 1 signed account age witness\n"
                + "  - The age of the payment account\n"
                + "  - The buyer's reputation\n\n"
                + "  The next section explains how these factors impact the trade limits.";

        UnorderedList list = runOnFxThread(() -> new UnorderedList(text, "bisq-text-13"));

        assertEquals(6, list.getChildren().size());
        assertInstanceOf(TextFlow.class, list.getChildren().get(0));
        assertInstanceOf(HBox.class, list.getChildren().get(1));
        assertInstanceOf(HBox.class, list.getChildren().get(2));
        assertInstanceOf(HBox.class, list.getChildren().get(3));
        assertInstanceOf(HBox.class, list.getChildren().get(4));
        assertInstanceOf(TextFlow.class, list.getChildren().get(5));

        assertEquals("Most fiat payment methods carry a risk of bank chargebacks.\nTrade limits are determined by several factors:",
                textFromFlow((TextFlow) list.getChildren().get(0)));

        assertEquals("The chargeback risk associated with the selected payment method",
                listItemText(list.getChildren().get(1)));
        assertEquals("Whether the buyer has imported a Bisq 1 signed account age witness",
                listItemText(list.getChildren().get(2)));
        assertEquals("The age of the payment account",
                listItemText(list.getChildren().get(3)));
        assertEquals("The buyer's reputation",
                listItemText(list.getChildren().get(4)));

        assertEquals("\nThe next section explains how these factors impact the trade limits.",
                textFromFlow((TextFlow) list.getChildren().get(5)));
    }

    @Test
    @DisplayName("parses inline bullets without leading paragraph")
    void parses_inline_bullets_without_leading_paragraph() throws Exception {
        String text = "- First item. - Second item.";
        UnorderedList list = runOnFxThread(() -> new UnorderedList(text, "bisq-text-13"));

        assertEquals(2, list.getChildren().size());
        assertEquals("First item.", listItemText(list.getChildren().get(0)));
        assertEquals("Second item.", listItemText(list.getChildren().get(1)));
    }

    @Test
    @DisplayName("renders single paragraph when no list markers present")
    void renders_single_paragraph_when_no_list_markers_present() throws Exception {
        String text = "No list markers here.";
        UnorderedList list = runOnFxThread(() -> new UnorderedList(text, "bisq-text-13"));

        assertEquals(1, list.getChildren().size());
        assertInstanceOf(TextFlow.class, list.getChildren().get(0));
        assertEquals("No list markers here.", textFromFlow((TextFlow) list.getChildren().get(0)));
    }

    @Test
    @DisplayName("parses indented bullets with blank lines")
    void parses_indented_bullets_with_blank_lines() throws Exception {
        String text = "  - First item\n"
                + "\n"
                + "  - Second item\n"
                + "\n";

        UnorderedList list = runOnFxThread(() -> new UnorderedList(text, "bisq-text-13"));

        assertEquals(2, list.getChildren().size());
        assertEquals("First item", listItemText(list.getChildren().get(0)));
        assertEquals("Second item", listItemText(list.getChildren().get(1)));
    }

    @Test
    @DisplayName("parses bullets at start without indent")
    void parses_bullets_at_start_without_indent() throws Exception {
        String text = "- First item\n"
                + "- Second item\n"
                + "- Third item";

        UnorderedList list = runOnFxThread(() -> new UnorderedList(text, "bisq-text-13"));

        assertEquals(3, list.getChildren().size());
        assertEquals("First item", listItemText(list.getChildren().get(0)));
        assertEquals("Second item", listItemText(list.getChildren().get(1)));
        assertEquals("Third item", listItemText(list.getChildren().get(2)));
    }

    @Test
    @DisplayName("parses bullets with no space after dash and no indent")
    void parses_bullets_with_no_space_after_dash_and_no_indent() throws Exception {
        String text = "-First item\n"
                + "-Second item";

        UnorderedList list = runOnFxThread(() -> new UnorderedList(text, "bisq-text-13", 7, 0, "-\\s*", UnorderedList.BULLET_SYMBOL));

        assertEquals(2, list.getChildren().size());
        assertEquals("First item", listItemText(list.getChildren().get(0)));
        assertEquals("Second item", listItemText(list.getChildren().get(1)));
    }

    private static String listItemText(Node node) {
        HBox box = (HBox) node;
        Text mark = (Text) box.getChildren().get(0);
        assertEquals(UnorderedList.BULLET_SYMBOL, mark.getText());
        TextFlow flow = (TextFlow) box.getChildren().get(1);
        return textFromFlow(flow);
    }

    private static String textFromFlow(TextFlow flow) {
        Text text = (Text) flow.getChildren().get(0);
        return text.getText();
    }

    private static <T> T runOnFxThread(Callable<T> task) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return task.call();
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(task.call());
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        if (!completed) {
            throw new IllegalStateException("Timed out waiting for FX thread.");
        }
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }
}

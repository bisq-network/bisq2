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

class OrderedListTest {
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
    @DisplayName("parses ordered list with preamble and trailing paragraph")
    void parses_ordered_list_with_preamble_and_trailing_paragraph() throws Exception {
        String text = "Intro paragraph.\n"
                + "  1. First item\n"
                + "  2. Second item\n\n"
                + "  Closing paragraph.";

        OrderedList list = runOnFxThread(() -> new OrderedList(text, "bisq-text-13", 7, 0));

        assertEquals(4, list.getChildren().size());
        assertInstanceOf(TextFlow.class, list.getChildren().get(0));
        assertInstanceOf(HBox.class, list.getChildren().get(1));
        assertInstanceOf(HBox.class, list.getChildren().get(2));
        assertInstanceOf(TextFlow.class, list.getChildren().get(3));

        assertEquals("Intro paragraph.", textFromFlow((TextFlow) list.getChildren().get(0)));
        assertEquals("First item", listItemText(list.getChildren().get(1), "1."));
        assertEquals("Second item", listItemText(list.getChildren().get(2), "2."));
        assertEquals("\nClosing paragraph.", textFromFlow((TextFlow) list.getChildren().get(3)));
    }

    @Test
    @DisplayName("parses inline ordered items without leading paragraph")
    void parses_inline_ordered_items_without_leading_paragraph() throws Exception {
        String text = "1. First item 2. Second item 3. Third item";
        OrderedList list = runOnFxThread(() -> new OrderedList(text, "bisq-text-13", 7, 0));

        assertEquals(3, list.getChildren().size());
        assertEquals("First item", listItemText(list.getChildren().get(0), "1."));
        assertEquals("Second item", listItemText(list.getChildren().get(1), "2."));
        assertEquals("Third item", listItemText(list.getChildren().get(2), "3."));
    }

    @Test
    @DisplayName("renders single paragraph when no numbered markers present")
    void renders_single_paragraph_when_no_numbered_markers_present() throws Exception {
        String text = "Just a paragraph.";
        OrderedList list = runOnFxThread(() -> new OrderedList(text, "bisq-text-13", 7, 0));

        assertEquals(1, list.getChildren().size());
        assertInstanceOf(TextFlow.class, list.getChildren().get(0));
        assertEquals("Just a paragraph.", textFromFlow((TextFlow) list.getChildren().get(0)));
    }

    @Test
    @DisplayName("parses numbered items starting at ten")
    void parses_numbered_items_starting_at_ten() throws Exception {
        String text = "10. Ten\n"
                + "11. Eleven\n"
                + "12. Twelve";

        OrderedList list = runOnFxThread(() -> new OrderedList(text, "bisq-text-13", 7, 0));

        assertEquals(3, list.getChildren().size());
        assertEquals("Ten", listItemText(list.getChildren().get(0), "1."));
        assertEquals("Eleven", listItemText(list.getChildren().get(1), "2."));
        assertEquals("Twelve", listItemText(list.getChildren().get(2), "3."));
    }

    @Test
    @DisplayName("parses items without space after dot")
    void parses_items_without_space_after_dot() throws Exception {
        String text = "1.First\n"
                + "2.Second\n"
                + "3.Third";

        OrderedList list = runOnFxThread(() -> new OrderedList(text, "bisq-text-13", 7, 0));

        assertEquals(3, list.getChildren().size());
        assertEquals("First", listItemText(list.getChildren().get(0), "1."));
        assertEquals("Second", listItemText(list.getChildren().get(1), "2."));
        assertEquals("Third", listItemText(list.getChildren().get(2), "3."));
    }

    private static String listItemText(Node node, String expectedMark) {
        HBox box = (HBox) node;
        Text mark = (Text) box.getChildren().get(0);
        assertEquals(expectedMark, mark.getText());
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

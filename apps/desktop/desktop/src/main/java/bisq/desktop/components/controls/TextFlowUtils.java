package bisq.desktop.components.controls;

import bisq.common.data.Pair;
import bisq.common.util.StringUtils;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

public final class TextFlowUtils {
    /**
     * Updates the given TextFlow with styled text segments.
     *
     * @param textFlow The TextFlow to update.
     * @param text     The text with style information.
     */
    public static void updateTextFlow(TextFlow textFlow, String text) {
        textFlow.getChildren().clear();

        // Split the text into styled segments
        List<Pair<String, List<String>>> styledSegments = StringUtils.getTextStylePairs(text);

        for (Pair<String, List<String>> segment : styledSegments) {
            String segmentText = segment.getFirst();
            List<String> styles = segment.getSecond();

            if (segmentText != null && !segmentText.isEmpty()) {
                Text styledText = new Text(segmentText);

                // Apply styles to the Text node
                if (styles != null) {
                    for (String style : styles) {
                        if (style != null && !style.isEmpty()) {
                            styledText.getStyleClass().add(style);
                        }
                    }
                } else {
                    // Apply a default style if no specific styles are present
                    styledText.getStyleClass().add("trade-wizard-review-value");
                }

                // Add the styled Text node to the TextFlow
                textFlow.getChildren().add(styledText);
            }
        }
    }
}

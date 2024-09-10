package bisq.desktop.components.controls;

import bisq.common.data.Pair;
import bisq.common.util.StringUtils;
import javafx.collections.ObservableList;
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
        ObservableList<String> styleClasses = textFlow.getStyleClass();

        // Split the text into styled segments
        List<Pair<String, List<String>>> styledSegments = StringUtils.getTextStylePairs(text);

        for (Pair<String, List<String>> segment : styledSegments) {
            String segmentText = segment.getFirst();
            List<String> styles = segment.getSecond();

            if (segmentText != null && !segmentText.isEmpty()) {
                Text styledText = new Text(segmentText);
                styledText.getStyleClass().addAll(styleClasses);

                // Apply styles to the Text node
                if (styles != null) {
                    for (String style : styles) {
                        if (style != null && !style.isEmpty()) {
                            styledText.getStyleClass().add(style);
                        }
                    }
                }

                // Add the styled Text node to the TextFlow
                textFlow.getChildren().add(styledText);
            }
        }
    }
}

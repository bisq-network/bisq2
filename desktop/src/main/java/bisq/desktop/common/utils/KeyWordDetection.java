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

package bisq.desktop.common.utils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.text.BreakIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KeyWordDetection {
    public static StyleSpans<Collection<String>> getStyleSpans(String text, List<String> customTags) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        BreakIterator iterator = BreakIterator.getWordInstance();
        iterator.setText(text);
        Set<String> customTagUppercase = customTags.stream().map(String::toUpperCase).collect(Collectors.toSet());
        int lastIndex = iterator.first();
        int lastKwEnd = 0;
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = iterator.next();
            if (lastIndex != BreakIterator.DONE) {
                String word = text.substring(firstIndex, lastIndex).toUpperCase();
                if (customTagUppercase.contains(word)) {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-customTags"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else if (word.matches("[0-9]{1,13}(\\.[0-9]*)?")) {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-customTags"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-none"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                }
            }
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
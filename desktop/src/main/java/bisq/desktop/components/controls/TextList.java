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

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j

public abstract class TextList extends VBox {
    public static final String LIST_INDICATOR = "- ";
    private static final String BULLET_POINT = "•";

    public TextList(String text, String style) {
        this(text, style, 5, 0);
    }

    public TextList(String text, @Nullable String style, double gap, double vSpacing) {
        setSpacing(vSpacing);
        List<String> list = List.of(text.split(getRegex()));
        int i = 0;
        for (String item : list) {
            String textContent = item.stripLeading();
            if (textContent.isEmpty()) {
                continue;
            }
            i++;
            textContent = textContent.stripTrailing();
            MultiLineLabel content = new MultiLineLabel(textContent);
            Text mark = new Text(getMark(i));
            if (style != null) {
                mark.getStyleClass().add(style);
                content.getStyleClass().add(style);
            }
            HBox.setHgrow(mark, Priority.ALWAYS);
            getChildren().add(new HBox(gap, mark, content));
        }
    }

    protected abstract String getRegex();

    protected abstract String getMark(int index);
}
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

import bisq.common.data.Pair;
import bisq.common.util.StringUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.List;

// TODO (refactor, low prio) use TextFlow
@Slf4j
public class MultiStyleLabelPane extends HBox {
    private final StringProperty text = new SimpleStringProperty();

    public StringProperty textProperty() {
        return text;
    }

    public final void setText(String value) {
        text.set(value);
    }

    public final String getText() {
        return text.get();
    }

    public MultiStyleLabelPane() {
        text.addListener(new WeakReference<>((ChangeListener<String>) (observable, oldValue, newValue) -> {
            setAlignment(Pos.TOP_LEFT);
            List<Pair<String, List<String>>> result = StringUtils.getTextStylePairs(newValue);
            if (result.isEmpty()) {
                Label label = new Label(newValue);
                if (!getStyleClass().isEmpty()) {
                    label.getStyleClass().addAll(getStyleClass());
                }
                getChildren().setAll(label);
            } else {
                getChildren().clear();
                result.forEach(pair -> {
                    String text = pair.getFirst();
                    if (text != null && !text.isEmpty()) {
                        Label label = new Label(text);
                        if (!getStyleClass().isEmpty()) {
                            label.getStyleClass().addAll(getStyleClass());
                        }
                        List<String> styles = pair.getSecond();
                        if (styles != null) {
                            styles.stream()
                                    .filter(style -> style != null && !style.isEmpty())
                                    .forEach(style -> label.getStyleClass().add(style));
                        }
                        HBox.setHgrow(label, Priority.ALWAYS);
                        getChildren().add(label);
                    }
                });
            }
        }).get());
    }

    public MultiStyleLabelPane(String text) {
        this();
        this.text.set(text);
    }
}
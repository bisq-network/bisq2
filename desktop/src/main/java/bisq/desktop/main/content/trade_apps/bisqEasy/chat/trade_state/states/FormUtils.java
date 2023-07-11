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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.MultiLineLabel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class FormUtils {
    public static Label getLabel(String text) {
        MultiLineLabel label = new MultiLineLabel(text);
        label.getStyleClass().add("bisq-easy-trade-state-info-text");
        label.setWrapText(true);
        VBox.setMargin(label, new Insets(10, 0, 0, 0));
        VBox.setVgrow(label, Priority.ALWAYS);
        return label;
    }

    public static MultiLineLabel getHelpLabel(String text) {
        MultiLineLabel label = new MultiLineLabel(text);
        label.getStyleClass().add("bisq-easy-trade-state-info-help-text");
        label.setWrapText(true);
        VBox.setMargin(label, new Insets(10, 0, 0, 0));
        VBox.setVgrow(label, Priority.ALWAYS);
        return label;
    }

    public static MaterialTextField getTextField(String description, String value, boolean isEditable) {
        MaterialTextField field = new MaterialTextField(description, null);
        field.setText(value);
        field.showCopyIcon();
        field.setEditable(isEditable);
        VBox.setMargin(field, new Insets(0, 0, 0, 0));
        if (isEditable) {
            UIThread.runOnNextRenderFrame(field::requestFocus);
        }
        return field;
    }

    public static MaterialTextArea addTextArea(String description, String value, boolean isEditable) {
        MaterialTextArea field = new MaterialTextArea(description, null);
        field.setText(value);
        field.showCopyIcon();
        field.setEditable(isEditable);
        field.setFixedHeight(107);
        VBox.setMargin(field, new Insets(0, 0, 0, 0));
        if (isEditable) {
            UIThread.runOnNextRenderFrame(field::requestFocus);
        }
        return field;
    }

    public static Region getVLine() {
        Region separator = Layout.vLine();
        separator.setMinHeight(10);
        separator.setMaxHeight(separator.getMinHeight());
        VBox.setMargin(separator, new Insets(5, 0, 5, 17));
        return separator;
    }
}
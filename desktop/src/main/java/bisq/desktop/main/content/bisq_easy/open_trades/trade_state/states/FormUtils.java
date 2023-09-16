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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.common.data.Pair;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;

public class FormUtils {
    public static WrappingText getHeadline() {
        return getHeadline(null);
    }

    public static WrappingText getHeadline(@Nullable String text) {
        return new WrappingText(text, "bisq-easy-trade-state-headline");
    }

    public static WrappingText getInfo() {
        return getInfo(null);
    }

    public static WrappingText getInfo(@Nullable String text) {
        return new WrappingText(text, "bisq-easy-trade-state-info");
    }

    public static WrappingText getHelp() {
        return getHelp(null);
    }

    public static WrappingText getHelp(@Nullable String text) {
        return new WrappingText(text, "bisq-easy-trade-state-help");
    }

    public static Pair<WrappingText, HBox> getConfirmHeadline() {
        return getConfirmHeadline(null);
    }

    public static Pair<WrappingText, HBox> getConfirmHeadline(@Nullable String text) {
        WrappingText headline = FormUtils.getHeadline(text);
        HBox hBox = new HBox(10, ImageUtil.getImageViewById("check-circle"), headline);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return new Pair<>(headline, hBox);
    }

    public static Pair<WrappingText, HBox> getConfirmInfo() {
        return getConfirmInfo(null);
    }

    public static Pair<WrappingText, HBox> getConfirmInfo(@Nullable String text) {
        WrappingText headline = FormUtils.getInfo(text);
        HBox hBox = new HBox(10, ImageUtil.getImageViewById("check-circle"), headline);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return new Pair<>(headline, hBox);
    }


    public static Label getLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bisq-easy-trade-state-info");
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
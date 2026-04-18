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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.text;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BaselineHBox;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.MuSigAmountInputFontSizeHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

import static bisq.common.encoding.UniCodeTable.EN_DASH_SYMBOL;

@Slf4j
public class MuSigAmountTextInputView extends View<HBox, MuSigAmountTextInputModel, MuSigAmountTextInputController> {
    private static final double HEIGHT = 70;
    private static final double DASH_PADDING = 5;
    private static final double CODE_WIDTH = 30;
    private static final double CODE_SPACING = 10;


    private final Label code;
    private final TextField textField;
    private final Label dash;
    private double lastSize = 0;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigAmountTextInputView(MuSigAmountTextInputModel model,
                                    MuSigAmountTextInputController controller) {
        super(new HBox(CODE_SPACING), model, controller);

        root.getStyleClass().add("amount-input");
        root.setAlignment(Pos.CENTER);
        root.setMinHeight(HEIGHT);
        root.setMaxHeight(HEIGHT);

        textField = new TextField();
        textField.setTextFormatter(model.getTextFormatter());
        textField.setAlignment(Pos.BASELINE_RIGHT);
        textField.getStyleClass().add("value");
        textField.setPadding(new Insets(0));
        textField.setMinWidth(10);

        code = new Label();
        code.setMinWidth(CODE_WIDTH);
        code.setMaxWidth(CODE_WIDTH);
        code.setAlignment(Pos.BASELINE_LEFT);
        code.getStyleClass().add("code");
        code.setPadding(new Insets(0));

        dash = new Label(EN_DASH_SYMBOL);
        dash.getStyleClass().add("amount-input-dash");
        dash.setPadding(new Insets(0));

        double baseline = 54;
        BaselineHBox textInputNode = new BaselineHBox(textField, baseline);
        if (model.isLeftSideRangeAmount()) {
            BaselineHBox dashNode = new BaselineHBox(dash, baseline);
            HBox.setMargin(dashNode, new Insets(0, 3, 0, -2));
            root.getChildren().addAll(textInputNode, dashNode);
        } else {
            BaselineHBox codeLabelNode = new BaselineHBox(code, baseline);
            root.getChildren().addAll(textInputNode, codeLabelNode);
        }
    }

    @Override
    protected void onViewAttached() {
        code.textProperty().bind(model.getCode());
        model.getFocusedProperty().bind(textField.focusedProperty());
        subscriptions.add(EasyBind.subscribe(model.getSumOfNumChars(), sumOfNumChars -> {
            if (sumOfNumChars != null) {
                updateFontsize(sumOfNumChars.intValue());
            }
        }));


        subscriptions.add(EasyBind.subscribe(model.getAmountFieldWidth(), width -> {
            if (width != null) {
                textField.setPrefWidth(width.doubleValue());
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getDashFieldWidth(), width -> {
            if (width != null) {
                dash.setPrefWidth(width.doubleValue());
            }
        }));

        if (model.isFixedAmount() || !model.isLeftSideRangeAmount()) {
            UIThread.run(() -> {
                textField.requestFocus();
                textField.selectRange(textField.getLength(), textField.getLength());
            });
        }
    }


    private void updateFontsize(int length) {
        double size = MuSigAmountInputFontSizeHelper.computeFontSize(length);
        if (Math.abs(size - lastSize) > 0.1) {
            String style = "-fx-font-size: " + size + "em;";
            textField.setStyle(style);
            dash.setStyle(style);
            lastSize = size;
        }
    }

    @Override
    protected void onViewDetached() {
        code.textProperty().unbind();
        model.getFocusedProperty().unbind();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }
}

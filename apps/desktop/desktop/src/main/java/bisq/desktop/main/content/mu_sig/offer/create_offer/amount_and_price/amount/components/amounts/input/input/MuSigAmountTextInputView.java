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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.input;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BaselineHBox;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout;
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
import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout.CODE_SPACING;
import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout.CODE_WIDTH;

@Slf4j
public class MuSigAmountTextInputView extends View<HBox, MuSigAmountTextInputModel, MuSigAmountTextInputController> {
    private static final double HEIGHT = 70;
    private static final double DASH_PADDING = 5;


    private final Label codeLabel;
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

        codeLabel = new Label();
        codeLabel.setMinWidth(CODE_WIDTH);
        codeLabel.setMaxWidth(CODE_WIDTH);
        codeLabel.setAlignment(Pos.BASELINE_LEFT);
        codeLabel.getStyleClass().add("code");
        codeLabel.setPadding(new Insets(0));

        dash = new Label(EN_DASH_SYMBOL);
        dash.getStyleClass().add("amount-input-dash");
        dash.setPadding(new Insets(0));

        double baseline = 54;
        BaselineHBox textInputNode = new BaselineHBox(textField, baseline);
        if (model.isLeftSideRangeAmount()) {
            BaselineHBox dashNode = new BaselineHBox(dash, baseline);
            root.getChildren().addAll(textInputNode, dashNode);
        } else {
            BaselineHBox codeLabelNode = new BaselineHBox(codeLabel, baseline);
            root.getChildren().addAll(textInputNode, codeLabelNode);
        }
    }

    @Override
    protected void onViewAttached() {
        codeLabel.textProperty().bind(model.getCode());
        model.getFocusedProperty().bind(textField.focusedProperty());
        subscriptions.add(EasyBind.subscribe(model.getSumOfNumChars(), sumOfNumChars -> {
            if (sumOfNumChars != null) {
                updateFontsize(sumOfNumChars.intValue());
            }
        }));


        subscriptions.add(EasyBind.subscribe(model.getAmountFieldWidth(), width -> {
            if (width != null) {
               // textField.setMinWidth(width.doubleValue());
               // textField.setMaxWidth(width.doubleValue());
                textField.setPrefWidth(width.doubleValue());
               /* UIThread.runOnNextRenderFrame(()->{
                    textField.setMinWidth(width.doubleValue());
                    textField.setMaxWidth(width.doubleValue());
                    textField.setPrefWidth(width.doubleValue());
                });*/
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getDashFieldWidth(), width -> {
            if (width != null) {
              //  dash.setMinWidth(width.doubleValue());
               // dash.setMaxWidth(width.doubleValue());
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
        double size = AmountTextInputLayout.computeFontSize(length);
        if (Math.abs(size - lastSize) > 0.1) {
            textField.setStyle("-fx-font-size: " + size + "em;");
            dash.setStyle("-fx-font-size: " + size + "em;");
            lastSize = size;
        }
    }

    @Override
    protected void onViewDetached() {
        codeLabel.textProperty().unbind();
        model.getFocusedProperty().unbind();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }
}

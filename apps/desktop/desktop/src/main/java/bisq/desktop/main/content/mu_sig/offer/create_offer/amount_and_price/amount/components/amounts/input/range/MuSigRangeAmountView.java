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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.range;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout.PADDING;
import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout.WIDTH;

@Slf4j
public class MuSigRangeAmountView extends View<VBox, MuSigRangeAmountModel, MuSigRangeAmountController> {
    private final BisqMenuItem inputModeToggle;
    private final RangeAmountLayoutHelper layoutHelper;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigRangeAmountView(MuSigRangeAmountModel model,
                                MuSigRangeAmountController controller,
                                HBox minAmountInput,
                                HBox maxAmountInput,
                                HBox minPassiveAmount,
                                HBox maxPassiveAmount,
                                VBox amountSlider) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        layoutHelper = new RangeAmountLayoutHelper(model);
        layoutHelper.setVisible(false);
        layoutHelper.setManaged(false);

        inputModeToggle = new BisqMenuItem("flip-fields-arrows-green", "flip-fields-arrows-white");
        inputModeToggle.setTooltip(Res.get("muSig.offer.create.amount.selection.flipCurrenciesButton.tooltip"));

        HBox amountInputHBox = new HBox(5, minAmountInput, maxAmountInput);
        amountInputHBox.setMinWidth(WIDTH);
        amountInputHBox.setMaxWidth(WIDTH);
        amountInputHBox.setPadding(new Insets(0, PADDING, 0, PADDING));

        HBox.setMargin(minPassiveAmount, new Insets(0, 0, 0, 20));
        HBox amountDisplayAndToggle = new HBox(Spacer.fillHBox(), minPassiveAmount, maxPassiveAmount, Spacer.fillHBox(), inputModeToggle);
        amountDisplayAndToggle.setPadding(new Insets(0, 10, 0, 10));

        VBox.setMargin(amountSlider, new Insets(40, 0, 0, 0));

        Pane amountInputHBoxPane = new Pane(layoutHelper, amountInputHBox);

        root.getChildren().addAll(amountInputHBoxPane, amountDisplayAndToggle, amountSlider);
    }

    @Override
    protected void onViewAttached() {
        layoutHelper.onViewAttached();
        inputModeToggle.setOnAction(e -> controller.onToggleInputMode());
    }

    @Override
    protected void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        layoutHelper.onViewDetached();
        inputModeToggle.setOnAction(null);
    }
}

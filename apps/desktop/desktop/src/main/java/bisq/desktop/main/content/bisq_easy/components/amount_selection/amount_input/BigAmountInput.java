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

package bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BigAmountInput extends AmountInput {
    public BigAmountInput(boolean isBaseCurrency, boolean showCurrencyCode) {
        super(isBaseCurrency, showCurrencyCode);

        controller.setView(new BigAmountInputView(controller.model, controller));
    }

    private static class BigAmountInputView extends View {
        protected BigAmountInputView(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        public void initView() {
            root.setSpacing(10);
            root.getStyleClass().add("big-amount-input");
        }
    }
}

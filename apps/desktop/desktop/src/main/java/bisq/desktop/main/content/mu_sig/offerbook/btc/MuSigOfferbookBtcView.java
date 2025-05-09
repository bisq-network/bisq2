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

package bisq.desktop.main.content.mu_sig.offerbook.btc;

import bisq.common.currency.Market;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigOfferbookView;
import javafx.util.StringConverter;

public class MuSigOfferbookBtcView extends MuSigOfferbookView<MuSigOfferbookBtcModel, MuSigOfferbookBtcController> {

    public MuSigOfferbookBtcView(MuSigOfferbookBtcModel model, MuSigOfferbookBtcController controller) {
        super(model, controller);
    }

    protected StringConverter<Market> getConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Market market) {
                return market != null ? market.getQuoteCurrencyDisplayName() : "";
            }

            @Override
            public Market fromString(String string) {
                throw new UnsupportedOperationException("Conversion from String to Market is not supported");
            }
        };
    }
}

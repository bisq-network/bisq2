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

package bisq.desktop.primary.main.top;


import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Coin;
import bisq.desktop.common.view.Model;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.Getter;

@Getter
public class TopPanelModel implements Model {
    private final boolean isWalletEnabled;
    private final ObjectProperty<Coin> balanceAsCoinProperty = new SimpleObjectProperty<>(Coin.of(0, "BTC"));
    private final ObservableValue<String> formattedBalanceProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatAmount(balanceAsCoinProperty.get(), true),
            balanceAsCoinProperty
    );

    public TopPanelModel(DefaultApplicationService applicationService) {
        isWalletEnabled = applicationService.getElectrumWalletService().isWalletEnabled();
    }
}

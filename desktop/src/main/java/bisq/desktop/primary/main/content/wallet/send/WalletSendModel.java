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

package bisq.desktop.primary.main.content.wallet.send;

import bisq.desktop.common.view.Model;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WalletSendModel implements Model {
    private final StringProperty addressProperty = new SimpleStringProperty(this, "address");
    private final StringProperty amountProperty = new SimpleStringProperty(this, "amount");

    public StringProperty addressProperty() {
        return addressProperty;
    }

    public StringProperty amountProperty() {
        return amountProperty;
    }

    public String getAddress() {
        return addressProperty.get();
    }

    public String getAmount() {
        return amountProperty.get();
    }
}

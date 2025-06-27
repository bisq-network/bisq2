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

package bisq.desktop.main.content.user.accounts.create.summary;

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.user.accounts.create.summary.details.AccountDetailsGridPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PaymentSummaryModel implements Model {
    @Setter
    private PaymentMethod<?> paymentMethod;
    @Setter
    private AccountPayload<?> accountPayload;
    @Setter
    private AccountDetailsGridPane accountDetailsGridPane;
    @Setter
    private String currency;
    @Setter
    private String country;
    @Setter
    private String defaultAccountName;
    private final BooleanProperty showAccountNameOverlay = new SimpleBooleanProperty();
}
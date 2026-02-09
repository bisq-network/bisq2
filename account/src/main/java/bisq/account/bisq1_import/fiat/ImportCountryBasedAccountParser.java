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

package bisq.account.bisq1_import.fiat;

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class ImportCountryBasedAccountParser<M extends PaymentMethod<?>, P extends AccountPayload<M>> extends ImportFiatAccountParser<M, P> {
    protected final String countryCode;

    ImportCountryBasedAccountParser(JsonNode accountNode) {
        super(accountNode);
        countryCode = requireText(paymentAccountPayloadNode, "countryCode");
    }
}

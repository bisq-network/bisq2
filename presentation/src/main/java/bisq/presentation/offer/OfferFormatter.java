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

package bisq.presentation.offer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class OfferFormatter {
   /* static String formatAmountWithMinAmount(Monetary amount, Optional<Long> optionalMinAmount, boolean useMinPrecision) {
        return AmountFormatter.formatMinAmount(optionalMinAmount, amount, useMinPrecision) +
                AmountFormatter.formatAmountWithCode(amount, useMinPrecision);
    }

    static String formatDate(long date) {
        return DateFormatter.formatDateTime(new Date(date));
    }

    static String formatProtocolTypes(List<? extends SwapProtocolType> protocolTypes) {
        return protocolTypes.toString();
    }

    static String formatReputationOptions(Optional<ReputationOption> reputationOptions) {
        return reputationOptions.toString();
    }

    static String formatTransferOptions(FiatSettlementOption settlementOption) {
        return settlementOption.getBankName() + " / " + settlementOption.getCountyCodeOfBank();
    }

    static String formatTransferTypes(List<SettlementMethod> settlementMethods) {
        return settlementMethods.toString();
    }*/
}

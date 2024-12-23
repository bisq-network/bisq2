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

package bisq.dto.offer.bisq_easy;

import bisq.dto.account.protocol_type.TradeProtocolTypeDto;
import bisq.dto.common.currency.MarketDto;
import bisq.dto.network.identity.NetworkIdDto;
import bisq.dto.offer.DirectionDto;
import bisq.dto.offer.amount.spec.AmountSpecDto;
import bisq.dto.offer.options.OfferOptionDto;
import bisq.dto.offer.payment_method.BitcoinPaymentMethodSpecDto;
import bisq.dto.offer.payment_method.FiatPaymentMethodSpecDto;
import bisq.dto.offer.price.spec.PriceSpecDto;

import java.util.List;

public record BisqEasyOfferDto(String id,
                               long date,
                               NetworkIdDto makerNetworkId,
                               DirectionDto direction,
                               MarketDto market,
                               AmountSpecDto amountSpec,
                               PriceSpecDto priceSpec,
                               List<TradeProtocolTypeDto> protocolTypes,
                               List<BitcoinPaymentMethodSpecDto> baseSidePaymentMethodSpecs,
                               List<FiatPaymentMethodSpecDto> quoteSidePaymentMethodSpecs,
                               List<OfferOptionDto> offerOptions,
                               List<String> supportedLanguageCodes) {
}

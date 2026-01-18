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

package bisq.api.dto.contract.bisq_easy;

import bisq.api.dto.contract.PartyDto;
import bisq.api.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.api.dto.offer.payment_method.BitcoinPaymentMethodSpecDto;
import bisq.api.dto.offer.payment_method.FiatPaymentMethodSpecDto;
import bisq.api.dto.offer.price.spec.PriceSpecDto;
import bisq.api.dto.user.profile.UserProfileDto;

import java.util.Optional;

public record BisqEasyContractDto(long takeOfferDate,
                                  BisqEasyOfferDto offer,
                                  PartyDto maker,
                                  PartyDto taker,
                                  long baseSideAmount,
                                  long quoteSideAmount,
                                  BitcoinPaymentMethodSpecDto baseSidePaymentMethodSpec,
                                  FiatPaymentMethodSpecDto quoteSidePaymentMethodSpec,
                                  Optional<UserProfileDto> mediator,
                                  PriceSpecDto priceSpec,
                                  long marketPrice) {
}
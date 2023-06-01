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

package bisq.offer.poc.options;

import bisq.offer.offer_options.OfferOption;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class ContractOption implements OfferOption {
    private final long maxTradeAmount;
    private final long maxTradePeriod;

    public ContractOption(long maxTradeAmount, long maxTradePeriod) {
        this.maxTradeAmount = maxTradeAmount;
        this.maxTradePeriod = maxTradePeriod;
    }

    public bisq.offer.protobuf.OfferOption toProto() {
        return getOfferOptionBuilder().setContractOption(bisq.offer.protobuf.ContractOption.newBuilder()
                        .setMaxTradeAmount(maxTradeAmount)
                        .setMaxTradePeriod(maxTradePeriod))
                .build();
    }

    public static ContractOption fromProto(bisq.offer.protobuf.ContractOption proto) {
        return new ContractOption(proto.getMaxTradeAmount(), proto.getMaxTradePeriod());
    }
}
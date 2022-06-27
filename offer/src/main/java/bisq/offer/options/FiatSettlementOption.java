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

package bisq.offer.options;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class FiatSettlementOption implements OfferOption {
    private final String countyCodeOfBank;
    private final String bankName;

    public FiatSettlementOption(String countyCodeOfBank, String bankName) {
        this.countyCodeOfBank = countyCodeOfBank;
        this.bankName = bankName;
    }

    public bisq.offer.protobuf.OfferOption toProto() {
        return getOfferOptionBuilder().setFiatSettlementOption(bisq.offer.protobuf.FiatSettlementOption.newBuilder()
                        .setCountyCodeOfBank(countyCodeOfBank)
                        .setBankName(bankName))
                .build();
    }

    public static FiatSettlementOption fromProto(bisq.offer.protobuf.FiatSettlementOption proto) {
        return new FiatSettlementOption(proto.getCountyCodeOfBank(), proto.getBankName());
    }
}
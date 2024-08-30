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

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

//todo should be part of payment spec?
@Getter
@ToString
@EqualsAndHashCode
public final class FiatPaymentOption implements OfferOption {
    private final String countyCodeOfBank;
    private final String bankName;

    public FiatPaymentOption(String countyCodeOfBank, String bankName) {
        this.countyCodeOfBank = countyCodeOfBank;
        this.bankName = bankName;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateCode(countyCodeOfBank);
        NetworkDataValidation.validateText(bankName, 100);
    }

    @Override
    public bisq.offer.protobuf.OfferOption.Builder getBuilder(boolean serializeForHash) {
        return getOfferOptionBuilder(serializeForHash)
                .setFiatPaymentOption(bisq.offer.protobuf.FiatPaymentOption.newBuilder()
                        .setCountyCodeOfBank(countyCodeOfBank)
                        .setBankName(bankName));
    }

    @Override
    public bisq.offer.protobuf.OfferOption toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static FiatPaymentOption fromProto(bisq.offer.protobuf.FiatPaymentOption proto) {
        return new FiatPaymentOption(proto.getCountyCodeOfBank(), proto.getBankName());
    }
}
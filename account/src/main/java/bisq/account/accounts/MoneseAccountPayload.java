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

package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneseAccountPayload extends CountryBasedAccountPayload {

    private final String mobileNr;
    private final String holderName;

    public MoneseAccountPayload(String id,
                                String paymentMethodName,
                                String countryCode,
                                String mobileNr,
                                String holderName) {
        super(id, paymentMethodName, countryCode);
        this.mobileNr = mobileNr;
        this.holderName = holderName;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(mobileNr, 20);
        NetworkDataValidation.validateText(holderName, 100);
    }

    public static MoneseAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var monesePayload = countryBasedAccountPayload.getMoneseAccountPayload();

        return new MoneseAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                monesePayload.getMobileNr(),
                monesePayload.getHolderName()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setMoneseAccountPayload(toMoneseAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.MoneseAccountPayload toMoneseAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getMoneseAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneseAccountPayload.Builder getMoneseAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneseAccountPayload.newBuilder()
                .setMobileNr(mobileNr)
                .setHolderName(holderName);
    }
}
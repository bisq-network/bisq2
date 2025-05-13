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
public final class SatispayAccountPayload extends CountryBasedAccountPayload {

    private final String mobileNr;
    private final String holderName;

    public SatispayAccountPayload(String id,
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

    public static SatispayAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var satispayPayload = countryBasedAccountPayload.getSatispayAccountPayload();

        return new SatispayAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                satispayPayload.getMobileNr(),
                satispayPayload.getHolderName()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setSatispayAccountPayload(toSatispayAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SatispayAccountPayload toSatispayAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSatispayAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SatispayAccountPayload.Builder getSatispayAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SatispayAccountPayload.newBuilder()
                .setMobileNr(mobileNr)
                .setHolderName(holderName);
    }
}
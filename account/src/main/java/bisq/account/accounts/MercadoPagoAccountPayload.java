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
public final class MercadoPagoAccountPayload extends CountryBasedAccountPayload {

    private final String holderName;
    private final String holderId;

    public MercadoPagoAccountPayload(String id,
                                     String paymentMethodName,
                                     String countryCode,
                                     String holderName,
                                     String holderId) {
        super(id, paymentMethodName, countryCode);
        this.holderName = holderName;
        this.holderId = holderId;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validateText(holderId, 100);
    }

    public static MercadoPagoAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var mercadoPagoPayload = countryBasedAccountPayload.getMercadoPagoAccountPayload();

        return new MercadoPagoAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                mercadoPagoPayload.getHolderName(),
                mercadoPagoPayload.getHolderId()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setMercadoPagoAccountPayload(toMercadoPagoAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.MercadoPagoAccountPayload toMercadoPagoAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getMercadoPagoAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MercadoPagoAccountPayload.Builder getMercadoPagoAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MercadoPagoAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setHolderId(holderId);
    }
}
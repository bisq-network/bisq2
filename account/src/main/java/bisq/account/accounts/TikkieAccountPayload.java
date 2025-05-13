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
public final class TikkieAccountPayload extends CountryBasedAccountPayload {

    private final String iban;

    public TikkieAccountPayload(String id, String paymentMethodName, String countryCode, String iban) {
        super(id, paymentMethodName, countryCode);
        this.iban = iban;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(iban, 34); // IBAN length
    }

    public static TikkieAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var tikkiePayload = countryBasedAccountPayload.getTikkieAccountPayload();

        return new TikkieAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                tikkiePayload.getIban()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setTikkieAccountPayload(toTikkieAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.TikkieAccountPayload toTikkieAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getTikkieAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.TikkieAccountPayload.Builder getTikkieAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.TikkieAccountPayload.newBuilder()
                .setIban(iban);
    }
}
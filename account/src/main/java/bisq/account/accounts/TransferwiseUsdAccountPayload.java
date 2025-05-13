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
public final class TransferwiseUsdAccountPayload extends CountryBasedAccountPayload {

    private final String email;
    private final String holderName;
    private final String beneficiaryAddress;

    public TransferwiseUsdAccountPayload(String id,
                                         String paymentMethodName,
                                         String countryCode,
                                         String email,
                                         String holderName,
                                         String beneficiaryAddress) {
        super(id, paymentMethodName, countryCode);
        this.email = email;
        this.holderName = holderName;
        this.beneficiaryAddress = beneficiaryAddress;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateEmail(email);
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validateText(beneficiaryAddress, 200);
    }

    public static TransferwiseUsdAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var transferwiseUsdPayload = countryBasedAccountPayload.getTransferwiseUsdAccountPayload();

        return new TransferwiseUsdAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                transferwiseUsdPayload.getEmail(),
                transferwiseUsdPayload.getHolderName(),
                transferwiseUsdPayload.getBeneficiaryAddress()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setTransferwiseUsdAccountPayload(toTransferwiseUsdAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.TransferwiseUsdAccountPayload toTransferwiseUsdAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getTransferwiseUsdAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.TransferwiseUsdAccountPayload.Builder getTransferwiseUsdAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.TransferwiseUsdAccountPayload.newBuilder()
                .setEmail(email)
                .setHolderName(holderName)
                .setBeneficiaryAddress(beneficiaryAddress);
    }
}